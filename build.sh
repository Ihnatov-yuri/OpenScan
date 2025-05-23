#!/bin/bash

# OpenScan Build Script
echo "🔨 OpenScan Build Script"
echo "========================"

# Function to check if command was successful
check_result() {
    if [ $? -eq 0 ]; then
        echo "✅ $1 completed successfully"
    else
        echo "❌ $1 failed"
        exit 1
    fi
}

# Make gradlew executable
chmod +x ./gradlew
check_result "Making gradlew executable"

case "$1" in
    clean)
        echo "🧹 Cleaning project..."
        ./gradlew clean
        check_result "Clean"
        ;;
    debug)
        echo "🏗️ Building debug APK..."
        ./gradlew clean assembleDebug
        check_result "Debug build"
        echo "📱 Debug APK location: app/build/outputs/apk/debug/app-debug.apk"
        ;;
    release)
        echo "🏗️ Building release APK..."
        ./gradlew clean assembleRelease
        check_result "Release build"
        echo "📱 Release APK location: app/build/outputs/apk/release/app-release.apk"
        ;;
    install)
        echo "📱 Installing debug APK on connected device..."
        ./gradlew clean installDebug
        check_result "Install debug"
        ;;
    test)
        echo "🧪 Running tests..."
        ./gradlew test
        check_result "Tests"
        ;;
    lint)
        echo "🔍 Running lint checks..."
        ./gradlew lint
        check_result "Lint"
        ;;
    dependencies)
        echo "📦 Checking dependencies..."
        ./gradlew dependencies
        check_result "Dependencies check"
        ;;
    check)
        echo "🔧 Running full check (test + lint)..."
        ./gradlew clean check
        check_result "Full check"
        ;;
    help|*)
        echo "Usage: $0 [command]"
        echo ""
        echo "Available commands:"
        echo "  clean       - Clean the project"
        echo "  debug       - Build debug APK"
        echo "  release     - Build release APK"
        echo "  install     - Build and install debug APK"
        echo "  test        - Run unit tests"
        echo "  lint        - Run lint checks"
        echo "  dependencies- Check project dependencies"
        echo "  check       - Run tests and lint checks"
        echo "  help        - Show this help message"
        echo ""
        echo "Examples:"
        echo "  ./build.sh debug     # Build debug APK"
        echo "  ./build.sh install   # Install on connected device"
        echo "  ./build.sh check     # Run full checks"
        ;;
esac

echo "🎉 Build script completed!" 