import firebase_admin
from firebase_admin import credentials, firestore, auth
import os
from config import FIREBASE_CREDENTIALS_PATH, FIREBASE_DATABASE_URL

def initializeFirebase():
    """Initialize Firebase Admin SDK if not already initialized"""
    if not firebase_admin._apps:
        if os.path.exists(FIREBASE_CREDENTIALS_PATH):
            cred = credentials.Certificate(FIREBASE_CREDENTIALS_PATH)
            firebase_admin.initialize_app(cred, {
                'databaseURL': FIREBASE_DATABASE_URL
            })
        else:
            firebase_admin.initialize_app()
    
    return firestore.client(), auth

db, authClient = initializeFirebase()
