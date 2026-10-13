#!/usr/bin/env bash
# Session 2 - capabilities of /todos/:id/tasksof(/:id) and /todos/:id/categories(/:id).
# Run against a freshly started runTodoManagerRestAPI-1.5.5.jar (seed data only).
# Usage: ./session2_todo_relationships_capabilities.sh            (default http://localhost:4567)
#        BASE=http://localhost:4568 ./session2_todo_relationships_capabilities.sh

BASE="${BASE:-http://localhost:4567}"
JSON='Content-Type: application/json'
XML='Content-Type: application/xml'

section() { printf '\n==================== %s\n' "$1"; }
req() { curl -s -w '\n[HTTP %{http_code}]\n' "$@"; }
req_headers() { curl -s -i "$@" | tr -d '\r' | grep -v -E '^(Date|Server|Transfer-Encoding):'; echo; }
id_in() { grep -o '"id":"[0-9]*"' | head -1 | grep -o '[0-9]\+'; }

section "Projects and categories of seed todo 1 (GET and HEAD)"
req "$BASE/todos/1/tasksof"
req_headers -I "$BASE/todos/1/tasksof"
req "$BASE/todos/1/categories"
req_headers -I "$BASE/todos/1/categories"

section "Create a todo to link"
REPORT=$(req -X POST "$BASE/todos" -H "$JSON" \
  -d '{"title":"Submit expense report","doneStatus":false,"description":"September receipts"}')
echo "$REPORT"
REPORT_ID=$(echo "$REPORT" | id_in)

section "Link the todo to project 1, list its projects, unlink"
req -X POST "$BASE/todos/$REPORT_ID/tasksof" -H "$JSON" -d '{"id":"1"}'
req "$BASE/todos/$REPORT_ID/tasksof"
req -X DELETE "$BASE/todos/$REPORT_ID/tasksof/1"

section "Projects of a missing todo: 200 with other todos' projects instead of 404"
req "$BASE/todos/999/tasksof"

section "Link the todo to category 2, list its categories, unlink twice"
req -X POST "$BASE/todos/$REPORT_ID/categories" -H "$JSON" -d '{"id":"2"}'
req "$BASE/todos/$REPORT_ID/categories"
req -X DELETE "$BASE/todos/$REPORT_ID/categories/2"
req -X DELETE "$BASE/todos/$REPORT_ID/categories/2"

section "Missing todo: linking gives 404, but GET and HEAD of its categories give 200"
req -X POST "$BASE/todos/999/categories" -H "$JSON" -d '{"id":"1"}'
req "$BASE/todos/999/categories"
req_headers -I "$BASE/todos/999/categories"

section "Linking with an XML body fails although category 1 exists"
req -X POST "$BASE/todos/$REPORT_ID/categories" -H "$XML" -d '<category><id>1</id></category>'

section "Undocumented: a title instead of an id creates a new category and links it"
ERRANDS=$(req -X POST "$BASE/todos/$REPORT_ID/categories" -H "$JSON" -d '{"title":"Errands"}')
echo "$ERRANDS"
ERRANDS_ID=$(echo "$ERRANDS" | id_in)

section "PUT with only a title removes the todo's category and project links"
req -X POST "$BASE/todos/$REPORT_ID/tasksof" -H "$JSON" -d '{"id":"1"}'
req "$BASE/todos/$REPORT_ID/categories"
req -X PUT "$BASE/todos/$REPORT_ID" -H "$JSON" -d '{"title":"Submit expense report"}'
req "$BASE/todos/$REPORT_ID/categories"
req "$BASE/todos/$REPORT_ID/tasksof"

section "Undocumented: OPTIONS on a category link allows only DELETE"
req_headers -X OPTIONS "$BASE/todos/1/categories/1"

section "Cleanup: delete the todo and the category this script created"
req -X DELETE "$BASE/todos/$REPORT_ID"
req -X DELETE "$BASE/categories/$ERRANDS_ID"
req "$BASE/todos"
