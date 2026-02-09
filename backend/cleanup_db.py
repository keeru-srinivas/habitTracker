"""
Script to clean up all data from Firebase (users, habits, entries)
WARNING: This will delete ALL data from your database!
"""

from firebase_config import db, authClient
from firebase_admin import auth
import sys

def deleteAllUsers():
    """Delete all users from Firestore and Firebase Auth"""
    print("Deleting all users...")
    
    # Delete from Firestore
    usersRef = db.collection("users")
    users = list(usersRef.stream())
    print(f"Found {len(users)} users in Firestore")
    
    for userDoc in users:
        userData = userDoc.to_dict()
        userId = userData.get("id") or userDoc.id
        
        # Delete from Firestore
        userDoc.reference.delete()
        print(f"  ✓ Deleted user from Firestore: {userId}")
        
        # Delete from Firebase Auth
        try:
            auth.delete_user(userId)
            print(f"  ✓ Deleted user from Auth: {userId}")
        except Exception as e:
            print(f"  ⚠ Could not delete from Auth: {str(e)}")
    
    print(f"✅ Deleted {len(users)} users\n")

def deleteAllHabits():
    """Delete all habits from Firestore"""
    print("Deleting all habits...")
    
    habitsRef = db.collection("habits")
    habits = list(habitsRef.stream())
    print(f"Found {len(habits)} habits")
    
    for habitDoc in habits:
        habitData = habitDoc.to_dict()
        habitId = habitDoc.id
        habitTitle = habitData.get("title", "Unknown")
        habitDoc.reference.delete()
        print(f"  ✓ Deleted habit: {habitTitle} ({habitId})")
    
    print(f"✅ Deleted {len(habits)} habits\n")

def deleteAllHabitEntries():
    """Delete all habit entries from Firestore"""
    print("Deleting all habit entries...")
    
    entriesRef = db.collection("habitEntries")
    entries = list(entriesRef.stream())
    print(f"Found {len(entries)} entries")
    
    for entryDoc in entries:
        entryDoc.reference.delete()
    
    print(f"✅ Deleted {len(entries)} entries\n")

def main():
    print("=" * 60)
    print("FIREBASE DATABASE CLEANUP SCRIPT")
    print("=" * 60)
    print("\n⚠️  WARNING: This will delete ALL data from your database!")
    print("   - All users (Firestore + Auth)")
    print("   - All habits")
    print("   - All habit entries")
    print()
    
    response = input("Are you sure you want to continue? (type 'yes' to confirm): ")
    
    if response.lower() != 'yes':
        print("❌ Cleanup cancelled.")
        return
    
    print("\n" + "=" * 60)
    print("Starting cleanup...")
    print("=" * 60 + "\n")
    
    try:
        # Delete in order: entries -> habits -> users
        deleteAllHabitEntries()
        deleteAllHabits()
        deleteAllUsers()
        
        print("=" * 60)
        print("✅ CLEANUP COMPLETE!")
        print("=" * 60)
        print("\nAll data has been deleted from:")
        print("  - Firestore Database")
        print("  - Firebase Authentication")
        print("\nYou can now create new users with the updated requirements.")
        
    except Exception as e:
        print(f"\n❌ Error during cleanup: {str(e)}")
        sys.exit(1)

if __name__ == "__main__":
    main()
