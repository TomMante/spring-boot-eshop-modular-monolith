package io.github.tommante.eshopmodularmonolith.shared.infrastructure;

public interface MessageBus {

    void publish(Object message);
}
