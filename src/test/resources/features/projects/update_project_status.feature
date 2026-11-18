Feature: Update a project's status
  As a student, I want to mark a course project as completed or active,
  so that I can tell which projects I am still working on.

  Background:
    Given the Todo Manager is running

  Scenario Outline: Mark a project as completed (normal flow)
    Given a project titled "<project>" with description "<description>" exists
    When I mark the project "<project>" as completed
    Then the response status is 200
    And the project "<project>" has completed "true" and active "false"
    And the project "<project>" has description "<description>"

    Examples:
      | project          | description            |
      | ECSE 429 project | Story tests for Part B |
      | COMP 251 essay   | Graph algorithms       |

  Scenario Outline: Set both status flags at once (alternate flow)
    Given a project titled "<project>" exists
    When I set the project "<project>" to completed "<completed>" and active "<active>"
    Then the response status is 200
    And the project "<project>" has completed "<completed>" and active "<active>"

    Examples:
      | project          | completed | active |
      | ECSE 429 project | true      | true   |
      | COMP 251 essay   | false     | true   |

  Scenario Outline: Mark a project that does not exist as completed (error flow)
    When I mark the project with id "<id>" as completed
    Then the response status is 404
    And the error message contains "No such project entity instance with GUID or ID <id> found"

    Examples:
      | id  |
      | 999 |
      | 0   |
