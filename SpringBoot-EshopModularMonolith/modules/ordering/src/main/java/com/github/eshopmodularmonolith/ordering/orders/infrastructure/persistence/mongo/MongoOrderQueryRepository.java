package com.github.eshopmodularmonolith.ordering.orders.infrastructure.persistence.mongo;

import com.github.eshopmodularmonolith.ordering.orders.application.query.OrderQueryRepository;
import com.github.eshopmodularmonolith.ordering.orders.contract.OrderDTO;
import com.github.eshopmodularmonolith.ordering.orders.infrastructure.persistence.mongo.model.DBMapper;
import com.github.eshopmodularmonolith.ordering.orders.infrastructure.persistence.mongo.model.OrderDB;
import com.github.eshopmodularmonolith.shared.contract.identity.CurrentUser;
import com.github.eshopmodularmonolith.shared.domain.pagination.PageRequest;
import com.github.eshopmodularmonolith.shared.domain.pagination.PageResult;
import com.github.eshopmodularmonolith.shared.domain.pagination.SortOrder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class MongoOrderQueryRepository implements OrderQueryRepository {

    private MongoTemplate template;

    public MongoOrderQueryRepository(@Qualifier("orderMongoTemplate") MongoTemplate template) {
        this.template = template;
    }

    @Override
    public Optional<OrderDTO> findVisibleTo(CurrentUser viewer, UUID id) {
        var query = new Query(Criteria.where("_id").is(id).and("customer").is(viewer.username()));
        return Optional.ofNullable(template.findOne(query, OrderDB.class)).map(DBMapper::toDto);
    }

    @Override
    public PageResult<OrderDTO> ordersVisibleTo(CurrentUser viewer, PageRequest pageRequest) {
        var visibleToViewer = Criteria.where("customer").is(viewer.username());
        var query = new Query(visibleToViewer);
        query.skip(pageRequest.skip());
        query.limit(pageRequest.pageSize());
        query.with(Sort.by(pageRequest.sortOrders().stream().map(this::toSpringSortOrder).toList()));

        var orders = template.find(query, OrderDB.class, OrderDB.COLLECTION_NAME).stream().map(DBMapper::toDto).toList();
        var count = template.count(new Query(visibleToViewer), OrderDB.class);

        return PageResult.of(orders, pageRequest.pageIndex(), pageRequest.pageSize(), count);
    }

    private Sort.Order toSpringSortOrder(SortOrder sortOrder){
        return switch (sortOrder.direction()) {
            case ASC -> Sort.Order.asc(sortOrder.property());
            case DESC -> Sort.Order.desc(sortOrder.property());
        };
    }
}
