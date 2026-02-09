from flask import Flask, render_template
from firebase_config import db
from datetime import datetime

app = Flask(__name__)

def getAllUsers():
    """Get all users from Firestore"""
    try:
        users = []
        usersRef = db.collection("users")
        for doc in usersRef.stream():
            userData = doc.to_dict()
            # Convert datetime to string for display
            if userData.get("createdAt"):
                if isinstance(userData["createdAt"], datetime):
                    userData["createdAt"] = userData["createdAt"].strftime("%Y-%m-%d %H:%M:%S")
            users.append(userData)
        return users
    except Exception as e:
        print(f"Error getting users: {str(e)}")
        return []

def getAllHabits():
    """Get all habits from Firestore"""
    try:
        habits = []
        habitsRef = db.collection("habits")
        for doc in habitsRef.stream():
            habitData = doc.to_dict()
            habits.append(habitData)
        return habits
    except Exception as e:
        print(f"Error getting habits: {str(e)}")
        return []

def getAllHabitEntries():
    """Get all habit entries from Firestore"""
    try:
        entries = []
        entriesRef = db.collection("habitEntries")
        for doc in entriesRef.stream():
            entryData = doc.to_dict()
            # Ensure ID is set
            if not entryData.get("id"):
                entryData["id"] = doc.id
            
            # Ensure habitId is set
            if not entryData.get("habitId"):
                entryData["habitId"] = entryData.get("habit_id", "")
            
            # Ensure date is formatted
            if entryData.get("date"):
                if isinstance(entryData["date"], str):
                    # Already a string, keep it
                    pass
                else:
                    entryData["date"] = str(entryData["date"])
            else:
                entryData["date"] = ""
            
            # Convert datetime to string for display
            if entryData.get("completedAt"):
                if isinstance(entryData["completedAt"], datetime):
                    entryData["completedAt"] = entryData["completedAt"].strftime("%Y-%m-%d %H:%M:%S")
                elif isinstance(entryData["completedAt"], str):
                    try:
                        # Try to parse ISO format
                        dt = datetime.fromisoformat(entryData["completedAt"].replace('Z', '+00:00'))
                        entryData["completedAt"] = dt.strftime("%Y-%m-%d %H:%M:%S")
                    except:
                        pass
            else:
                entryData["completedAt"] = ""
            
            # Ensure completed is a boolean
            if "completed" not in entryData:
                entryData["completed"] = False
            
            entries.append(entryData)
        
        # Sort by date (newest first)
        entries.sort(key=lambda x: x.get("date", ""), reverse=True)
        return entries
    except Exception as e:
        print(f"Error getting entries: {str(e)}")
        return []

def getStats():
    """Get statistics"""
    users = getAllUsers()
    habits = getAllHabits()
    entries = getAllHabitEntries()
    
    totalCompleted = sum(1 for e in entries if e.get("completed", False))
    totalEntries = len(entries)
    
    return {
        "totalUsers": len(users),
        "totalHabits": len(habits),
        "totalEntries": totalEntries,
        "completedEntries": totalCompleted,
        "activeHabits": sum(1 for h in habits if not h.get("isArchived", False)),
        "archivedHabits": sum(1 for h in habits if h.get("isArchived", False))
    }

@app.route("/")
def index():
    """Main admin dashboard"""
    try:
        users = getAllUsers()
        habits = getAllHabits()
        entries = getAllHabitEntries()
        stats = getStats()
        
        # Group habits by user
        habitsByUser = {}
        for habit in habits:
            userId = habit.get("userId", "unknown")
            if userId not in habitsByUser:
                habitsByUser[userId] = []
            habitsByUser[userId].append(habit)
        
        # Group entries by habit
        entriesByHabit = {}
        for entry in entries:
            habitId = entry.get("habitId", "unknown")
            if habitId not in entriesByHabit:
                entriesByHabit[habitId] = []
            entriesByHabit[habitId].append(entry)
        
        return render_template("admin.html", 
                             users=users,
                             habits=habits,
                             entries=entries,
                             stats=stats,
                             habitsByUser=habitsByUser,
                             entriesByHabit=entriesByHabit)
    except Exception as e:
        return f"Error loading data: {str(e)}", 500

if __name__ == "__main__":
    app.run(debug=True, host="0.0.0.0", port=5000)
