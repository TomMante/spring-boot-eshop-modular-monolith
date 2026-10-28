Feature: Catalog
  The catalog is seeded with three demo products when the application starts.

  Scenario: The seeded products are listed
    Given Alice is a customer
    When Alice lists the products
    Then Alice sees the products "Galaxy Smartphone", "Aurora Laptop" and "Pulse Headphones"
