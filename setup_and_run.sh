#!/bin/bash

# Habit Tracker Backend Setup and Run Script for Mac
# This script creates a virtual environment, installs dependencies, and runs the backend

echo "🚀 Habit Tracker Backend Setup"
echo "================================"
echo ""

# Check if Python 3 is installed
if ! command -v python3 &> /dev/null; then
    echo "❌ Python 3 is not installed. Please install Python 3 first."
    exit 1
fi

echo "✅ Python 3 found: $(python3 --version)"
echo ""

# Check if virtual environment already exists
if [ -d "venv" ]; then
    echo "📦 Virtual environment already exists"
    echo "   Activating existing virtual environment..."
    source venv/bin/activate
else
    echo "📦 Creating virtual environment..."
    python3 -m venv venv
    
    if [ $? -ne 0 ]; then
        echo "❌ Failed to create virtual environment"
        exit 1
    fi
    
    echo "✅ Virtual environment created"
    echo "   Activating virtual environment..."
    source venv/bin/activate
fi

echo ""

# Upgrade pip
echo "⬆️  Upgrading pip..."
pip install --upgrade pip --quiet

# Install requirements
echo "📥 Installing requirements..."
if [ -f "requirements.txt" ]; then
    pip install -r requirements.txt
    
    if [ $? -ne 0 ]; then
        echo "❌ Failed to install requirements"
        exit 1
    fi
    
    echo "✅ Requirements installed"
else
    echo "⚠️  requirements.txt not found"
fi

echo ""
echo "================================"
echo "✅ Setup complete!"
echo ""
echo "⚠️  IMPORTANT: Make sure you have:"
echo "   1. Firebase credentials file (firebase-config.json)"
echo "   2. Firestore Database enabled in Firebase Console"
echo "   3. Email/Password authentication enabled in Firebase Console"
echo ""
echo "🚀 Starting FastAPI backend server..."
echo "   Server will be available at: http://localhost:8000"
echo "   API docs at: http://localhost:8000/docs"
echo ""
echo "   Press Ctrl+C to stop the server"
echo "================================"
echo ""

# Run the backend
python main.py
