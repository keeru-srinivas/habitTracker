from firebase_admin import auth
from firebase_config import db
from models import UserCreate, HabitCreate, HabitUpdate, HabitEntryCreate, HabitEntryUpdate, HabitCheckRequest
from datetime import datetime, date
from typing import List, Optional
import hashlib
import secrets

# Password utilities
def hashPassword(password: str) -> str:
    """Hash password using SHA256"""
    salt = secrets.token_hex(16)
    return hashlib.sha256((password + salt).encode()).hexdigest() + ":" + salt

def verifyPassword(password: str, passwordHash: str) -> bool:
    """Verify password against hash"""
    try:
        hashPart, salt = passwordHash.split(":")
        return hashlib.sha256((password + salt).encode()).hexdigest() == hashPart
    except:
        return False

# User functions
async def createUser(userData: UserCreate) -> dict:
    """Create a new user in Firebase Auth and Firestore"""
    try:
        userRecord = auth.create_user(
            email=userData.email,
            password=userData.password
        )
        
        passwordHash = hashPassword(userData.password)
        
        # Get name from userData, default to empty string if not provided
        userName = userData.name if userData.name else ""
        
        userDoc = {
            "id": userRecord.uid,
            "email": userData.email,
            "name": userName,
            "passwordHash": passwordHash,
            "createdAt": datetime.now()
        }
        
        db.collection("users").document(userRecord.uid).set(userDoc)
        
        return {
            "id": userRecord.uid,
            "email": userData.email,
            "name": userName,
            "createdAt": userDoc["createdAt"]
        }
    except Exception as e:
        raise Exception(f"Error creating user: {str(e)}")

async def getUser(userId: str) -> dict:
    """Get user by ID"""
    try:
        userDoc = db.collection("users").document(userId).get()
        if not userDoc.exists:
            raise Exception("User not found")
        
        userData = userDoc.to_dict()
        return {
            "id": userData["id"],
            "email": userData["email"],
            "name": userData.get("name", ""),
            "createdAt": userData["createdAt"]
        }
    except Exception as e:
        raise Exception(f"Error getting user: {str(e)}")

async def updateUserName(userId: str, name: str) -> dict:
    """Update user's name"""
    try:
        userRef = db.collection("users").document(userId)
        userDoc = userRef.get()
        
        if not userDoc.exists:
            raise Exception("User not found")
        
        userRef.update({"name": name})
        
        # Return updated user
        updatedData = userRef.get().to_dict()
        return {
            "id": updatedData["id"],
            "email": updatedData["email"],
            "name": updatedData.get("name", ""),
            "createdAt": updatedData["createdAt"]
        }
    except Exception as e:
        raise Exception(f"Error updating user name: {str(e)}")

async def getUserByEmail(email: str) -> dict:
    """Get user by email - checks Firestore first, then Firebase Auth if not found"""
    try:
        # First, try to find in Firestore
        usersRef = db.collection("users")
        query = usersRef.where("email", "==", email).limit(1)
        docs = list(query.stream())
        
        if docs:
            userData = docs[0].to_dict()
            return {
                "id": userData["id"],
                "email": userData["email"],
                "name": userData.get("name", ""),
                "createdAt": userData["createdAt"]
            }
        
        # If not found in Firestore, check Firebase Auth
        try:
            userRecord = auth.get_user_by_email(email)
            # User exists in Auth but not in Firestore - create Firestore document
            userDoc = {
                "id": userRecord.uid,
                "email": userRecord.email,
                "name": userRecord.display_name if userRecord.display_name else "",
                "passwordHash": "",  # Can't retrieve password hash
                "createdAt": datetime.now()
            }
            
            db.collection("users").document(userRecord.uid).set(userDoc)
            
            return {
                "id": userRecord.uid,
                "email": userRecord.email,
                "name": userDoc["name"],
                "createdAt": userDoc["createdAt"]
            }
        except Exception as authError:
            # User doesn't exist in Auth either
            raise Exception("User not found")
            
    except Exception as e:
        raise Exception(f"Error getting user: {str(e)}")

