Feature: Delete a category
  As a user
  I want to delete categories
  So that I can remove categories I no longer need

  Background:
    Given the Todo Manager is running

  Scenario Outline: Delete a category (normal flow)
    Given a category titled "<category>" exists
    When I delete category "<category>"
    Then the response status indicates success
    And no category exists with title "<category>"

    Examples:
      | category   |
      | University |
      | Personal   |

  Scenario Outline: Delete a category that has linked todos (alternate flow)
    Given a category titled "<category>" exists
    And a todo titled "<todo>" exists
    And todo "<todo>" is assigned to category "<category>"
    When I delete category "<category>"
    Then the response status indicates success
    And no category exists with title "<category>"

    Examples:
      | category | todo             |
      | Work     | Prepare report   |
      | School   | Study for exam   |

  Scenario Outline: Delete a category twice (error flow)
    Given a category titled "<category>" exists
    When I delete category "<category>" twice
    Then the response status is 404

    Examples:
      | category |
      | Shopping |
      | Hobbies  |