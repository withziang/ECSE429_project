Feature: Create a course project
  As a student, I want to create a project for each course assignment,
  so that I can group the work that belongs to it in one place.

  Background:
    Given the Todo Manager is running

  Scenario Outline: Create a project with all fields (normal flow)
    When I create a project titled "<title>" with description "<description>", completed "<completed>" and active "<active>"
    Then the response status is 201
    And the project "<title>" has description "<description>"
    And the project "<title>" has completed "<completed>" and active "<active>"

    Examples:
      | title            | description            | completed | active |
      | ECSE 429 project | Story tests for Part B | false     | true   |
      | COMP 251 essay   | Graph algorithms       | true      | false  |

  Scenario Outline: Create a project from an XML or an empty body (alternate flow)
    When I create a project from the <format> body "<body>"
    Then the response status is 201
    And the created project has title "<expected title>", completed "false" and active "false"

    Examples:
      | format | body                                           | expected title |
      | XML    | <project><title>MATH 240 lab</title></project> | MATH 240 lab   |
      | JSON   | {}                                             |                |

  Scenario Outline: Create a project with a flag that is not a boolean (error flow)
    When I create a project titled "<title>" with <flag> set to "<value>"
    Then the response status is 400
    And the error message contains "Failed Validation: <flag> should be BOOLEAN"
    And no project is titled "<title>"

    Examples:
      | title            | flag      | value |
      | ECSE 429 project | completed | yes   |
      | COMP 251 essay   | active    | maybe |
      | MATH 240 lab     | completed | true  |
