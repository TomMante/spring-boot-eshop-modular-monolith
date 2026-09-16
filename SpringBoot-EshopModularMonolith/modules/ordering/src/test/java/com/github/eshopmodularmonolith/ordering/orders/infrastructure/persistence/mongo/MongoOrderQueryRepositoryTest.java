package com.github.eshopmodularmonolith.ordering.orders.infrastructure.persistence.mongo;

import com.github.eshopmodularmonolith.ordering.orders.application.query.OrderQueryRepository;
import com.github.eshopmodularmonolith.ordering.orders.contract.DTOMapper;
import com.github.eshopmodularmonolith.ordering.orders.domain.Order;
import com.github.eshopmodularmonolith.ordering.orders.domain.OrderMother;
import com.github.eshopmodularmonolith.ordering.orders.domain.port.OrderRepository;
import com.github.eshopmodularmonolith.ordering.orders.infrastructure.persistence.mongo.model.OrderDB;
import com.github.eshopmodularmonolith.shared.contract.identity.CurrentUser;
import com.github.eshopmodularmonolith.shared.domain.pagination.PageRequest;
import com.github.eshopmodularmonolith.shared.domain.pagination.SortOrder;
import com.mongodb.client.MongoClient;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.test.context.ContextConfiguration;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static com.github.eshopmodularmonolith.shared.domain.pagination.SortOrder.Direction.ASC;
import static com.github.eshopmodularmonolith.shared.domain.pagination.SortOrder.Direction.DESC;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ContextConfiguration(classes = MongoOrderQueryRepositoryTest.Config.class)
@Import(value = {MongoOrderRepository.class, MongoOrderQueryRepository.class})
class MongoOrderQueryRepositoryTest extends OrderingDatabaseTest {

