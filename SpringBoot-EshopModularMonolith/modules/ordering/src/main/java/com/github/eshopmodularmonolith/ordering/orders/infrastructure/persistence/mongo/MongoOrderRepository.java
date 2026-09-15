package com.github.eshopmodularmonolith.ordering.orders.infrastructure.persistence.mongo;

import com.github.eshopmodularmonolith.ordering.orders.domain.Order;
import com.github.eshopmodularmonolith.ordering.orders.domain.exceptions.OrderAlreadyExists;
import com.github.eshopmodularmonolith.ordering.orders.domain.port.OrderRepository;
import com.github.eshopmodularmonolith.ordering.orders.infrastructure.persistence.mongo.model.DBMapper;
import com.github.eshopmodularmonolith.ordering.orders.infrastructure.persistence.mongo.model.OrderDB;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexResolver;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class MongoOrderRepository implements OrderRepository {

    private final SpringMongoOrderRepository springRepository;
    private final MongoTemplate mongoTemplate;

    public MongoOrderRepository(@Qualifier("orderMongoTemplate") MongoTemplate mongoTemplate, SpringMongoOrderRepository springRepository) {
        this.springRepository = springRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    void createIndexes(){
        var indexOps = mongoTemplate.indexOps(OrderDB.class);
        IndexResolver.create(mongoTemplate.getConverter().getMappingContext())
                .resolveIndexFor(OrderDB.class)
                .forEach(indexOps::createIndex);
    }

    @Override
    public Optional<Order> findById(UUID orderId) {
        return springRepository.findById(orderId).map(DBMapper::toDomain);
    }

    @Override
    public void save(Order order) {
        try {
            springRepository.save(DBMapper.toDb(order));
        }catch (DuplicateKeyException duplicate){
            throw new OrderAlreadyExists(order.checkoutId(), duplicate);
        }
    }

    @Override
    public void delete(UUID orderId) {
        springRepository.deleteById(orderId);
    }
}
