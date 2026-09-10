package com.github.eshopmodularmonolith.ordering.orders.application.features.getorders;

import com.github.eshopmodularmonolith.ordering.orders.contract.DTOMapper;
import com.github.eshopmodularmonolith.ordering.orders.domain.OrderMother;
import com.github.eshopmodularmonolith.ordering.orders.infrastructure.persistence.fake.FakeOrderQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GetOrdersHandlerTest {


    private FakeOrderQueryRepository repository;
    private GetOrdersHandler handler;

    @BeforeEach
    void setUp() {
        repository = new FakeOrderQueryRepository();
        handler = new GetOrdersHandler(repository);
    }

    @Test
    @DisplayName("Given get orders query, when handler is called, then get orders result is return")
    void givenGetOrdersQuery_whenHandlerIsCalled_thenGetOrdersResultIsReturned() {
        var oldest = DTOMapper.toDto(OrderMother.order(Instant.parse("2023-01-01T00:00:00Z")));
        var middle = DTOMapper.toDto(OrderMother.order(Instant.parse("2024-01-01T00:00:00Z")));
        var newest = DTOMapper.toDto(OrderMother.order(Instant.parse("2025-01-01T00:00:00Z")));

        repository.addOrder(middle);
        repository.addOrder(newest);
        repository.addOrder(oldest);

        var page1 = handler.handle(new GetOrdersQuery(0, 2));
        assertThat(page1.orders()).containsExactlyElementsOf(List.of(newest, middle));
        assertThat(page1.pageIndex()).isZero();
        assertThat(page1.pageSize()).isEqualTo(2);
        assertThat(page1.totalItems()).isEqualTo(3);
        assertThat(page1.totalPages()).isEqualTo(2);

        var page2 = handler.handle(new GetOrdersQuery(1, 2));
        assertThat(page2.orders()).containsExactlyElementsOf(List.of(oldest));
        assertThat(page2.pageIndex()).isOne();
        assertThat(page2.pageSize()).isEqualTo(2);
        assertThat(page2.totalItems()).isEqualTo(3);
        assertThat(page2.totalPages()).isEqualTo(2);

    }

    @Test
    @DisplayName("Given orders sharing a placedAt instant, when consecutive pages are requested, then each order appears on exactly one page")
    void givenOrdersSharingPlacedAtInstant_whenConsecutivePagesAreRequested_thenEachOrderAppearsOnExactlyOnePage() {
        // Leading bytes straddle 0x80, where unsigned byte order and UUID.compareTo disagree. The same three
        // ids are asserted against real Mongo in MongoOrderQueryRepositoryTest.
        var sameInstant = Instant.parse("2025-01-01T00:00:00Z");
        var firstById = DTOMapper.toDto(OrderMother.order(UUID.fromString("7f000000-0000-4000-8000-000000000001"), sameInstant));
        var secondById = DTOMapper.toDto(OrderMother.order(UUID.fromString("80000000-0000-4000-8000-000000000002"), sameInstant));
        var thirdById = DTOMapper.toDto(OrderMother.order(UUID.fromString("ff000000-0000-4000-8000-000000000003"), sameInstant));

        repository.addOrder(thirdById);
        repository.addOrder(firstById);
        repository.addOrder(secondById);

        var page1 = handler.handle(new GetOrdersQuery(0, 2));
        assertThat(page1.orders()).containsExactly(firstById, secondById);

        var page2 = handler.handle(new GetOrdersQuery(1, 2));
        assertThat(page2.orders()).containsExactly(thirdById);
    }
}
