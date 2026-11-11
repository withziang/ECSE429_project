Feature: Create a todo
  As a student, I want to create a todo for a piece of coursework,
  so that I keep track of the work I still have to do.

  Background:
    Given the Todo Manager is running

  Scenario Outline: Create a todo with a title and a description (normal flow)
    When I create a todo with title "<title>" and description "<description>"
    Then the response status is 201
    And the todo "<title>" has description "<description>"
    And the todo "<title>" is not done

    Examples:
      | title          | description               |
      | Assignment 3   | Exercises 1 to 5          |
      | Read chapter 4 | Exploratory testing notes |

  Scenario Outline: Create a todo with only a title (alternate flow)
    When I create a todo with title "<title>"
    Then the response status is 201
    And the todo "<title>" has description ""
    And the todo "<title>" is not done

    Examples:
      | title          |
      | Lab 2          |
      | Study for quiz |

  Scenario Outline: Create a todo with an empty title (error flow)
    When I create a todo with title <title> and description "<description>"
    Then the response status is 400
    And the error message contains "title : can not be empty"
    And no todo has the description "<description>"

    Examples:
      | title | description   |
      | ""    | Lab report    |
      | "   " | Project notes |
