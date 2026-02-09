"""
Example script demonstrating how to use the Habit Tracker API
Run this after starting the server with: python main.py
"""

import requests
import json
from datetime import date
from urllib.parse import quote

BASE_URL = "http://localhost:8000/api"

def printResponse(response, title="Response"):
    print(f"\n{'='*50}")
    print(f"{title}")
    print(f"{'='*50}")
    print(f"Status: {response.status_code}")
    try:
        print(f"Data: {json.dumps(response.json(), indent=2)}")
    except:
        print(f"Text: {response.text}")

def main():
    print("Habit Tracker API - Example Usage\n")
    
    # 1. Check if user exists, if not create one
    email = "test@example.com"
    password = "testpassword123"
    name = "John Doe"  # User's display name
    
    print(f"\n1. Checking if user exists ({email})...")
    print(f"   Name: {name}")
    # URL encode email for the request
    encodedEmail = quote(email)
    userResponse = requests.get(f"{BASE_URL}/users/email/{encodedEmail}")
    
    if userResponse.status_code == 200:
        # User exists, use existing user
        print("✅ User already exists! Using existing user...")
        userInfo = userResponse.json()
        userId = userInfo["id"]
        userName = userInfo.get("name", "Unknown")
        print(f"User ID: {userId}")
        print(f"User Name: {userName}")
    else:
        # User doesn't exist, create new user
        print("📝 User doesn't exist. Creating new user...")
        userData = {
            "email": email,
            "password": password,
            "name": name
        }
        userResponse = requests.post(f"{BASE_URL}/users", json=userData)
        printResponse(userResponse, "Create User")
        
        if userResponse.status_code != 201:
            errorDetail = userResponse.json().get("detail", "Unknown error")
            if "EMAIL_EXISTS" in errorDetail:
                # Try to get user by email if creation failed due to existing email
                print("\n⚠️ User exists but couldn't retrieve. Trying to get by email...")
                encodedEmail = quote(email)
                userResponse = requests.get(f"{BASE_URL}/users/email/{encodedEmail}")
                if userResponse.status_code == 200:
                    userInfo = userResponse.json()
                    userId = userInfo["id"]
                    userName = userInfo.get("name", "Unknown")
                    print(f"✅ Found existing user! User ID: {userId}, Name: {userName}")
                else:
                    print("❌ Failed to get existing user.")
                    return
            else:
                print("❌ Failed to create user. Make sure Firebase is configured correctly.")
                return
        else:
            userInfo = userResponse.json()
            userId = userInfo["id"]
            userName = userInfo.get("name", "Unknown")
            print(f"✅ New user created! User ID: {userId}, Name: {userName}")
    
    # 2. Create a daily habit
    print("\n2. Creating a daily habit...")
    dailyHabit = {
        "title": "Morning Exercise",
        "frequency": "daily",
        "startDate": date.today().isoformat(),
        "userId": userId
    }
    habitResponse = requests.post(f"{BASE_URL}/habits", json=dailyHabit)
    printResponse(habitResponse, "Create Daily Habit")
    
    if habitResponse.status_code != 201:
        print("Failed to create habit.")
        return
    
    dailyHabitId = habitResponse.json()["id"]
    
    # 3. Create a weekly habit
    print("\n3. Creating a weekly habit...")
    weeklyHabit = {
        "title": "Weekly Review",
        "frequency": "weekly",
        "startDate": date.today().isoformat(),
        "userId": userId
    }
    weeklyResponse = requests.post(f"{BASE_URL}/habits", json=weeklyHabit)
    printResponse(weeklyResponse, "Create Weekly Habit")
    
    if weeklyResponse.status_code != 201:
        print("Failed to create weekly habit.")
        return
    
    weeklyHabitId = weeklyResponse.json()["id"]
    
    # 4. Check the daily habit (today)
    print("\n4. Checking daily habit for today...")
    checkData = {
        "habitId": dailyHabitId,
        "completed": True
    }
    checkResponse = requests.post(f"{BASE_URL}/habits/check", json=checkData)
    printResponse(checkResponse, "Check Daily Habit")
    
    # 5. Check the weekly habit
    print("\n5. Checking weekly habit...")
    weeklyCheck = {
        "habitId": weeklyHabitId,
        "completed": True
    }
    weeklyCheckResponse = requests.post(f"{BASE_URL}/habits/check", json=weeklyCheck)
    printResponse(weeklyCheckResponse, "Check Weekly Habit")
    
    # 6. Get all user habits
    print("\n6. Getting all user habits...")
    habitsResponse = requests.get(f"{BASE_URL}/users/{userId}/habits")
    printResponse(habitsResponse, "Get User Habits")
    
    # 7. Get entries for a habit
    print("\n7. Getting entries for daily habit...")
    entriesResponse = requests.get(f"{BASE_URL}/habits/{dailyHabitId}/entries")
    printResponse(entriesResponse, "Get Habit Entries")
    
    # 8. Get user entries for today
    print("\n8. Getting user entries for today...")
    todayEntries = requests.get(f"{BASE_URL}/users/{userId}/entries/{date.today().isoformat()}")
    printResponse(todayEntries, "Get User Entries for Today")
    
    print("\n" + "="*50)
    print("Example completed!")
    print("="*50)

if __name__ == "__main__":
    try:
        main()
    except requests.exceptions.ConnectionError:
        print("Error: Could not connect to the API server.")
        print("Make sure the server is running with: python main.py")
    except Exception as e:
        print(f"Error: {str(e)}")
