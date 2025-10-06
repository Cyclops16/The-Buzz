package edu.lehigh.cse216.ant823.admin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Map;

import edu.lehigh.cse216.ant823.admin.Database.UserRow;

public class Database {
    public interface RowData {
        int id();
        String displaySummary(); // or any common methods you want
    }

    public void createViews() {
        String[] viewStatements = {
            // View for valid users only
            "CREATE OR REPLACE VIEW ValidUsers AS " +
            "SELECT * FROM Users WHERE is_invalid = FALSE",

            // View for valid messages only
            "CREATE OR REPLACE VIEW ValidMessages AS " +
            "SELECT * FROM Messages WHERE is_invalid = FALSE",

            // View for messages with user info
            "CREATE OR REPLACE VIEW MessagesWithUser AS " +
            "SELECT m.*, u.first_name, u.last_name, u.email " +
            "FROM Messages m " +
            "JOIN Users u ON m.user_id = u.id",

            // View for number of comments per message
            "CREATE OR REPLACE VIEW MessageCommentCounts AS " +
            "SELECT msg_id, COUNT(*) AS comment_count " +
            "FROM Comments GROUP BY msg_id",

            // View for upvote/downvote counts per message (redundant if like/dislike_count is maintained, but still useful)
            "CREATE OR REPLACE VIEW MessageVoteCounts AS " +
            "SELECT m.id AS message_id, " +
            "       COUNT(DISTINCT uv.id) AS upvotes, " +
            "       COUNT(DISTINCT dv.id) AS downvotes " +
            "FROM Messages m " +
            "LEFT JOIN UserUpvotes uv ON m.id = uv.msg_id " +
            "LEFT JOIN UserDownvotes dv ON m.id = dv.msg_id " +
            "GROUP BY m.id",

            // View for active users (users who posted at least one message or comment)
            "CREATE OR REPLACE VIEW ActiveUsers AS " +
            "SELECT u.id, u.first_name, u.last_name, u.email " +
            "FROM Users u " +
            "WHERE u.id IN (SELECT user_id FROM Messages) OR u.id IN (SELECT user_id FROM Comments)"
        };

        for (String sql : viewStatements) {
            try (PreparedStatement stmt = mConnection.prepareStatement(sql)) {
                stmt.executeUpdate();
            } catch (SQLException e) {
                System.err.println("Create view failed: " + e.getMessage());
            }
        }
    }

    public void dropViews() {
        String[] views = {
            "ActiveUsers",
            "MessageCommentCounts",
            "MessagesWithUser",
            "MessageVoteCounts",
            "ValidMessages",
            "ValidUsers"
        };

        for (String view : views) {
            String sql = "DROP VIEW IF EXISTS " + view;
            try (PreparedStatement stmt = mConnection.prepareStatement(sql)) {
                stmt.executeUpdate();
                System.out.println("View '" + view + "' dropped.");
            } catch (SQLException e) {
                System.err.println("Error dropping view '" + view + "': " + e.getMessage());
            }
        }
    }

    public static record UserRow(
        int id,
        String firstName,
        String lastName,
        String email,
        String genderIdentity,
        String sexualOrientation,
        String note,
        boolean isValid
    )implements RowData {
        @Override
        public String displaySummary() {
            return firstName + " " + lastName + " (" + email + ")";
        }
    }

    public static record MessageRow(
        int id,
        int userId,
        String subject,
        String body,
        String attachmentUrl,
        int likes,
        int dislikes,
        boolean isValid
    )implements RowData {
        @Override
        public String displaySummary() {
            return "Subject: " + subject + ", Likes: " + likes + ", Dislikes: " + dislikes + attachmentUrl == null ? "<none>" : attachmentUrl;
        }
    }

