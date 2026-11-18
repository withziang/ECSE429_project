Feature: Remove work from a project
  As a student, I want to remove a task from a course project,
  so that the project only lists the work it still needs.

  Background:
    Given the Todo Manager is running
    And a project titled "ECSE 429 project" exists

  Scenario Outline: Remove a task from a project (normal flow)
    Given a todo titled "<task>" exists
    And the todo "<task>" is a task of the project "ECSE 429 project"
    When I remove the task "<task>" from the project "ECSE 429 project"
    Then the response status is 200
    And the project "ECSE 429 project" has no tasks
    And the todo "<task>" still exists

    Examples:
      | task                 |
      | Write the user story |
      | Record the video     |

  Scenario Outline: Remove one of several tasks from a project (alternate flow)
    Given a todo titled "<task>" exists
    And a todo titled "<other>" exists
    And the todo "<task>" is a task of the project "ECSE 429 project"
    And the todo "<other>" is a task of the project "ECSE 429 project"
    When I remove the task "<task>" from the project "ECSE 429 project"
    Then the response status is 200
    And the project "ECSE 429 project" does not have the task "<task>"
    And the project "ECSE 429 project" has the task "<other>"
    And the todo "<task>" still exists

    Examples:
      | task                 | other            |
      | Write the user story | Record the video |
      | Record the video     | Write the report |

  Scenario Outline: Remove a task that was already removed (error flow)
    Given a todo titled "<task>" exists
    And the todo "<task>" is a task of the project "ECSE 429 project"
    And the task "<task>" has been removed from the project "ECSE 429 project"
    When I remove the task "<task>" from the project "ECSE 429 project"
    Then the response status is 404
    And the error message contains "Could not find any instances with projects/"
    And the todo "<task>" still exists

    Examples:
      | task                 |
      | Write the user story |
      | Record the video     |
