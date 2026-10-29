Feature: Requests need a token
  Every endpoint except health and the API docs needs a bearer token issued by the eshop realm.

  Scenario: A request without a token is refused
    When Visitor lists the products
    Then Visitor's request is refused as unauthenticated

  Scenario: A token from another realm is refused
    Given Mallory holds a token from another realm
    When Mallory lists the products
    Then Mallory's request is refused as unauthenticated

  Scenario: A token from the eshop realm is accepted
    Given Alice is a customer
    When Alice lists the products
    Then Alice's request is accepted