    @Configuration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {OrderDB.class})
    @EnableMongoRepositories(basePackageClasses = {MongoOrderRepository.class, MongoOrderQueryRepository.class},
            mongoTemplateRef = "orderMongoTemplate")
    static class Config {

        @Bean("orderMongoTemplate")
        public MongoTemplate mongoTemplate(MongoClient client) {
            return new MongoTemplate(client, "eshop-orders-db");
        }
    }

    @Autowired
    private OrderQueryRepository queryRepository;
    @Autowired
    private OrderRepository writeRepository;



    @Test
    @DisplayName("Given existing orders, when querying by id, then return correct order")
    void givenExistingOrders_whenQueryingById_thenReturnCorrectOrder() {
        var order1 = OrderMother.order();
        var order2 = OrderMother.order();
        writeRepository.save(order1);
        writeRepository.save(order2);
        assertThat(queryRepository.findVisibleTo(new CurrentUser("john_doe"), order1.id())).isPresent()
                .get().usingRecursiveComparison()
                .isEqualTo(DTOMapper.toDto(order1));
        assertThat(queryRepository.findVisibleTo(new CurrentUser("john_doe"), order2.id())).isPresent()
                .get().usingRecursiveComparison()
                .isEqualTo(DTOMapper.toDto(order2));
    }

    @Test
    @DisplayName("Given an order placed by another customer, when querying by id, then returns empty")
    void givenAnOrderPlacedByAnotherCustomer_whenQueryingById_thenReturnsEmpty() {
        var order1 = OrderMother.instance().withCustomer("jane_roe").build();
        writeRepository.save(order1);
        assertThat(queryRepository.findVisibleTo(new CurrentUser("john_doe"), order1.id())).isEmpty();
        assertThat(queryRepository.findVisibleTo(new CurrentUser("jane_roe"), order1.id())).isPresent();
    }

    @Nested
    @DisplayName("Paginated orders")
    class PaginatedOrders {

        private static final int PAGE_SIZE = 2;
        private static final int EXPECTED_TOTAL_PAGES = 2;

        private List<Order> seededOrders;

        @BeforeEach
        void seedOrdersOldestFirst() {
            seededOrders = List.of(
                    OrderMother.order(Instant.parse("2024-01-01T00:00:00Z")),
                    OrderMother.order(Instant.parse("2025-01-01T00:00:00Z")),
                    OrderMother.order(Instant.parse("2026-01-01T00:00:00Z")));
            seededOrders.forEach(writeRepository::save);
        }

        static Stream<Arguments> pageRequests() {
            return Stream.of(
                    arguments(0, ASC, List.of(0, 1)),
                    arguments(1, ASC, List.of(2)),
                    arguments(0, DESC, List.of(2, 1)),
                    arguments(1, DESC, List.of(0)),
                    arguments(2, ASC, List.of()));
        }

        @ParameterizedTest(name = "page {0} sorted {1} returns seeded orders {2}")
        @MethodSource("pageRequests")
        @DisplayName("Given database with orders, when a page is requested, then that page is returned in sort order")
        void givenDatabaseWithOrders_whenPageIsRequested_thenPageIsReturnedInSortOrder(
                int pageIndex, SortOrder.Direction direction, List<Integer> expectedSeededOrderIndexes) {

            var page = queryRepository.ordersVisibleTo(
                    new CurrentUser("john_doe"),
                    new PageRequest(pageIndex, PAGE_SIZE, List.of(new SortOrder("placedAt", direction))));

            var expectedOrders = expectedSeededOrderIndexes.stream()
                    .map(seededOrders::get)
                    .map(DTOMapper::toDto)
                    .toList();

            Assertions.assertThat(page.items())
                    .usingRecursiveFieldByFieldElementComparator()
                    .containsExactlyElementsOf(expectedOrders);
            Assertions.assertThat(page.pageIndex()).isEqualTo(pageIndex);
            Assertions.assertThat(page.pageSize()).isEqualTo(PAGE_SIZE);
            Assertions.assertThat(page.totalItems()).isEqualTo(seededOrders.size());
            Assertions.assertThat(page.totalPages()).isEqualTo(EXPECTED_TOTAL_PAGES);
        }
    }

    @Nested
    @DisplayName("Paginated orders sharing a placedAt instant")
    class PaginatedOrdersSharingAPlacedAtInstant {

        private static final Instant SAME_INSTANT = Instant.parse("2025-01-01T00:00:00Z");
        private static final int PAGE_SIZE = 2;

        // Leading bytes chosen to straddle 0x80, where unsigned byte order and UUID.compareTo's signed
        // comparison disagree: signed, this list would sort 0x80, 0xff, 0x7f. GetOrdersHandlerTest asserts
        // the same three ids against the in-memory fake, so the two seams are pinned to one order.
        private static final List<UUID> IDS_IN_ASCENDING_ORDER = List.of(
                UUID.fromString("7f000000-0000-4000-8000-000000000001"),
                UUID.fromString("80000000-0000-4000-8000-000000000002"),
                UUID.fromString("ff000000-0000-4000-8000-000000000003"));

        private List<Order> ordersInExpectedOrder;

        @BeforeEach
        void seedOrdersOutOfIdOrder() {
            ordersInExpectedOrder = IDS_IN_ASCENDING_ORDER.stream()
                    .map(id -> OrderMother.order(id, SAME_INSTANT))
                    .toList();
            Stream.of(2, 0, 1).map(ordersInExpectedOrder::get).forEach(writeRepository::save);
        }

        @Test
        @DisplayName("Given orders sharing a placedAt instant, when consecutive pages are requested, then each order appears on exactly one page")
        void givenOrdersSharingPlacedAtInstant_whenConsecutivePagesAreRequested_thenEachOrderAppearsOnExactlyOnePage() {
            assertThatPageContains(0, 0, 1);
            assertThatPageContains(1, 2);
        }

        private void assertThatPageContains(int pageIndex, int... expectedOrderIndexes) {
            var page = queryRepository.ordersVisibleTo(new CurrentUser("john_doe"), new PageRequest(pageIndex, PAGE_SIZE,
                    List.of(new SortOrder("placedAt", DESC), new SortOrder("id", ASC))));

            var expectedOrders = Arrays.stream(expectedOrderIndexes)
                    .mapToObj(ordersInExpectedOrder::get)
                    .map(DTOMapper::toDto)
                    .toList();

            Assertions.assertThat(page.items())
                    .usingRecursiveFieldByFieldElementComparator()
                    .containsExactlyElementsOf(expectedOrders);
        }
    }

    @Nested
    @DisplayName("Paginated orders visible for customer")
    class PaginatedOrdersVisibleForCustomer {

        @Test
        @DisplayName("Given orders with different customers, when querying orders visible to customer, then return only that customer's orders")
        void givenOrdersWithDifferentCustomers_whenQueryingOrdersVisibleToCustomer_thenReturnOnlyThatCustomerOrders() {
            var johnOldest = OrderMother.instance().withPlacedAt(Instant.parse("2021-01-01T00:00:00Z")).withCustomer("john_doe").build();
            var johnsMiddle = OrderMother.instance().withPlacedAt(Instant.parse("2023-01-01T00:00:00Z")).withCustomer("john_doe").build();
            var johnsNewest = OrderMother.instance().withPlacedAt(Instant.parse("2024-01-01T00:00:00Z")).withCustomer("john_doe").build();
            var seededOrders = List.of(
                    johnOldest,
                    johnsNewest,
                    johnsMiddle,
                    OrderMother.instance().withPlacedAt(Instant.parse("2022-01-01T00:00:00Z")).withCustomer("jane_roe").build(),
                    OrderMother.instance().withPlacedAt(Instant.parse("2025-01-01T00:00:00Z")).withCustomer("jane_roe").build()
            );
            seededOrders.forEach(writeRepository::save);
            var firstPage = queryRepository.ordersVisibleTo(
                    new CurrentUser("john_doe"),
                    new PageRequest(0, 2, List.of(new SortOrder("placedAt", DESC))));

            Assertions.assertThat(firstPage.items())
                    .usingRecursiveFieldByFieldElementComparator()
                    .containsExactly(DTOMapper.toDto(johnsNewest), DTOMapper.toDto(johnsMiddle));
            Assertions.assertThat(firstPage.totalItems()).isEqualTo(3);
            Assertions.assertThat(firstPage.totalPages()).isEqualTo(2);

            var secondPage = queryRepository.ordersVisibleTo(
                    new CurrentUser("john_doe"),
                    new PageRequest(1, 2, List.of(new SortOrder("placedAt", DESC))));

            Assertions.assertThat(secondPage.items())
                    .usingRecursiveFieldByFieldElementComparator()
                    .containsExactly(DTOMapper.toDto(johnOldest));
        }


    }

}
