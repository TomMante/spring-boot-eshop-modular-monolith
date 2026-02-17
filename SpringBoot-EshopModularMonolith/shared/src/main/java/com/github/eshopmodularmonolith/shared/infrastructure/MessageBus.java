package com.github.eshopmodularmonolith.shared.infrastructure;

public interface MessageBus {

    void publish(Object message);
}
