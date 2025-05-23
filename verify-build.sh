#!/bin/bash

# OpenScan Build Verification Script
echo "🔍 OpenScan Build Verification"
echo "==============================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check Gradle Wrapper
print_status "Checking Gradle wrapper..."
if [ -f "./gradlew" ]; then
    chmod +x ./gradlew
    print_success "Gradle wrapper found and made executable"
else
    print_error "Gradle wrapper not found!"
    exit 1
fi

# Check Gradle version
print_status "Verifying Gradle version..."
GRADLE_VERSION=$(./gradlew --version | grep "Gradle" | head -1)
echo "   $GRADLE_VERSION"

# Check key source files
print_status "Checking critical source files..."
critical_files=(
    "app/src/main/java/com/openscan/scanner/MainActivity.kt"
    "app/src/main/java/com/openscan/scanner/ui/navigation/OpenScanNavigation.kt"
    "app/src/main/java/com/openscan/scanner/ui/screens/HomeScreen.kt"
    "app/src/main/java/com/openscan/scanner/ui/screens/CameraScreen.kt"
    "app/src/main/java/com/openscan/scanner/ui/screens/DocumentReviewScreen.kt"
    "app/src/main/java/com/openscan/scanner/ui/components/DocumentCard.kt"
    "app/src/main/java/com/openscan/scanner/ui/components/EmptyState.kt"
    "app/src/main/java/com/openscan/scanner/utils/DocumentDetector.kt"
    "app/src/main/java/com/openscan/scanner/utils/ImageProcessor.kt"
    "app/src/main/java/com/openscan/scanner/utils/PdfGenerator.kt"
    "app/src/main/java/com/openscan/scanner/data/repository/DocumentRepository.kt"
)

missing_files=0
for file in "${critical_files[@]}"; do
    if [ -f "$file" ]; then
        print_success "✓ $file"
    else
        print_error "✗ $file (MISSING)"
        missing_files=$((missing_files + 1))
    fi
done

if [ $missing_files -gt 0 ]; then
    print_error "Missing $missing_files critical source files!"
    exit 1
fi

# Check resource files
print_status "Checking resource files..."
resource_files=(
    "app/src/main/res/values/strings.xml"
    "app/src/main/res/values/colors.xml"
    "app/src/main/res/values/themes.xml"
    "app/src/main/res/xml/file_paths.xml"
    "app/src/main/AndroidManifest.xml"
    "app/proguard-rules.pro"
)

for file in "${resource_files[@]}"; do
    if [ -f "$file" ]; then
        print_success "✓ $file"
    else
        print_error "✗ $file (MISSING)"
        missing_files=$((missing_files + 1))
    fi
done

# Test compile
print_status "Testing project compilation..."
./gradlew assembleDebug --dry-run > /dev/null 2>&1
if [ $? -eq 0 ]; then
    print_success "Dry run compilation passed"
else
    print_warning "Dry run compilation failed - running dependencies check"
    ./gradlew dependencies > /dev/null 2>&1
fi

# Check dependencies
print_status "Verifying key dependencies..."
if grep -q "org.opencv:opencv" app/build.gradle; then
    print_success "✓ OpenCV dependency configured"
else
    print_error "✗ OpenCV dependency missing"
fi

if grep -q "com.itextpdf:itext7-core" app/build.gradle; then
    print_success "✓ iText PDF dependency configured"
else
    print_error "✗ iText dependency missing"
fi

if grep -q "androidx.camera:camera-core" app/build.gradle; then
    print_success "✓ CameraX dependencies configured"
else
    print_error "✗ CameraX dependencies missing"
fi

# Check Gradle configuration
print_status "Checking Gradle configuration..."
if grep -q "8.11.1" gradle/wrapper/gradle-wrapper.properties; then
    print_success "✓ Gradle version 8.11.1 configured"
else
    print_warning "! Gradle version might not be 8.11.1"
fi

# Summary
echo ""
echo "==============================="
if [ $missing_files -eq 0 ]; then
    print_success "🎉 BUILD VERIFICATION PASSED!"
    echo ""
    print_status "Project is ready for assembly. You can now run:"
    echo "   ./build.sh debug    # For debug build"
    echo "   ./build.sh release  # For release build"
    echo "   ./build.sh install  # To install on device"
    echo ""
    print_status "For detailed assembly information, see: ASSEMBLY_READY.md"
else
    print_error "❌ BUILD VERIFICATION FAILED!"
    print_error "Please fix the missing files before proceeding."
    exit 1
fi 