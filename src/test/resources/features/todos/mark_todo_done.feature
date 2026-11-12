Feature: Mark a todo done
  As a student, I want to mark a todo as done,
  so that I can see which coursework I have already finished.

  Background:
    Given the Todo Manager is running

  Scenario Outline: Mark an unfinished todo as done (normal flow)
    Given a todo titled "<title>" with description "<description>" exists
    When I mark the todo "<title>" as done
    Then the response status is 200
    And the todo "<title>" is done
    And the todo "<title>" has description "<description>"

    Examples:
      | title        | description      |
      | Assignment 3 | Exercises 1 to 5 |
      | Lab 2        | Circuit analysis |

  Scenario Outline: Mark a todo that is already done (alternate flow)
    Given a todo titled "<title>" exists
    And the todo "<title>" is marked as done
    When I mark the todo "<title>" as done
    Then the response status is 200
    And the todo "<title>" is done

    Examples:
      | title          |
      | Read chapter 4 |
      | Study for quiz |

  Scenario Outline: Mark a todo that does not exist as done (error flow)
    When I mark the todo with id "<id>" as done
    Then the response status is 404
    And the error message contains "No such todo entity instance with GUID or ID <id> found"

    Examples:
      | id |
      | 0  |
      | -1 |
