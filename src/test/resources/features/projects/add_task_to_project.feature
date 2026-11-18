Feature: Add a task to a project
  As a student, I want to add tasks to a course project,
  so that I know every piece of work the project needs.

  Background:
    Given the Todo Manager is running
    And a project titled "ECSE 429 project" exists

  Scenario Outline: Add an existing todo to a project (normal flow)
    Given a todo titled "<todo>" exists
    When I add the todo "<todo>" to the project "ECSE 429 project"
    Then the response status is 201
    And the project "ECSE 429 project" has the task "<todo>"
    And the todo "<todo>" belongs to the project "ECSE 429 project"

    Examples:
      | todo                 |
      | Write the user story |
      | Record the video     |

  Scenario Outline: Create a new task through the project (alternate flow)
    When I add a new task titled "<todo>" to the project "ECSE 429 project"
    Then the response status is 201
    And the project "ECSE 429 project" has the task "<todo>"
    And the todo "<todo>" belongs to the project "ECSE 429 project"

    Examples:
      | todo                 |
      | Write the user story |
      | Record the video     |

  Scenario Outline: Add a todo that does not exist (error flow)
    When I add the todo with id "<id>" to the project "ECSE 429 project"
    Then the response status is 404
    And the error message contains "Could not find thing matching value for id"
    And the project "ECSE 429 project" has no tasks

    Examples:
      | id  |
      | 999 |
      | 0   |
