#!/bin/bash
# NoteX Desktop - Run Script
# This script runs the NoteX Desktop application

# Set JAVA_HOME to Java 22
export JAVA_HOME=$(/usr/libexec/java_home -v 22)

# Navigate to project directory
cd "$(dirname "$0")"

# Run the application
./gradlew run
