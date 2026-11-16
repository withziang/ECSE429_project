Feature: Create a category
  As a user, I want to create categories
  So that I can organize my todos and projects.

  Background:
    Given the Todo Manager is running

  Scenario Outline: Create a category with a title and description (normal flow)
    When I create a category with title "<title>" and description "<description>"
    Then the response status is 201
    And the category "<title>" exists

    Examples:
      | title      | description       |
      | University | University tasks  |
      | Personal   | Personal tasks    |

  Scenario Outline: Create a category with only a title (alternate flow)
    When I create a category with title "<title>"
    Then the response status is 201
    And the category "<title>" exists

    Examples:
      | title    |
      | Shopping |
      | Work     |

  Scenario Outline: Create a category with an invalid title (error flow)
    When I create a category with invalid title "<title>"
    Then the response status is 400
    And the error message contains "title"
    And no category exists with invalid title "<title>"

    Examples:
      | title      |
      | EMPTY      |
      | WHITESPACE |