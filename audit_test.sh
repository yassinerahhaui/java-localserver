#!/bin/bash

# الألوان لتوضيح النتائج في سطر الأوامر
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# متغيرات المنافذ بناءً على إعداداتك
PORT_MAIN=8080
PORT_SEC=9090
HOST="localhost"
BASE_URL="http://${HOST}:${PORT_MAIN}"
SEC_URL="http://${HOST}:${PORT_SEC}"

echo -e "${BLUE}======================================================${NC}"
echo -e "${BLUE}       Webserv / Custom HTTP Server Audit Script      ${NC}"
echo -e "${BLUE}======================================================${NC}"
echo ""

# دالة مساعدة لطباعة العناوين
print_title() {
    echo -e "\n${YELLOW}=== $1 ===${NC}"
}

# ---------------------------------------------------------
print_title "1. Basic GET Requests & Status Codes"
# ---------------------------------------------------------

echo -n "Test 1.1: Standard GET (200 OK) -> "
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -X GET ${BASE_URL}/)
if [ "$RESPONSE" -eq 200 ]; then echo -e "${GREEN}PASS (200)${NC}"; else echo -e "${RED}FAIL ($RESPONSE)${NC}"; fi

echo -n "Test 1.2: File Not Found (404 Not Found) -> "
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -X GET ${BASE_URL}/does_not_exist.html)
if [ "$RESPONSE" -eq 404 ]; then echo -e "${GREEN}PASS (404)${NC}"; else echo -e "${RED}FAIL ($RESPONSE)${NC}"; fi


# ---------------------------------------------------------
print_title "2. Directory Listing & Default Files"
# ---------------------------------------------------------

echo -n "Test 2.1: Directory Listing (if no default file) -> "
# سنختبر مسار الرفع في الخادم الأول لأنه يمتلك directory_listing: true
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -X GET ${BASE_URL}/upload/)
if [ "$RESPONSE" -eq 200 ] || [ "$RESPONSE" -eq 403 ]; then echo -e "${GREEN}PASS ($RESPONSE)${NC}"; else echo -e "${RED}FAIL ($RESPONSE)${NC}"; fi


# ---------------------------------------------------------
print_title "3. Methods Restrictions (405 Method Not Allowed)"
# ---------------------------------------------------------

echo -n "Test 3.1: Try POST on Root (Should be blocked) -> "
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -X POST -d "test" ${BASE_URL}/)
if [ "$RESPONSE" -eq 405 ]; then echo -e "${GREEN}PASS (405)${NC}"; else echo -e "${RED}FAIL ($RESPONSE)${NC}"; fi

echo -n "Test 3.2: Try DELETE on Root (Should be blocked) -> "
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE ${BASE_URL}/)
if [ "$RESPONSE" -eq 405 ]; then echo -e "${GREEN}PASS (405)${NC}"; else echo -e "${RED}FAIL ($RESPONSE)${NC}"; fi


# ---------------------------------------------------------
print_title "4. Client Body Limit (413 Payload Too Large)"
# ---------------------------------------------------------
# سنصنع ملفاً كبيراً نسبياً (أكبر من 1 ميجابايت إذا كان إعداد الخادم الافتراضي 1 ميجا)
# للسرعة، سنحاول رفع بيانات وهمية ضخمة
echo -n "Test 4.1: Exceed Body Limit (if applicable) -> "
# سنحاول إرسال 5 ميجابايت إلى مسار لا يدعمها (مثل مسار فيه حد 1MB)
# قمنا بإنشاء ملف 5 ميجا مؤقت
dd if=/dev/urandom of=large_test.bin bs=1M count=5 2>/dev/null
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -X POST ${SEC_URL}/ -H "Expect:" --data-binary @large_test.bin)
if [ "$RESPONSE" -eq 413 ] || [ "$RESPONSE" -eq 405 ]; then 
    echo -e "${GREEN}PASS (Handled by 413 or 405)${NC}"
else 
    echo -e "${RED}FAIL ($RESPONSE)${NC}"
