import os
import psycopg2
from psycopg2.extras import RealDictCursor
from psycopg2 import OperationalError, InterfaceError, DatabaseError

class Database:
    def __init__(self):
        self._connect()
        self.create_all_tables()

    def _connect(self):
        database_url = os.getenv("DATABASE_URL")
        self.conn = psycopg2.connect(database_url)
        self.conn.autocommit = False

    def _ensure_connection(self):
        try:
            with self.conn.cursor() as cur:
                cur.execute("SELECT 1;")
        except (OperationalError, InterfaceError):
            self._connect()

    def _execute(self, query, args=None, fetch=False, fetchone=False, commit=False):
        self._ensure_connection()
        try:
            with self.conn.cursor(cursor_factory=RealDictCursor) as cur:
                cur.execute(query, args or [])
                if commit:
                    self.conn.commit()
                if fetchone:
                    return cur.fetchone()
                if fetch:
                    return cur.fetchall()
        except DatabaseError:
            self.conn.rollback()
            raise

    def create_all_tables(self):
        self._execute("""
            CREATE TABLE IF NOT EXISTS public.users (
                id SERIAL PRIMARY KEY,
                first_name TEXT,
                last_name TEXT,
                email TEXT UNIQUE,
                gender_identity TEXT,
                sexual_orientation TEXT,
                note TEXT,
                is_invalid BOOLEAN DEFAULT FALSE
            );
            CREATE TABLE IF NOT EXISTS public.messages (
                id SERIAL PRIMARY KEY,
                subject TEXT,
                body TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                last_modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                like_count INT DEFAULT 0,
                dislike_count INT DEFAULT 0,
                user_id INT REFERENCES public.users(id) ON DELETE CASCADE,
                is_invalid BOOLEAN DEFAULT FALSE,
                attachment_url TEXT,
                link_url TEXT
            );
            CREATE TABLE IF NOT EXISTS public.comments (
                id SERIAL PRIMARY KEY,
                user_id INT REFERENCES public.users(id) ON DELETE CASCADE,
                msg_id INT REFERENCES public.messages(id) ON DELETE CASCADE,
                body TEXT,
                attachment_url TEXT,
                link_url TEXT
            );
            CREATE TABLE IF NOT EXISTS public.userupvotes (
                id SERIAL PRIMARY KEY,
                user_id INT REFERENCES public.users(id) ON DELETE CASCADE,
                msg_id INT REFERENCES public.messages(id) ON DELETE CASCADE,
                UNIQUE(user_id, msg_id)
            );
            CREATE TABLE IF NOT EXISTS public.userdownvotes (
                id SERIAL PRIMARY KEY,
                user_id INT REFERENCES public.users(id) ON DELETE CASCADE,
                msg_id INT REFERENCES public.messages(id) ON DELETE CASCADE,
                UNIQUE(user_id, msg_id)
            );
        """, commit=True)

    def upsert_user(self, first_name, last_name, email):
        return self._execute("""
            INSERT INTO public.users (first_name, last_name, email)
            VALUES (%s, %s, %s)
            ON CONFLICT(email) DO UPDATE SET first_name = EXCLUDED.first_name, last_name = EXCLUDED.last_name
            RETURNING id;
        """, (first_name, last_name, email), fetchone=True, commit=True)["id"]

    def get_user_profile(self, user_id):
        return self._execute("""
            SELECT email, first_name, last_name, gender_identity, sexual_orientation, note
            FROM public.users WHERE id = %s;
        """, (user_id,), fetchone=True)

    def get_public_profile(self, user_id):
        return self._execute("""
            SELECT first_name, last_name, email, note FROM public.users WHERE id = %s;
        """, (user_id,), fetchone=True)

    def update_user_gender(self, user_id, gender_identity):
        self._execute("""
            UPDATE public.users SET gender_identity = %s
            WHERE id = %s;
        """, (gender_identity, user_id), commit=True)

    def update_user_orientation(self, user_id, sexual_orientation):
        self._execute("""
            UPDATE public.users SET sexual_orientation = %s
            WHERE id = %s;
        """, (sexual_orientation, user_id), commit=True)

    def update_user_note(self, user_id, note):
        self._execute("""
            UPDATE public.users SET note = %s
            WHERE id = %s;
        """, (note, user_id), commit=True)


    def get_all_ideas(self):
        return self._execute("SELECT * FROM public.messages WHERE is_invalid = FALSE;", fetch=True)

    def get_idea(self, msg_id):
        return self._execute("SELECT * FROM public.messages WHERE id = %s;", (msg_id,), fetchone=True)

    def insert_idea(self, title, message, user_id, attachment=None, link=None):
        if attachment and link:
            return self._execute("""
                INSERT INTO public.messages (subject, body, user_id, attachment_url, link_url)
                VALUES (%s, %s, %s, %s, %s) RETURNING id;
            """, (title, message, user_id, attachment, link), fetchone=True, commit=True)["id"]
        elif attachment:
            return self._execute("""
                INSERT INTO public.messages (subject, body, user_id, attachment_url)
                VALUES (%s, %s, %s, %s) RETURNING id;
            """, (title, message, user_id, attachment), fetchone=True, commit=True)["id"]
        elif link:
            return self._execute("""
                INSERT INTO public.messages (subject, body, user_id, link_url)
                VALUES (%s, %s, %s, %s) RETURNING id;
            """, (title, message, user_id, link), fetchone=True, commit=True)["id"]
        else:
            return self._execute("""
                INSERT INTO public.messages (subject, body, user_id)
                VALUES (%s, %s, %s) RETURNING id;
            """, (title, message, user_id), fetchone=True, commit=True)["id"]


    def update_idea(self, msg_id, title, message, attachment=None):
        if attachment:
            self._execute("""
                UPDATE public.messages
                SET subject = %s, body = %s, last_modified_at = CURRENT_TIMESTAMP, attachment_url = %s
                WHERE id = %s;
            """, (title, message, attachment, msg_id), commit=True)
        else:
            self._execute("""
                UPDATE public.messages
                SET subject = %s, body = %s, last_modified_at = CURRENT_TIMESTAMP
                WHERE id = %s;
            """, (title, message, msg_id), commit=True)

    def delete_idea(self, msg_id):
        self._execute("DELETE FROM public.messages WHERE id = %s;", (msg_id,), commit=True)

    def add_comment(self, user_id, msg_id, body, attachment=None, link=None):
        if attachment and link:
            return self._execute("""
                INSERT INTO public.comments (user_id, msg_id, body, attachment_url, link_url)
                VALUES (%s, %s, %s, %s, %s) RETURNING id;
            """, (user_id, msg_id, body, attachment, link), fetchone=True, commit=True)["id"]
        elif attachment:
            return self._execute("""
                INSERT INTO public.comments (user_id, msg_id, body, attachment_url)
                VALUES (%s, %s, %s, %s) RETURNING id;
            """, (user_id, msg_id, body, attachment), fetchone=True, commit=True)["id"]
        elif link:
            return self._execute("""
                INSERT INTO public.comments (user_id, msg_id, body, link_url)
                VALUES (%s, %s, %s, %s) RETURNING id;
            """, (user_id, msg_id, body, link), fetchone=True, commit=True)["id"]
        else:
            return self._execute("""
                INSERT INTO public.comments (user_id, msg_id, body)
                VALUES (%s, %s, %s) RETURNING id;
            """, (user_id, msg_id, body), fetchone=True, commit=True)["id"]
        
    def get_comments(self, msg_id):
        return self._execute("SELECT * FROM public.comments WHERE msg_id = %s;", (msg_id,), fetch=True)

    def edit_comment(self, comment_id, user_id, new_body, attachment=None):
        if attachment is not None:
            self._execute("""
                UPDATE public.comments SET body = %s, attachment_url = %s 
                WHERE id = %s AND user_id = %s;
            """, (new_body, attachment, comment_id, user_id), commit=True)
        else:
            self._execute("""
                UPDATE public.comments SET body = %s 
                WHERE id = %s AND user_id = %s;
            """, (new_body, comment_id, user_id), commit=True)

    def apply_vote(self, user_id, msg_id, direction):
        self._execute("DELETE FROM public.userupvotes WHERE user_id = %s AND msg_id = %s;", (user_id, msg_id), commit=True)
        self._execute("DELETE FROM public.userdownvotes WHERE user_id = %s AND msg_id = %s;", (user_id, msg_id), commit=True)

        if direction == "up":
            # Check if the user already upvoted
            existing = self._execute(
                "SELECT 1 FROM public.userupvotes WHERE user_id = %s AND msg_id = %s;",
                (user_id, msg_id), fetchone=True
            )
            if not existing:
                self._execute(
                    "INSERT INTO public.userupvotes (user_id, msg_id) VALUES (%s, %s);",
                    (user_id, msg_id), commit=True
                )

        elif direction == "down":
            # Check if the user already downvoted
            existing = self._execute(
                "SELECT 1 FROM public.userdownvotes WHERE user_id = %s AND msg_id = %s;",
                (user_id, msg_id), fetchone=True
            )
            if not existing:
                self._execute(
                    "INSERT INTO public.userdownvotes (user_id, msg_id) VALUES (%s, %s);",
                    (user_id, msg_id), commit=True
                )

        # Update the vote counts
        upvotes = self._execute(
            "SELECT COUNT(*) FROM public.userupvotes WHERE msg_id = %s;",
            (msg_id,), fetchone=True)["count"]

        downvotes = self._execute(
            "SELECT COUNT(*) FROM public.userdownvotes WHERE msg_id = %s;",
            (msg_id,), fetchone=True)["count"]

        self._execute("""
            UPDATE public.messages SET like_count = %s, dislike_count = %s WHERE id = %s;
        """, (upvotes, downvotes, msg_id), commit=True)