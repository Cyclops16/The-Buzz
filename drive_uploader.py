import os
from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload

# Path to your downloaded service account JSON file
SERVICE_ACCOUNT_FILE = "service_account.json"

# Scopes define the level of access requested (Drive access here)
SCOPES = ["https://www.googleapis.com/auth/drive"]

# Optional: Specify a folder ID in your Drive to upload into
DRIVE_FOLDER_ID = None  # Replace with a folder ID or leave None

# Authenticate and create the Drive service
credentials = service_account.Credentials.from_service_account_file(
    SERVICE_ACCOUNT_FILE, scopes=SCOPES
)
drive_service = build("drive", "v3", credentials=credentials)


def upload_file_to_drive(local_file_path, filename, mime_type="application/octet-stream"):
    file_metadata = {"name": filename}
    if DRIVE_FOLDER_ID:
        file_metadata["parents"] = [DRIVE_FOLDER_ID]

    media = MediaFileUpload(local_file_path, mimetype=mime_type)
    uploaded_file = (
        drive_service.files()
        .create(body=file_metadata, media_body=media, fields="id")
        .execute()
    )

    # Make the file publicly viewable
    drive_service.permissions().create(
        fileId=uploaded_file["id"],
        body={"role": "reader", "type": "anyone"},
    ).execute()

    # Return a direct download URL
    file_id = uploaded_file["id"]
    file_url = f"https://drive.google.com/uc?id={file_id}&export=download"
    return file_url