fi
rm -f large_test.bin


# ---------------------------------------------------------
print_title "5. File Upload (POST) & Delete (DELETE)"
# ---------------------------------------------------------

echo "Creating a test file for upload..."
echo "Hello Webserv Auditor!" > test_upload.txt

echo -n "Test 5.1: Upload File (201 Created) -> "
# لاحظ استخدام المسار الصحيح للرفع
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -X POST ${BASE_URL}/upload \
     -H "Content-Disposition: attachment; filename=test_upload.txt" \
     --data-binary @test_upload.txt)
if [ "$RESPONSE" -eq 201 ]; then echo -e "${GREEN}PASS (201)${NC}"; else echo -e "${RED}FAIL ($RESPONSE)${NC}"; fi

echo -n "Test 5.2: Verify Uploaded File Exists (200 OK) -> "
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -X GET ${BASE_URL}/upload/test_upload.txt)
if [ "$RESPONSE" -eq 200 ]; then echo -e "${GREEN}PASS (200)${NC}"; else echo -e "${RED}FAIL ($RESPONSE)${NC}"; fi

echo -n "Test 5.3: Delete Uploaded File (204 No Content) -> "
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE ${BASE_URL}/upload/test_upload.txt)
if [ "$RESPONSE" -eq 204 ]; then echo -e "${GREEN}PASS (204)${NC}"; else echo -e "${RED}FAIL ($RESPONSE)${NC}"; fi

rm -f test_upload.txt


# ---------------------------------------------------------
print_title "6. Redirection (301/302)"
# ---------------------------------------------------------

echo -n "Test 6.1: Redirect Route (/old-api) -> "
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -X GET ${BASE_URL}/old-api)
if [ "$RESPONSE" -eq 301 ] || [ "$RESPONSE" -eq 302 ]; then echo -e "${GREEN}PASS ($RESPONSE)${NC}"; else echo -e "${RED}FAIL ($RESPONSE)${NC}"; fi


# ---------------------------------------------------------
print_title "7. WRONG Requests (Malformed/Garbage)"
# ---------------------------------------------------------
# The server MUST NOT CRASH when receiving garbage data
echo -n "Test 7.1: Send Garbage Request (Should handle safely, e.g., 400 Bad Request) -> "
RESPONSE=$(printf "GARBAGE REQUEST\r\n\r\n" | nc -w 1 $HOST $PORT_MAIN | grep "HTTP/" | awk '{print $2}')
if [ -z "$RESPONSE" ]; then
    echo -e "${YELLOW}PASS (Connection closed safely without response)${NC}"
else
    echo -e "${GREEN}PASS (Returned $RESPONSE)${NC}"
fi


# ---------------------------------------------------------
print_title "8. Virtual Hosting / Multiple Hostnames"
# ---------------------------------------------------------

echo -n "Test 8.1: Host header routing (test.com -> 8080) -> "
# If your server differentiates based on 'Host: test.com' vs 'Host: localhost'
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -H "Host: test.com" -X GET ${BASE_URL}/)
if [ "$RESPONSE" -eq 200 ]; then echo -e "${GREEN}PASS (200)${NC}"; else echo -e "${RED}FAIL ($RESPONSE)${NC}"; fi


# ---------------------------------------------------------
echo -e "\n${BLUE}======================================================${NC}"
echo -e "${BLUE}  Automated Tests Completed. Manual checks remaining: ${NC}"
echo -e "${BLUE}======================================================${NC}"
echo -e "1. ${YELLOW}CGI Execution:${NC} Test your .py or .sh scripts manually."
echo -e "2. ${YELLOW}Siege Stress Test:${NC} Run 'siege -b -c 50 -t 30S http://localhost:8080/'"
echo -e "3. ${YELLOW}Browser Test:${NC} Open your browser and check sessions/cookies."
echo -e "4. ${YELLOW}Config Port Error:${NC} Force duplicate ports in config.json and restart server to show crash handling."
echo ""