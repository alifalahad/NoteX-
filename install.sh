#!/bin/bash

# NoteX Desktop - Installation Script
# This script builds and creates a distributable JAR file

echo "=========================================="
echo "NoteX Desktop - Installation Script"
echo "=========================================="
echo ""

# Check Java version
echo "Checking Java installation..."
if ! command -v /usr/libexec/java_home &> /dev/null
then
    echo "❌ Error: Java not found. Please install Java 22."
    exit 1
fi

# Set Java 22
export JAVA_HOME=$(/usr/libexec/java_home -v 22)
JAVA_VERSION=$("$JAVA_HOME/bin/java" -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)

echo "✅ Java Home: $JAVA_HOME"
echo "✅ Java Version: $JAVA_VERSION"
echo ""

if [ "$JAVA_VERSION" -lt 22 ]; then
    echo "❌ Error: Java 22 or higher required. Found Java $JAVA_VERSION"
    exit 1
fi

# Clean previous builds
echo "Cleaning previous builds..."
./gradlew clean
echo ""

# Build the application
echo "Building NoteX Desktop..."
./gradlew build
if [ $? -ne 0 ]; then
    echo "❌ Build failed!"
    exit 1
fi
echo ""

# Create fat JAR
echo "Creating distributable JAR..."
./gradlew jar
if [ $? -ne 0 ]; then
    echo "❌ JAR creation failed!"
    exit 1
fi
echo ""

# Copy JAR to distribution folder
echo "Creating distribution package..."
mkdir -p dist
cp build/libs/*.jar dist/NoteX-Desktop.jar
echo ""

# Create run script for the JAR
cat > dist/run-notex.sh << 'EOF'
#!/bin/bash
# Run NoteX Desktop from JAR
export JAVA_HOME=$(/usr/libexec/java_home -v 22)
"$JAVA_HOME/bin/java" -jar NoteX-Desktop.jar
EOF

chmod +x dist/run-notex.sh

echo "=========================================="
echo "✅ Installation Complete!"
echo "=========================================="
echo ""
echo "Distribution package created in: ./dist/"
echo ""
echo "Files created:"
echo "  - NoteX-Desktop.jar    (Executable JAR)"
echo "  - run-notex.sh         (Run script)"
echo ""
echo "To run the application:"
echo "  1. cd dist"
echo "  2. ./run-notex.sh"
echo ""
echo "Or run directly:"
echo "  java -jar dist/NoteX-Desktop.jar"
echo ""
echo "=========================================="
