Feature: Assign a category to a project
  As a user
  I want to assign categories to projects
  So that I can organize related work

  Background:
    Given the Todo Manager is running

  Scenario Outline: Assign a category to a project (normal flow)
    Given a category titled "<category>" exists
    And a project titled "<project>" exists
    When I assign project "<project>" to category "<category>"
    Then the response status indicates success
    And category "<category>" contains project "<project>" exactly once

    Examples:
      | category   | project       |
      | University | ECSE 429      |
      | Personal   | Home Renovation |

  Scenario Outline: Assign the same category to a project twice (alternate flow)
    Given a category titled "<category>" exists
    And a project titled "<project>" exists
    When I assign project "<project>" to category "<category>" twice
    Then the response status indicates success
    And category "<category>" contains project "<project>" exactly once

    Examples:
      | category | project       |
      | Work     | Internship    |
      | School   | COMP 565      |

  Scenario Outline: Assign a category to a project that does not exist (error flow)
    Given a category titled "<category>" exists
    When I assign project with id "<projectId>" to category "<category>"
    Then the response status is 404

    Examples:
      | category | projectId |
      | Work     | 100       |
      | Work     | 101       |