# Habit functions
async def createHabit(habitData: HabitCreate) -> dict:
    """Create a new habit"""
    try:
        userDoc = db.collection("users").document(habitData.userId).get()
        if not userDoc.exists:
            raise Exception("User not found")
        
        habitDoc = {
            "id": "",
            "userId": habitData.userId,
            "title": habitData.title,
            "frequency": habitData.frequency.value,
            "startDate": habitData.startDate.isoformat(),
            "isArchived": False
        }
        
        docRef = db.collection("habits").document()
        habitDoc["id"] = docRef.id
        docRef.set(habitDoc)
        
        return habitDoc
    except Exception as e:
        raise Exception(f"Error creating habit: {str(e)}")

async def getHabit(habitId: str) -> dict:
    """Get habit by ID"""
    try:
        habitDoc = db.collection("habits").document(habitId).get()
        if not habitDoc.exists:
            raise Exception("Habit not found")
        
        return habitDoc.to_dict()
    except Exception as e:
        raise Exception(f"Error getting habit: {str(e)}")

async def getUserHabits(userId: str, includeArchived: bool = False) -> List[dict]:
    """Get all habits for a user"""
    try:
        habitsRef = db.collection("habits")
        query = habitsRef.where("userId", "==", userId)
        
        if not includeArchived:
            query = query.where("isArchived", "==", False)
        
        habits = []
        for doc in query.stream():
            habits.append(doc.to_dict())
        
        return habits
    except Exception as e:
        raise Exception(f"Error getting user habits: {str(e)}")

async def updateHabit(habitId: str, habitUpdate: HabitUpdate) -> dict:
    """Update a habit"""
    try:
        habitRef = db.collection("habits").document(habitId)
        habitDoc = habitRef.get()
        
        if not habitDoc.exists:
            raise Exception("Habit not found")
        
        updateData = {}
        if habitUpdate.title is not None:
            updateData["title"] = habitUpdate.title
        if habitUpdate.frequency is not None:
            updateData["frequency"] = habitUpdate.frequency.value
        if habitUpdate.isArchived is not None:
            updateData["isArchived"] = habitUpdate.isArchived
        
        habitRef.update(updateData)
        
        return habitRef.get().to_dict()
    except Exception as e:
        raise Exception(f"Error updating habit: {str(e)}")

async def deleteHabit(habitId: str) -> bool:
    """Delete a habit"""
    try:
        db.collection("habits").document(habitId).delete()
        return True
    except Exception as e:
        raise Exception(f"Error deleting habit: {str(e)}")

# Habit Entry functions
async def createHabitEntry(entryData: HabitEntryCreate) -> dict:
    """Create a new habit entry"""
    try:
        habitDoc = db.collection("habits").document(entryData.habitId).get()
        if not habitDoc.exists:
            raise Exception("Habit not found")
        
        entryDoc = {
            "id": "",
            "habitId": entryData.habitId,
            "date": entryData.date.isoformat(),
            "completed": entryData.completed,
            "completedAt": datetime.now().isoformat() if entryData.completed else None
        }
        
        docRef = db.collection("habitEntries").document()
        entryDoc["id"] = docRef.id
        docRef.set(entryDoc)
        
        return entryDoc
    except Exception as e:
        raise Exception(f"Error creating habit entry: {str(e)}")

