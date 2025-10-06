from google.oauth2 import id_token
from google.auth.transport import requests
import os

def verify_google_token(token):
    try:
        idinfo = id_token.verify_oauth2_token(
            token,
            requests.Request(),
            os.getenv("GOOGLE_CLIENT_ID")
        )
        return idinfo  # This includes email, name, etc.
    except Exception as e:
        print("Token verification failed:", e)
        return None
