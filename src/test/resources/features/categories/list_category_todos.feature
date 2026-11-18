Feature: List todos belonging to a category
  As a user
  I want to list todos in a category
  So that I can see the work associated with it

  Background:
    Given the Todo Manager is running

  Scenario Outline: List todos belonging to a category (normal flow)
    Given a category titled "<category>" exists
    And a todo titled "<todo>" exists
    And todo "<todo>" is assigned to category "<category>"
    When I list todos for category "<category>"
    Then the response status is 200
    And category "<category>" contains todo "<todo>"

    Examples:
      | category   | todo              |
      | University | Finish assignment |
      | Personal   | Buy groceries     |

  Scenario Outline: List todos from an empty category (alternate flow)
    Given a category titled "<category>" exists
    When I list todos for category "<category>"
    Then the response status is 200
    And category "<category>" has no todos

    Examples:
      | category |
      | Shopping |
      | Hobbies  |

  Scenario Outline: List todos for a category that does not exist (error flow)
    Given a category titled "<existingCategory>" exists
    And a todo titled "<todo>" exists
    And todo "<todo>" is assigned to category "<existingCategory>"
    When I list todos for category with id "<missingId>"
    Then the response status is 200
    And the response contains no todos

  Examples:
    | existingCategory | todo              | missingId |
    | University       | Finish assignment | 100       |
    | Personal         | Buy groceries     | 101       |