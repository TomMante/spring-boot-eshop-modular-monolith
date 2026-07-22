# Testing Considerations and Known Bugs

This document tracks identified issues, testing gaps, and known bugs within the application, particularly focusing on business and behavior logic rather than simple line coverage.

## 🐛 Known Bugs

### 1. `ShoppingCart` Quantity Ignored in Checkout
- **Location:** `ShoppingCart.checkout(Map<UUID, BigDecimal> currentPrices)`
- **Issue:** The total price calculation completely ignores the `quantity` of the items in the basket. If a user adds 3 units of the same product, they are currently only charged for 1. Additionally, if the product ID is missing from `currentPrices`, `.get()` returns null, which throws a `NullPointerException` during the `BigDecimal::add` reduction.
- **Impact:** Critical business logic failure affecting order totals.
- **Action Required:** Fix the calculation logic to multiply the price by `quantity` and handle missing prices gracefully. Add isolated domain aggregate tests for `ShoppingCart`.

## 🧪 Testing Considerations & Gaps

### 1. Missing Isolated Tests for Domain Aggregates
While classes like `ShoppingCart` and `Product` may show 100% JaCoCo coverage, that coverage is purely a side effect of running the Application layer tests (e.g., `CheckoutBasketHandlerTest`, `UpdateProductHandlerTest`). 
- **Improvement:** In Domain-Driven Design (DDD), core aggregates should have their own isolated unit tests to ensure business invariants are met regardless of the application layer. Create `ShoppingCartTest.java` and `ProductTest.java` to test domain rules purely in isolation.

### 2. Missing Failure Path in `UpdateProductHandler`
The `UpdateProductHandler` handles the "happy path" (product exists and is updated) but the failure path is untested.
- **Location:** `UpdateProductHandler.java`
- **Issue:** The `ProductNotFound` exception thrown inside `.ifPresentOrElse(...)` is never reached in tests.
- **Improvement:** Add a test scenario in `UpdateProductHandlerTest` ensuring that attempting to update a non-existent product properly aborts and triggers a `ProductNotFound` exception.

### 3. Untested Application Domain Event Listeners
- **Locations:** `BasketCheckedOutHandler`, `ProductCreatedHandler`
- **Issue:** These classes currently report 0 lines covered. Although they might only be logging right now, missing tests mean there is no guarantee that a `ProductCreated` or `BasketCheckedOut` domain event is actually being picked up and routed by the Spring context.
- **Improvement:** Add integration/unit tests verifying that the `ApplicationEventPublisher` successfully triggers these event handlers.

### 4. Empty / Unimplemented Error Handler
- **Location:** `CreateOrderErrorHandler`
- **Issue:** The class is completely empty and misses test coverage.
- **Improvement:** Implement the error handler to provide a proper Contract DTO error response (instead of an unhandled 500 Server Error) if order creation fails, and add corresponding tests.