from dotenv import load_dotenv
load_dotenv()

import os
import uuid
from flask import Flask, request, jsonify, send_from_directory
from flask_cors import CORS
from database import Database
from google_oauth import verify_google_token
from werkzeug.utils import secure_filename

app = Flask(__name__)

CORS(app, supports_credentials=True, origins=[
    "http://localhost:3000",
    "https://cse216-sp25-team-02.onrender.com"
])

# Attachment upload config
UPLOAD_FOLDER = "uploads"
ALLOWED_EXTENSIONS = {"png", "jpg", "jpeg", "gif", "pdf"}
app.config["UPLOAD_FOLDER"] = UPLOAD_FOLDER
os.makedirs(UPLOAD_FOLDER, exist_ok=True)

def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

try:
    db = Database()
except Exception as e:
    db = None

session_store = {}  # in-memory session storage (session_id -> user_id)

def get_user_id():
    session_id = request.cookies.get("session_id")
    return session_store.get(session_id)

@app.route("/")
def home():
    return "Flask backend is running!"

@app.route("/login", methods=["POST", "OPTIONS"])
def login():
    if request.method == "OPTIONS":
        return '', 200
    try:
        credential = request.json.get("credential")
        if not credential:
            return jsonify({"status": "error", "message": "Missing credential"}), 400

        payload = verify_google_token(credential)
        if not payload:
            return jsonify({"status": "error", "message": "Invalid token"}), 401

        email = payload.get("email")
        if not email.endswith("@lehigh.edu"):
            return jsonify({"status": "error", "message": "Email must be @lehigh.edu"}), 403

        user_id = db.upsert_user(
            payload.get("given_name", ""),
            payload.get("family_name", ""),
            email
        )
        session_id = str(uuid.uuid4())
        session_store[session_id] = user_id

        response = jsonify({"status": "ok", "message": "Login successful"})
        response.set_cookie(
            "session_id", session_id,
            httponly=True,
            samesite="None",
            secure=True
        )
        return response

    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500

@app.route("/debug-insert", methods=["POST"])
def debug_insert():
    db.insert_idea("Debug Title", "Debug Body", 1)
    return jsonify({"status": "ok"})

@app.route("/logout", methods=["POST"])
def logout():
    session_id = request.cookies.get("session_id")
    if session_id in session_store:
        session_store.pop(session_id)
    response = jsonify({"status": "ok", "message": "Logged out"})
    response.delete_cookie("session_id")
    return response

@app.route("/profile", methods=["GET"])
def profile():
    user_id = get_user_id()
    if not user_id:
        return jsonify({"status": "error", "message": "Not logged in"}), 401
    return jsonify({"status": "ok", "data": db.get_user_profile(user_id)})

@app.route("/profile/<int:id>", methods=["GET"])
def public_profile(id):
    return jsonify({"status": "ok", "data": db.get_public_profile(id)})

@app.route("/profile", methods=["PUT"])
def update_profile():
    user_id = get_user_id()
    if not user_id:
        return jsonify({"status": "error", "message": "Not logged in"}), 401

    data = request.json

    allowed_genders = ["male", "female", "transgender male", "transgender female", "queer", "another identity"]
    allowed_orientations = ["gay", "lesbian", "straight", "bisexual", "something else", "don't know"]

    if "gender_identity" in data:
        value = data["gender_identity"].strip().lower()
        if value not in allowed_genders:
            return jsonify({"status": "error", "message": "Invalid gender_identity."}), 400
        db.update_user_gender(user_id, data["gender_identity"].strip())

    if "sexual_orientation" in data:
        value = data["sexual_orientation"].strip().lower()
        if value not in allowed_orientations:
            return jsonify({"status": "error", "message": "Invalid sexual_orientation."}), 400
        db.update_user_orientation(user_id, data["sexual_orientation"].strip())

    if "note" in data:
        db.update_user_note(user_id, data["note"].strip())

    return jsonify({"status": "ok"})

@app.route("/ideas", methods=["GET"])
def get_ideas():
    user_id = get_user_id()
    if not user_id:
        return jsonify({"status": "error", "message": "Not logged in"}), 401
    return jsonify({"status": "ok", "data": db.get_all_ideas()})

@app.route("/ideas/<int:id>", methods=["GET"])
def get_idea(id):
    return jsonify({"status": "ok", "data": db.get_idea(id)})

@app.route("/ideas", methods=["POST"])
def post_idea():
    user_id = get_user_id()
    if not user_id:
        return jsonify({"status": "error", "message": "Not logged in"}), 401

    title = request.form.get("title")
    message = request.form.get("message")
    file = request.files.get("file")

    attachment_url = None
    if file and file.filename:
        filename = secure_filename(file.filename)
        file_path = os.path.join(UPLOAD_FOLDER, filename)
        file.save(file_path)
        attachment_url = f"/uploads/{filename}"  # Or however you reference the file in your DB

    idea_id = db.insert_idea(title, message, user_id, attachment_url)
    return jsonify({"status": "ok", "id": idea_id})

@app.route("/uploads/<path:filename>")
def serve_upload(filename):
    return send_from_directory(app.config["UPLOAD_FOLDER"], filename)

@app.route("/comments/<int:msg_id>", methods=["POST"])
def add_comment(msg_id):
    user_id = get_user_id()
    if not user_id:
        return jsonify({"status": "error", "message": "Not logged in"}), 401
    comment_id = db.add_comment(user_id, msg_id, request.json.get("body"))
    return jsonify({"status": "ok", "id": comment_id})

@app.route("/comments/<int:msg_id>", methods=["GET"])
def get_comments(msg_id):
    return jsonify({"status": "ok", "data": db.get_comments(msg_id)})

@app.route("/comments/<int:cid>", methods=["PUT"])
def edit_comment(cid):
    user_id = get_user_id()
    if not user_id:
        return jsonify({"status": "error", "message": "Not logged in"}), 401
    db.edit_comment(cid, user_id, request.json.get("body"))
    return jsonify({"status": "ok"})

@app.route("/vote/<int:msg_id>", methods=["POST"])
def vote(msg_id):
    user_id = get_user_id()
    if not user_id:
        return jsonify({"status": "error", "message": "Not logged in"}), 401
    direction = request.json.get("direction")
    db.apply_vote(user_id, msg_id, direction)
    return jsonify({"status": "ok"})

@app.errorhandler(404)
def not_found(error):
    return jsonify({"status": "error", "message": "Route not found"}), 404

if __name__ == "__main__":
    port = int(os.environ.get("PORT", 8000))
    app.run(host="0.0.0.0", port=port)
