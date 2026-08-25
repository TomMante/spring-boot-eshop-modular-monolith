# Testing Considerations and Known Bugs

This document tracks identified issues, testing gaps, and known bugs within the application, particularly focusing on business and behavior logic rather than simple line coverage.

## 🐛 Known Bugs

### 1. `ShoppingCart` Quantity Ignored in Checkout
- **Location:** `ShoppingCart.checkout(Map<UUID, BigDecimal> currentPrices)`
- **Issue:** The total price calculation completely ignores the `quantity` of the items in the basket. If a user adds 3 units of the same product, they are currently only charged for 1. Additionally, if the product ID is missing from `currentPrices`, `.get()` returns null, which throws a `NullPointerException` during the `BigDecimal::add` reduction.
- **Related, in the caller:** `CheckoutBasketHandler` builds `currentPrices` with `Collectors.toMap(ProductInfo::id, ProductInfo::price)`. `ShoppingCart.addItem` never merges duplicates, so adding the same product twice produces two `Item`s with the same `productId` and the collector throws `IllegalStateException` on the duplicate key — checkout fails outright for an ordinary user action.
- **Impact:** Critical business logic failure affecting order totals, and a hard failure on duplicate products.
- **Action Required:** Fix the calculation to multiply price by `quantity`, handle missing prices gracefully, and decide whether duplicate products merge into one item or the price map tolerates duplicate keys. Add isolated domain tests for `ShoppingCart` (see Gap 1). The pricing basis — add-time vs checkout-time price — must be settled first.

## 🧪 Testing Considerations & Gaps

### 1. Missing Domain Test for `ShoppingCart.checkout`
Domain classes here do not get isolated tests by default — they get one when they carry logic whose cases outgrow the seam above them, per the criteria in [TESTING_CONVENTIONS.md](TESTING_CONVENTIONS.md). Most of this codebase's domain classes (`Order`, `Product`, `Address`, `Payment`, `Item`) are constructors, getters, and factories that shuffle arguments; a test on them would assert that assignment works.
- **Issue:** `ShoppingCart.checkout` is the one method that does qualify — a calculation over a collection — and it has no isolated test. `CheckoutBasketHandlerTest` covers it with a single item at quantity 1, which is precisely the case where Known Bug 1 is invisible.
- **Improvement:** Create `ShoppingCartTest.java` covering multiple items, quantity greater than 1, a product missing from `currentPrices`, and the same product present as two separate items. Note that 100% JaCoCo coverage on these classes is a side effect of the Application layer tests and says nothing about whether these cases are exercised.

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