#!/usr/bin/env bash
# Session 1 - capabilities of /todos and /todos/:id.
# Run against a freshly started runTodoManagerRestAPI-1.5.5.jar (seed data only).
# Usage: ./session1_todos_capabilities.sh            (default http://localhost:4567)
#        BASE=http://localhost:4568 ./session1_todos_capabilities.sh

BASE="${BASE:-http://localhost:4567}"
JSON='Content-Type: application/json'
XML='Content-Type: application/xml'

section() { printf '\n==================== %s\n' "$1"; }
req() { curl -s -w '\n[HTTP %{http_code}]\n' "$@"; }
req_headers() { curl -s -i "$@" | tr -d '\r' | grep -v -E '^(Date|Server|Transfer-Encoding):'; echo; }
id_in() { grep -o '"id":"[0-9]*"' | head -1 | grep -o '[0-9]\+'; }

section "List all todos"
req "$BASE/todos"

section "Filter by doneStatus, by exact title, by partial title (returns nothing)"
req "$BASE/todos?doneStatus=false"
req "$BASE/todos?title=scan%20paperwork"
req "$BASE/todos?title=scan"

section "HEAD on the todo list"
req_headers -I "$BASE/todos"

section "Create a todo from JSON"
GROCERIES=$(req -X POST "$BASE/todos" -H "$JSON" \
  -d '{"title":"Buy groceries","doneStatus":false,"description":"milk, eggs, bread"}')
echo "$GROCERIES"
GROCERIES_ID=$(echo "$GROCERIES" | id_in)

section "Create a todo from XML"
FLIGHT=$(req -X POST "$BASE/todos" -H "$XML" \
  -d '<todo><title>Book flight</title><doneStatus>false</doneStatus><description>Montreal to Toronto</description></todo>')
echo "$FLIGHT"
FLIGHT_ID=$(echo "$FLIGHT" | id_in)

section "Rejected creates: no title, doneStatus as a string, id in the body"
req -X POST "$BASE/todos" -H "$JSON" -d '{"description":"no title here"}'
req -X POST "$BASE/todos" -H "$JSON" -d '{"title":"Pay rent","doneStatus":"true"}'
req -X POST "$BASE/todos" -H "$JSON" -d '{"id":"50","title":"With id"}'

section "Read one todo as JSON and as XML; HEAD; missing todo"
req "$BASE/todos/$GROCERIES_ID"
req "$BASE/todos/$GROCERIES_ID" -H 'Accept: application/xml'
req_headers -I "$BASE/todos/$GROCERIES_ID"
req "$BASE/todos/999"

section "POST amends only the fields sent"
req -X POST "$BASE/todos/$GROCERIES_ID" -H "$JSON" -d '{"description":"milk, eggs, bread, coffee"}'
req -X POST "$BASE/todos/$GROCERIES_ID" -H "$JSON" -d '{"doneStatus":true}'

section "PUT (documented as amend) resets description and doneStatus when only a title is sent"
req -X PUT "$BASE/todos/$GROCERIES_ID" -H "$JSON" -d '{"title":"Buy groceries today"}'
req "$BASE/todos/$GROCERIES_ID"

section "Amend a missing todo"
req -X POST "$BASE/todos/999" -H "$JSON" -d '{"title":"ghost"}'

section "Malformed JSON and XML: 400 with a JSON body but Content-Type text/html"
req_headers -X POST "$BASE/todos" -H "$JSON" -d '{"title":"Broken'
req_headers -X POST "$BASE/todos" -H "$XML" -d '<todo><title>Broken</todo>'

section "Delete a todo, then delete it again"
req -X DELETE "$BASE/todos/$FLIGHT_ID"
req -X DELETE "$BASE/todos/$FLIGHT_ID"

section "Undocumented: OPTIONS lists the allowed methods; PATCH is not allowed"
req_headers -X OPTIONS "$BASE/todos"
req_headers -X OPTIONS "$BASE/todos/$GROCERIES_ID"
req -X PATCH "$BASE/todos/$GROCERIES_ID" -H "$JSON" -d '{"title":"patched"}'

section "A failed create still uses up an id (valid, failed, valid: ids skip by one)"
PLANTS=$(req -X POST "$BASE/todos" -H "$JSON" -d '{"title":"Water plants"}')
echo "$PLANTS"
req -X POST "$BASE/todos" -H "$JSON" -d '{"title":""}'
PASSPORT=$(req -X POST "$BASE/todos" -H "$JSON" -d '{"title":"Renew passport"}')
echo "$PASSPORT"

section "Cleanup: delete the todos this script created"
req -X DELETE "$BASE/todos/$GROCERIES_ID"
req -X DELETE "$BASE/todos/$(echo "$PLANTS" | id_in)"
req -X DELETE "$BASE/todos/$(echo "$PASSPORT" | id_in)"
req "$BASE/todos"
