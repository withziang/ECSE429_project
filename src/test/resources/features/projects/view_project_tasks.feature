Feature: View a project's tasks
  As a student, I want to see the tasks of a course project,
  so that I know what is left to finish before the deadline.

  Background:
    Given the Todo Manager is running
    And a project titled "ECSE 429 project" exists
    And a todo titled "Write the user story" exists
    And a todo titled "Record the video" exists
    And a todo titled "Buy groceries" exists
    And the todo "Write the user story" is a task of the project "ECSE 429 project"
    And the todo "Record the video" is a task of the project "ECSE 429 project"

  Scenario Outline: View the tasks of a project in JSON (normal flow)
    When I view the tasks of the project "ECSE 429 project" in JSON
    Then the response status is 200
    And the list includes the todo "<task>"
    And the list does not include the todo "<other>"

    Examples:
      | task                 | other         |
      | Write the user story | Buy groceries |
      | Record the video     | Buy groceries |

  Scenario Outline: View the tasks of a project in XML (alternate flow)
    When I view the tasks of the project "ECSE 429 project" in XML
    Then the response status is 200
    And the XML task list includes the todo "<task>"
    And the XML task list does not include the todo "<other>"

    Examples:
      | task                 | other         |
      | Write the user story | Buy groceries |
      | Record the video     | Buy groceries |

  Scenario Outline: View the tasks of a project that does not exist, actual behaviour of bug PROJ-002 (error flow)
    When I view the tasks of the project with id "<id>"
    Then the response status is 200
    And the list includes the todo "<task of another project>"

    Examples:
      | id  | task of another project |
      | 999 | Write the user story    |
      | 0   | Record the video        |
