Feature: Orders belong to their customer
  A customer sees only their own orders, in the list and by id.

  Background:
    Given Alice is a customer
    And Alice has placed an order for 1 "Aurora Laptop"

  Scenario: The customer finds the order by its id
    When Alice asks for Alice's order by its id
    Then Alice receives that order

  Scenario: Another customer can't see the order
    Given Bob is a customer
    When Bob asks for Alice's order by its id
    Then Bob's request is refused as not found
    And Bob's order list is empty
