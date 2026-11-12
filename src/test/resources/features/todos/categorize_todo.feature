Feature: Categorize a todo
  As a student, I want to put a todo in a category,
  so that I can group my coursework by course or by priority.

  Background:
    Given the Todo Manager is running

  Scenario Outline: Add a todo to an existing category (normal flow)
    Given a todo titled "<todo>" exists
    And a category titled "<category>" exists
    When I add the todo "<todo>" to the category "<category>"
    Then the response status is 201
    And the todo "<todo>" is in the category "<category>"

    Examples:
      | todo         | category |
      | Assignment 3 | ECSE 429 |
      | Lab 2        | Urgent   |

  Scenario Outline: Add a todo to a new category by its title (alternate flow)
    Given a todo titled "<todo>" exists
    When I add the todo "<todo>" to a new category titled "<category>"
    Then the response status is 201
    And the todo "<todo>" is in the category "<category>"

    Examples:
      | todo           | category |
      | Read chapter 4 | Reading  |
      | Study for quiz | Exams    |

  Scenario Outline: Add a todo to a category that does not exist (error flow)
    Given a todo titled "<todo>" exists
    When I add the todo "<todo>" to the category with id "<id>"
    Then the response status is 404
    And the error message contains "Could not find thing matching value for id"
    And the todo "<todo>" has no categories

    Examples:
      | todo         | id |
      | Assignment 3 | 0  |
      | Lab 2        | -1 |
