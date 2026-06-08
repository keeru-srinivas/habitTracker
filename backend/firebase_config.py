import firebase_admin
from firebase_admin import credentials, firestore, auth
import os
from config import firebaseCredentialsPath, firebaseDatabaseUrl

def initializeFirebase():
    """Initialize Firebase Admin SDK if not already initialized"""
    if not firebase_admin._apps:
        if os.path.exists(firebaseCredentialsPath):
            cred = credentials.Certificate(firebaseCredentialsPath)
            firebase_admin.initialize_app(cred, {
                'databaseURL': firebaseDatabaseUrl
            })
        else:
            firebase_admin.initialize_app()
    
    return firestore.client(), auth

db, authClient = initializeFirebase()
