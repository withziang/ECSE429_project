#!/usr/bin/env bash
set -uo pipefail

BASE_URL="${BASE_URL:-http://localhost:4567}"
JSON="Content-Type: application/json"
XML="Content-Type: application/xml"
ACCEPT_XML="Accept: application/xml"
MISSING_ID=0

section() {
    printf '\n==== %s ====\n' "$1"
}

request() {
    printf '\n$ curl %s\n' "$*"
    curl -s -w '\n[status %{http_code}] [%{content_type}]\n' "$@"
}

create() {
    curl -s -X POST -H "$JSON" -d "$2" "$BASE_URL$1" | sed -E 's/^\{"id":"([0-9]+)".*/\1/'
}

section "/projects"
request "$BASE_URL/projects"
request -H "$ACCEPT_XML" "$BASE_URL/projects"
request -I "$BASE_URL/projects"
request -X POST -H "$JSON" -d '{"title":"Kitchen renovation","description":"Replace cabinets","completed":false,"active":true}' "$BASE_URL/projects"
request -X POST -H "$XML" -H "$ACCEPT_XML" -d '<project><title>Garden shed</title><active>true</active></project>' "$BASE_URL/projects"
request -X POST -H "$JSON" -d '{}' "$BASE_URL/projects"
request "$BASE_URL/projects?active=true"
request "$BASE_URL/projects?title=Garden%20shed&completed=false"

project=$(create /projects '{"title":"Bathroom renovation","description":"Retile the shower","active":true}')
todo=$(create /todos '{"title":"Order tiles"}')
category=$(create /categories '{"title":"Renovation"}')

section "/projects/:id"
request "$BASE_URL/projects/$project"
request -H "$ACCEPT_XML" "$BASE_URL/projects/$project"
request -I "$BASE_URL/projects/$project"
request -X POST -H "$JSON" -d '{"description":"Retile the shower and floor"}' "$BASE_URL/projects/$project"
request -X PUT -H "$JSON" -d '{"title":"Bathroom renovation","description":"Retile everything","completed":true,"active":false}' "$BASE_URL/projects/$project"

section "/projects/:id/tasks"
request "$BASE_URL/projects/$project/tasks"
request -X POST -H "$JSON" -d "{\"id\":\"$todo\"}" "$BASE_URL/projects/$project/tasks"
request -X POST -H "$JSON" -d "{\"id\":\"$todo\"}" "$BASE_URL/projects/$project/tasks"
request "$BASE_URL/projects/$project/tasks"
request -H "$ACCEPT_XML" "$BASE_URL/projects/$project/tasks"
request -I "$BASE_URL/projects/$project/tasks"
request "$BASE_URL/todos/$todo"

section "/projects/:id/tasks/:id"
request -X DELETE "$BASE_URL/projects/$project/tasks/$todo"
request -X DELETE "$BASE_URL/projects/$project/tasks/$todo"
request "$BASE_URL/todos/$todo"

section "/projects/:id/categories"
request "$BASE_URL/projects/$project/categories"
request -X POST -H "$JSON" -d "{\"id\":\"$category\"}" "$BASE_URL/projects/$project/categories"
request "$BASE_URL/projects/$project/categories"
request -H "$ACCEPT_XML" "$BASE_URL/projects/$project/categories"
request -I "$BASE_URL/projects/$project/categories"
request "$BASE_URL/categories/$category/projects"

section "/projects/:id/categories/:id"
request -X DELETE "$BASE_URL/projects/$project/categories/$category"
request -X DELETE "$BASE_URL/projects/$project/categories/$category"

section "Undocumented capabilities"
request -i -X OPTIONS "$BASE_URL/projects"
request -i -X OPTIONS "$BASE_URL/projects/$project"
request -X PUT -H "$JSON" -d '{}' "$BASE_URL/projects"
request -X DELETE "$BASE_URL/projects"
request -X PATCH -H "$JSON" -d '{}' "$BASE_URL/projects/$project"
request -X POST -H "$JSON" -d '{"title":"Buy grout"}' "$BASE_URL/projects/$project/tasks"
request -X POST -H "$JSON" -d '{"title":"Plumbing"}' "$BASE_URL/projects/$project/categories"
request -X POST -H "$JSON" -d "{\"title\":\"Hallway\",\"tasks\":[{\"id\":\"$todo\"}]}" "$BASE_URL/projects"

section "Invalid operations"
request -X POST -H "$JSON" -d '{"title":"Kitchen renovation"' "$BASE_URL/projects"
request -X POST -H "$XML" -d '<project><title>Kitchen renovation</project>' "$BASE_URL/projects"
request -X POST -H "$JSON" -d '{"id":"500","title":"Kitchen renovation"}' "$BASE_URL/projects"
request -X POST -H "$JSON" -d '{"title":"Kitchen renovation","deadline":"Friday"}' "$BASE_URL/projects"
request -X POST -H "$JSON" -d '{"active":"sometimes"}' "$BASE_URL/projects/$project"
request -X POST -d 'title=Kitchen' "$BASE_URL/projects"
request -H "Accept: text/plain" "$BASE_URL/projects"
request "$BASE_URL/projects/$MISSING_ID"
request -I "$BASE_URL/projects/$MISSING_ID"
request -X POST -H "$JSON" -d '{"title":"Nothing"}' "$BASE_URL/projects/$MISSING_ID"
request -X PUT -H "$JSON" -d '{"title":"Nothing"}' "$BASE_URL/projects/$MISSING_ID"
request -X DELETE "$BASE_URL/projects/$MISSING_ID"
request -X POST -H "$JSON" -d "{\"id\":\"$MISSING_ID\"}" "$BASE_URL/projects/$project/tasks"
request -X POST -H "$JSON" -d "{\"id\":\"$todo\"}" "$BASE_URL/projects/$MISSING_ID/tasks"

section "Behavior that differs from the documentation"
linked=$(create /projects '{"title":"Linked project","description":"Has a task","active":true}')
curl -s -o /dev/null -X POST -H "$JSON" -d "{\"id\":\"$todo\"}" "$BASE_URL/projects/$linked/tasks"
request "$BASE_URL/projects/$linked"
request -X POST -H "$JSON" -d '{"title":"Renamed by POST"}' "$BASE_URL/projects/$linked"
request -X PUT -H "$JSON" -d '{"title":"Renamed by PUT"}' "$BASE_URL/projects/$linked"
request "$BASE_URL/projects/$MISSING_ID/tasks"
request "$BASE_URL/projects/$MISSING_ID/categories"
request -X DELETE "$BASE_URL/projects/$MISSING_ID/tasks/1"
request -X POST -H "$XML" -d "<todo><id>$todo</id></todo>" "$BASE_URL/projects/$project/tasks"
request -X POST -H "$JSON" -d '{"title":"a qui officia deseru","completed":"false","active":"true","description":"t in voluptate velit"}' "$BASE_URL/projects"
request -X POST -H "$XML" -d '<project><active>true</active><description>t in voluptate velit</description><id>null</id><completed>false</completed><title>a qui officia deseru</title></project>' "$BASE_URL/projects"

section "/projects/:id DELETE"
request -X DELETE "$BASE_URL/projects/$project"
request -X DELETE "$BASE_URL/projects/$project"
request "$BASE_URL/projects"
