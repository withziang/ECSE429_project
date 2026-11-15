Feature: Find my todos
  As a student, I want to find my todos by their done status or by their id,
  so that I can see what is left to do and check the details of one piece of work.

  Background:
    Given the Todo Manager is running
    And a todo titled "Assignment 3" exists
    And the todo "Assignment 3" is marked as done
    And a todo titled "Lab 2" exists

  Scenario Outline: Filter my todos by done status (normal flow)
    When I list the todos with done status "<status>"
    Then the response status is 200
    And the list includes the todo "<included>"
    And the list does not include the todo "<excluded>"

    Examples:
      | status | included     | excluded     |
      | true   | Assignment 3 | Lab 2        |
      | false  | Lab 2        | Assignment 3 |

  Scenario Outline: Look up a todo by its id (alternate flow)
    When I look up the todo "<title>" in <format>
    Then the response status is 200
    And the <format> response shows the todo "<title>"

    Examples:
      | title        | format |
      | Assignment 3 | JSON   |
      | Lab 2        | XML    |

  Scenario Outline: Look up a todo that does not exist (error flow)
    When I look up the todo with id "<id>"
    Then the response status is 404
    And the error message contains "Could not find an instance with todos/<id>"

    Examples:
      | id |
      | 0  |
      | -1 |