async def checkHabit(checkRequest: HabitCheckRequest) -> dict:
    """Check a habit for a specific date (daily or weekly check)"""
    try:
        habitDoc = db.collection("habits").document(checkRequest.habitId).get()
        if not habitDoc.exists:
            raise Exception("Habit not found")
        
        habitData = habitDoc.to_dict()
        frequency = habitData.get("frequency", "daily")
        startDate = date.fromisoformat(habitData.get("startDate"))
        
        checkDate = checkRequest.date if checkRequest.date else date.today()
        
        # Validate date based on frequency
        if frequency == "weekly":
            daysSinceStart = (checkDate - startDate).days
            if daysSinceStart < 0:
                raise Exception("Check date cannot be before habit start date")
            
            if checkDate.weekday() != startDate.weekday():
                raise Exception(f"Weekly habit can only be checked on {startDate.strftime('%A')}")
        else:  # daily
            if checkDate < startDate:
                raise Exception("Check date cannot be before habit start date")
        
        # Check if entry already exists for this date
        entriesRef = db.collection("habitEntries")
        existingQuery = entriesRef.where("habitId", "==", checkRequest.habitId)\
                                 .where("date", "==", checkDate.isoformat())\
                                 .limit(1)
        
        existingDocs = list(existingQuery.stream())
        
        if existingDocs:
            # Update existing entry
            entryRef = existingDocs[0].reference
            updateData = {
                "completed": checkRequest.completed,
                "completedAt": datetime.now().isoformat() if checkRequest.completed else None
            }
            entryRef.update(updateData)
            entryData = entryRef.get().to_dict()
        else:
            # Create new entry
            entryData = await createHabitEntry(HabitEntryCreate(
                habitId=checkRequest.habitId,
                date=checkDate,
                completed=checkRequest.completed
            ))
        
        return entryData
    except Exception as e:
        raise Exception(f"Error checking habit: {str(e)}")

async def getHabitEntry(entryId: str) -> dict:
    """Get habit entry by ID"""
    try:
        entryDoc = db.collection("habitEntries").document(entryId).get()
        if not entryDoc.exists:
            raise Exception("Habit entry not found")
        
        return entryDoc.to_dict()
    except Exception as e:
        raise Exception(f"Error getting habit entry: {str(e)}")

async def getHabitEntries(habitId: str, startDate: Optional[date] = None, endDate: Optional[date] = None) -> List[dict]:
    """Get all entries for a habit, optionally filtered by date range"""
    try:
        entriesRef = db.collection("habitEntries")
        query = entriesRef.where("habitId", "==", habitId)
        
        entries = []
        for doc in query.stream():
            entryData = doc.to_dict()
            entryDate = date.fromisoformat(entryData.get("date"))
            
            if startDate and entryDate < startDate:
                continue
            if endDate and entryDate > endDate:
                continue
            
            entries.append(entryData)
        
        entries.sort(key=lambda x: x.get("date", ""))
        
        return entries
    except Exception as e:
        raise Exception(f"Error getting habit entries: {str(e)}")

async def getUserEntriesForDate(userId: str, checkDate: date) -> List[dict]:
    """Get all habit entries for a user on a specific date"""
    try:
        habitsRef = db.collection("habits")
        habitsQuery = habitsRef.where("userId", "==", userId).where("isArchived", "==", False)
        
        habitIds = [doc.id for doc in habitsQuery.stream()]
        
        if not habitIds:
            return []
        
        entries = []
        entriesRef = db.collection("habitEntries")
        
        for habitId in habitIds:
            query = entriesRef.where("habitId", "==", habitId)\
                             .where("date", "==", checkDate.isoformat())\
                             .limit(1)
            
            for doc in query.stream():
                entries.append(doc.to_dict())
        
        return entries
    except Exception as e:
        raise Exception(f"Error getting user entries for date: {str(e)}")

async def updateHabitEntry(entryId: str, entryUpdate: HabitEntryUpdate) -> dict:
    """Update a habit entry"""
    try:
        entryRef = db.collection("habitEntries").document(entryId)
        entryDoc = entryRef.get()
        
        if not entryDoc.exists:
            raise Exception("Habit entry not found")
        
        updateData = {}
        if entryUpdate.completed is not None:
            updateData["completed"] = entryUpdate.completed
            if entryUpdate.completed:
                updateData["completedAt"] = datetime.now().isoformat()
            else:
                updateData["completedAt"] = None
        
        if entryUpdate.completedAt is not None:
            updateData["completedAt"] = entryUpdate.completedAt.isoformat()
        
        entryRef.update(updateData)
        
        return entryRef.get().to_dict()
    except Exception as e:
        raise Exception(f"Error updating habit entry: {str(e)}")
