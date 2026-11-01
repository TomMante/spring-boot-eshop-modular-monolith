Feature: Checkout places an order
  A paid checkout becomes an order once the outbox has published it. A declined one changes nothing.

  Scenario: A paid checkout becomes an order
    Given Alice is a customer
    And Alice has 2 "Galaxy Smartphone" in the basket
    When Alice checks out paying with "pm_card_visa"
    Then Alice's checkout is accepted
    And Alice eventually has exactly one order: 2 "Galaxy Smartphone", 1100.00 in total, paid with a visa card ending in 4242
    And Alice no longer has a basket

  Scenario: A declined checkout keeps the basket and places no order
    Given Alice is a customer
    And Alice has 1 "Galaxy Smartphone" in the basket
    When Alice checks out paying with "pm_card_visa_chargeDeclined"
    Then Alice's payment is declined
    And Alice still has 1 "Galaxy Smartphone" in the basket
    When Alice checks out paying with "pm_card_visa"
    Then Alice eventually has exactly one order: 1 "Galaxy Smartphone", 550.00 in total, paid with a visa card ending in 4242
