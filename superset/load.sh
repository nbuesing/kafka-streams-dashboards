
#curl -i -L  -H "application/x-www-form-urlencoded" -d "username=admin&password=admin" http://localhost:28088/login/


URL=http://localhost:28088/api/v1/database/

CONTENT_TYPE_HDR='Content-Type:application/json'

DATA=config.json

curl -X POST -H "Authorization: Bearer eyJsb2NhbGUiOiJlbiJ9.YHbpeQ.tEA98N25vqaWs-uDWFIc3JJydgU" -H $CONTENT_TYPE_HDR -d "{}" $URL
