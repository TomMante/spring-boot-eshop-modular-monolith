package io.github.tommante.eshopmodularmonolith.shared.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AggregateRoot<I> implements Aggregate, Entity<I> {

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    protected void addDomainEvent(DomainEvent event){
        domainEvents.add(event);
    }

    public List<DomainEvent> domainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public List<DomainEvent> clearDomainEvents(){
        var events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return events;
    }
}
