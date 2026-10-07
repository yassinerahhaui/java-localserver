#!/bin/bash
set -e

echo "=========================================================="
echo "🚀 LocalServer Audit Verification Script"
echo "=========================================================="

PORT=8080
BASE_URL="http://127.0.0.1:${PORT}"

# 1. Compile project
echo "📦 1. Compiling project..."
javac -d bin $(find src -name "*.java")

# 2. Run Java unit audit tests
echo "🧪 2. Running Core Audit Unit Tests..."
java -cp bin TestAudit

echo ""
echo "=========================================================="
echo "🎉 ALL MANDATORY AUDIT REQUIREMENTS VALIDATED SUCCESSFULLY!"
echo "=========================================================="

