Feature: Delete a todo
  As a student, I want to delete a todo I no longer need,
  so that my list only shows the work that still matters.

  Background:
    Given the Todo Manager is running

  Scenario Outline: Delete a todo (normal flow)
    Given a todo titled "<title>" exists
    When I delete the todo "<title>"
    Then the response status is 200
    And the todo "<title>" no longer exists

    Examples:
      | title        |
      | Assignment 3 |
      | Lab 2        |

  Scenario Outline: Delete a todo that is a task of a project (alternate flow)
    Given a todo titled "<todo>" exists
    And a project titled "<project>" exists
    And the todo "<todo>" is a task of the project "<project>"
    When I delete the todo "<todo>"
    Then the response status is 200
    And the todo "<todo>" no longer exists
    And the project "<project>" has no tasks

    Examples:
      | todo           | project           |
      | Read chapter 4 | ECSE 429 project  |
      | Study for quiz | Midterm revision  |

  Scenario Outline: Delete a todo that was already deleted (error flow)
    Given a todo titled "<title>" exists
    And the todo "<title>" has been deleted
    When I delete the todo "<title>"
    Then the response status is 404
    And the error message contains "Could not find any instances with todos/"

    Examples:
      | title        |
      | Assignment 3 |
      | Lab 2        |
