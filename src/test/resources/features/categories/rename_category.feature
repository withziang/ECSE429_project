Feature: Rename a category
  As a user
  I want to rename categories
  So that I can keep their names accurate

  Background:
    Given the Todo Manager is running

  Scenario Outline: Rename a category using POST (normal flow)
    Given a category titled "<oldTitle>" exists
    When I rename category "<oldTitle>" to "<newTitle>" using "POST"
    Then the response status is 200
    And the category "<newTitle>" exists
    And no category exists with title "<oldTitle>"

    Examples:
      | oldTitle  | newTitle       |
      | University | School        |
      | Personal   | Private       |

  Scenario Outline: Rename a category using PUT (alternate flow)
    Given a category titled "<oldTitle>" exists
    When I rename category "<oldTitle>" to "<newTitle>" using "PUT"
    Then the response status is 200
    And the category "<newTitle>" exists

    Examples:
      | oldTitle | newTitle  |
      | Shopping | Groceries |
      | Work     | Career    |

  Scenario Outline: Rename a category that does not exist (error flow)
    When I rename category with id "<id>" to "<title>" using "POST"
    Then the response status is 404

    Examples:
      | id  | title        |
      | 100 | Missing One  |
      | 101 | Missing Two  |