    public static record CommentRow(
        int id,
        int userId,
        int messageId,
        String comment,
        boolean isValid
    )implements RowData {
        @Override
        public String displaySummary() {
            return "Comment: " + comment;
        }
    }
    public static record VoteRow(
        int userId,
        int messageId
    ) implements RowData {
        @Override
        public int id() {
            return userId; // or return a derived/composite id if needed
        }

        @Override
        public String displaySummary() {
            return "Vote by user " + userId + " on message " + messageId;
        }
    }

    public static record AttachmentRow(int id, String url, String lastModified, int userId) 
    implements RowData {
        @Override
        public String displaySummary() {
            return "Attachment: " + url + " (modified: " + lastModified + ")" + " Owner UserID: " + userId;
        }
    }


    private Connection mConnection;


    /**
     * Fetch all non-empty attachment URLs in descending last_modified_at order.
     */
    public ArrayList<AttachmentRow> selectAttachmentsSortedByModified() {
        String sql = """
            SELECT id, attachment_url, last_modified_at, user_id
              FROM messages
             WHERE attachment_url IS NOT NULL
               AND attachment_url <> ''
             ORDER BY last_modified_at DESC
        """;
        try (var stmt = mConnection.prepareStatement(sql);
             var rs = stmt.executeQuery()) {
            var list = new ArrayList<AttachmentRow>();
            while (rs.next()) {
                list.add(new AttachmentRow(
                    rs.getInt("id"),
                    rs.getString("attachment_url"),
                    rs.getTimestamp("last_modified_at").toString(),
                    rs.getInt("user_id")
                ));
            }
            return list;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Clear the attachment_url for a given message ID.
     * @param id message ID to clear
     * @return number of rows updated
     */
    public int clearAttachmentUrl(int id) {
        String sql = "UPDATE messages SET attachment_url = '' WHERE id = ?";
        try (var stmt = mConnection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
            return 0;
        }
    }

    private PreparedStatement mCreateAllTables;
    private static final String SQL_CREATE_ALL_TABLES =
        "CREATE TABLE IF NOT EXISTS Users (" +
        " id SERIAL PRIMARY KEY," +
        " email VARCHAR(255) UNIQUE NOT NULL," +
        " first_name VARCHAR(100)," +
        " last_name VARCHAR(100)," +
        " gender_identity VARCHAR(100)," +
        " sexual_orientation VARCHAR(100)," +
        " note TEXT," +
        " is_invalid BOOLEAN DEFAULT FALSE);" +

        "CREATE TABLE IF NOT EXISTS Messages (" +
        " id SERIAL PRIMARY KEY," +
        " user_id INTEGER NOT NULL REFERENCES Users(id) ON DELETE CASCADE," +
        " subject VARCHAR(100) NOT NULL," +
        " body VARCHAR(512) NOT NULL," +
        " created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
        " last_modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
        " like_count INTEGER DEFAULT 0," +
        " dislike_count INTEGER DEFAULT 0," +
        " is_invalid BOOLEAN DEFAULT FALSE," +
        " attachment_url TEXT," +
        " UNIQUE(user_id, subject, body));" +

        "CREATE TABLE IF NOT EXISTS Comments (" +
        " id SERIAL PRIMARY KEY," +
        " user_id INTEGER NOT NULL REFERENCES Users(id) ON DELETE CASCADE," +
        " msg_id INTEGER NOT NULL REFERENCES Messages(id) ON DELETE CASCADE," +
        " body TEXT NOT NULL);" +

        "CREATE TABLE IF NOT EXISTS UserUpvotes (" +
        " id SERIAL PRIMARY KEY," +
        " user_id INTEGER NOT NULL REFERENCES Users(id) ON DELETE CASCADE," +
        " msg_id INTEGER NOT NULL REFERENCES Messages(id) ON DELETE CASCADE," +
        " UNIQUE(user_id, msg_id));" +

        "CREATE TABLE IF NOT EXISTS UserDownvotes (" +
        " id SERIAL PRIMARY KEY," +
        " user_id INTEGER NOT NULL REFERENCES Users(id) ON DELETE CASCADE," +
        " msg_id INTEGER NOT NULL REFERENCES Messages(id) ON DELETE CASCADE," +
        " UNIQUE(user_id, msg_id));";

    public void createTable(String tableName) {
        String sql = switch (tableName) {
            case "users" -> "CREATE TABLE IF NOT EXISTS Users id SERIAL PRIMARY KEY, email VARCHAR(255) UNIQUE NOT NULL, first_name VARCHAR(100) last_name VARCHAR(100), gender_identity VARCHAR(100), sexual_orientation VARCHAR(100), note TEXT, is_invalid BOOLEAN DEFAULT FALSE)";
            case "messages" -> "CREATE TABLE IF NOT EXISTS messages (id SERIAL PRIMARY KEY, user_id INT, subject TEXT, body TEXT, like_count INT DEFAULT 0, dislike_count INT DEFAULT 0, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, last_modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, is_invalid BOOLEAN DEFAULT FALSE, attachment_url TEXT)";
            case "comments" -> "CREATE TABLE IF NOT EXISTS comments (id SERIAL PRIMARY KEY, user_id INT, message_id INT, comment TEXT)";
            case "user_upvotes" -> "CREATE TABLE IF NOT EXISTS user_upvotes (id SERIAL PRIMARY KEY, user_id INT, msg_id INT)";
            case "user_downvotes" -> "CREATE TABLE IF NOT EXISTS user_downvotes (id SERIAL PRIMARY KEY, user_id INT, msg_id INT)";
            default -> null;
        };
        if (sql != null) {
            try (PreparedStatement stmt = mConnection.prepareStatement(sql)) {
                stmt.executeUpdate();
            } catch (SQLException e) {
                System.err.println("Create table failed: " + e.getMessage());
            }
        }
    }

    public int insertRow(String tableName, String fName, String lName, String email, String gender, String sexOrient, String note) {
        if (!tableName.equals("users")) return -1;
        String sql = "INSERT INTO users (first_name, last_name, email, gender_identity, sexual_orientation, note) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = mConnection.prepareStatement(sql)) {
            stmt.setString(1, fName);
            stmt.setString(2, lName);
            stmt.setString(3, email);
            stmt.setString(4, gender);
            stmt.setString(5, sexOrient);
            stmt.setString(6, note);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Insert user failed: " + e.getMessage());
            return -1;
        }
    }

    public int insertRow(String tableName, int userId, int targetId, String subject, String body, String attachmentUrl, int likes, int dislikes) {
        String sql = switch (tableName) {
            case "messages" -> "INSERT INTO messages (user_id, subject, body, attachment_url) VALUES (?, ?, ?, ?)";
            case "comments" -> "INSERT INTO comments (user_id, msg_id, body) VALUES (?, ?, ?)";
            case "user_upvotes", "user_downvotes" -> "INSERT INTO " + tableName + " (user_id, msg_id) VALUES (?, ?)";
            default -> null;
        };
        if (sql == null) return -1;

        try (PreparedStatement stmt = mConnection.prepareStatement(sql)) {
            switch (tableName) {
                case "messages" -> {
                    stmt.setInt(1, userId);
                    stmt.setString(2, subject);
                    stmt.setString(3, body);
                    stmt.setString(4, (attachmentUrl == null || attachmentUrl.isBlank()) ? null : attachmentUrl);
                }
                case "comments" -> {
                    stmt.setInt(1, userId);
                    stmt.setInt(2, targetId); // msg_id
                    stmt.setString(3, body);
                }
                case "user_upvotes", "user_downvotes" -> {
                    stmt.setInt(1, userId);
                    stmt.setInt(2, targetId); // msg_id
                }
            }
            return stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Insert failed: " + e.getMessage());
            return -1;
        }
    }

    public int updateOne(String tableName, int id, String... newValues) {
        String sql;
        ArrayList<String> updates = new ArrayList<>();
        ArrayList<Object> params = new ArrayList<>();

        switch (tableName) {
            case "users" -> {
                String[] columns = {"email", "gender_identity", "sexual_orientation", "is_invalid"};
                for (int i = 0; i < columns.length; i++) {
                    if (i < newValues.length && newValues[i] != null && !newValues[i].isEmpty()) {
                        updates.add(columns[i] + " = ?");
                        if (columns[i].equals("is_invalid")) {
                            params.add(Boolean.parseBoolean(newValues[i]));
                        } else {
                            params.add(newValues[i]);
                        }
                    }
                }
                if (updates.isEmpty()) return 0;
                sql = "UPDATE users SET " + String.join(", ", updates) + " WHERE id = ?";
            }
            case "messages" -> {
                String[] columns = {"subject", "body", "is_invalid"};
                for (int i = 0; i < columns.length; i++) {
                    if (i < newValues.length && newValues[i] != null && !newValues[i].isEmpty()) {
                        updates.add(columns[i] + " = ?");
                        if (columns[i].equals("is_invalid")) {
                            params.add(Boolean.parseBoolean(newValues[i]));
                        } else {
                            params.add(newValues[i]);
                        }
                    }
                }
                if (updates.isEmpty()) return 0;
                sql = "UPDATE messages SET " + String.join(", ", updates) + " WHERE id = ?";
            }
            case "comments" -> {
                if (newValues.length < 1 || newValues[0] == null || newValues[0].isEmpty()) return 0;
                sql = "UPDATE comments SET comment = ? WHERE id = ?";
                params.add(newValues[0]);
            }
            default -> {
                return -1;
            }
        }

        try (PreparedStatement stmt = mConnection.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            stmt.setInt(params.size() + 1, id);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Update failed: " + e.getMessage());
            return -1;
        }
    }



    public void dropTable(String tableName) {
        String sql = "DROP TABLE IF EXISTS " + tableName + " CASCADE";
        try (PreparedStatement stmt = mConnection.prepareStatement(sql)) {
            stmt.executeUpdate();
            System.out.println("Dropped table: " + tableName);
        } catch (SQLException e) {
            System.err.println("Drop table failed for " + tableName + ": " + e.getMessage());
        }
    }
    
    public int deleteRow(String tableName, int id) {
        String sql = "DELETE FROM " + tableName + " WHERE id = ?";
        try (PreparedStatement stmt = mConnection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Delete failed: " + e.getMessage());
            return -1;
        }
    }

    public RowData selectOne(String tableName, int id) {
        String sql = "SELECT * FROM " + tableName + " WHERE id = ?";
        try (PreparedStatement stmt = mConnection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return switch (tableName) {
                        case "users" -> new UserRow(
                            rs.getInt("id"),
                            rs.getString("first_name"),
                            rs.getString("last_name"),
                            rs.getString("email"),
                            rs.getString("gender_identity"),
                            rs.getString("sexual_orientation"),
                            rs.getString("note"),
                            !rs.getBoolean("is_invalid")
                        );
                        case "messages" -> new MessageRow(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getString("subject"),
                            rs.getString("body"),
                            rs.getString("attachment_url"),
                            rs.getInt("like_count"),
                            rs.getInt("dislike_count"),
                            !rs.getBoolean("is_invalid")
                        );
                        case "comments" -> new CommentRow(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getInt("msg_id"),
                            rs.getString("body"),
                            true
                        );
                        default -> null;
                    };
                }
            }
        } catch (SQLException e) {
            System.err.println("Select one failed: " + e.getMessage());
        }
        return null;
    }

    public ArrayList<Object> selectAll(String tableName) {
        ArrayList<Object> results = new ArrayList<>();
        String sql = "SELECT * FROM " + tableName;
        try (PreparedStatement stmt = mConnection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                switch (tableName) {
                    case "users" -> results.add(new UserRow(
                        rs.getInt("id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("email"),
                        rs.getString("gender_identity"),
                        rs.getString("sexual_orientation"),
                        rs.getString("note"),
                        !rs.getBoolean("is_invalid")
                    ));
                    case "messages" -> results.add(new MessageRow(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("subject"),
                        rs.getString("body"),
                        rs.getString("attachment_url"),
                        rs.getInt("like_count"),
                        rs.getInt("dislike_count"),
                        !rs.getBoolean("is_invalid")
                    ));
                    case "comments" -> results.add(new CommentRow(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getInt("msg_id"),
                        rs.getString("body"),
                        true
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Select all failed: " + e.getMessage());
        }
        return results;
    }

    boolean disconnect() {
        if (mConnection != null) {
            System.out.print("Disconnecting from database.");
            try {
                mConnection.close();
            } catch (SQLException e) {
                System.err.println("\n\tError: close() threw a SQLException");
                e.printStackTrace();
                mConnection = null;
                return false;
            }
            System.out.println(" ...  connection successfully closed");
            mConnection = null;
            return true;
        }
        System.err.print("Unable to close connection: Connection was null");
        return false;
    }

    private Database() {}

    static Database getDatabase(String dbUri) {
        if (dbUri != null && dbUri.length() > 0) {
            Database returned_val = new Database();
            System.out.println("Attempting to use provided DATABASE_URI env var.");
            try {
                returned_val.mConnection = DriverManager.getConnection(dbUri);
                if (returned_val.mConnection == null) {
                    System.err.println("Error: DriverManager.getConnection() returned a null object");
                    return null;
                } else {
                    System.out.println(" ... successfully connected");
                }
            } catch (SQLException e) {
                System.err.println("Error: DriverManager.getConnection() threw a SQLException");
                e.printStackTrace();
                return null;
            }
            return returned_val;
        }
        return null;
    }

    static Database getDatabase(String ip, String port, String dbname, String user, String pass) {
        if (ip != null && port != null && dbname != null && user != null && pass != null) {
            Database returned_val = new Database();
            System.out.println("Attempting to use provided POSTGRES_{IP, PORT, USER, PASS, DBNAME} env var.");
            System.out.println("Connecting to " + ip + ":" + port);
            try {
                returned_val.mConnection = DriverManager.getConnection("jdbc:postgresql://" + ip + ":" + port + "/" + dbname, user, pass);
                if (returned_val.mConnection == null) {
                    System.out.println("\n\tError: DriverManager.getConnection() returned a null object");
                    return null;
                } else {
                    System.out.println(" ... successfully connected");
                }
            } catch (SQLException e) {
                System.out.println("\n\tError: DriverManager.getConnection() threw a SQLException");
                e.printStackTrace();
                return null;
            }
            return returned_val;
        }
        return null;
    }

    static Database getDatabase() {
        Map<String, String> env = System.getenv();
        String ip = env.get("POSTGRES_IP");
        String port = env.get("POSTGRES_PORT");
        String user = env.get("POSTGRES_USER");
        String pass = env.get("POSTGRES_PASS");
        String dbname = env.get("POSTGRES_DBNAME");
        String dbUri = env.get("DATABASE_URI");

        System.out.printf("Using the following environment variables:");
        System.out.printf("POSTGRES_IP=%s%n", ip);
        System.out.printf("POSTGRES_PORT=%s%n", port);
        System.out.printf("POSTGRES_USER=%s%n", user);
        System.out.printf("POSTGRES_PASS=%s%n", "omitted");
        System.out.printf("POSTGRES_DBNAME=%s%n", dbname);
        System.out.printf("DATABASE_URI=%s%n", dbUri);

        if (dbUri != null && dbUri.length() > 0) {
            return Database.getDatabase(dbUri);
        } else if (ip != null && port != null && dbname != null && user != null && pass != null) {
            return Database.getDatabase(ip, port, dbname, user, pass);
        }
        System.err.println("Insufficient information to connect to database.");
        return null;
    }
}