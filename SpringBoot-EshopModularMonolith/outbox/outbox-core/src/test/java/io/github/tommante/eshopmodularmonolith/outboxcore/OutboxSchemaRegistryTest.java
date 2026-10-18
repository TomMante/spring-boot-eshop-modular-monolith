package io.github.tommante.eshopmodularmonolith.outboxcore;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxSchemaRegistryTest {

    private static final OutboxPayloadUpcaster UNCHANGED = payload -> payload;

    private final OutboxSchemaRegistry registry = new OutboxSchemaRegistry();

    @Test
    @DisplayName("Given a newly registered event type, when asking its current version, then it is 1")
    void givenNewlyRegisteredEventType_whenAskingCurrentVersion_thenItIs1() {
        registry.registerEvent("X");

        assertThat(registry.currentVersionOf("X")).isEqualTo(1);
    }

    @Test
    @DisplayName("Given versions 2 and 3 registered, when asking the current version, then it is 3")
    void givenVersions2And3Registered_whenAskingCurrentVersion_thenItIs3() {
        registry.registerEvent("X");
        registry.registerVersion("X", 2, UNCHANGED);
        registry.registerVersion("X", 3, UNCHANGED);

        assertThat(registry.currentVersionOf("X")).isEqualTo(3);
    }

    @Test
    @DisplayName("Given an unregistered event type, when asking its current version, then it is rejected")
    void givenUnregisteredEventType_whenAskingCurrentVersion_thenItIsRejected() {
        assertThatThrownBy(() -> registry.currentVersionOf("X"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No schema version registered for event type X");
    }

    @Test
    @DisplayName("Given a registered event type, when registering it again, then it is rejected")
    void givenRegisteredEventType_whenRegisteringItAgain_thenItIsRejected() {
        registry.registerEvent("X");

        assertThatThrownBy(() -> registry.registerEvent("X"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Event type X is already registered");
    }

    @Test
    @DisplayName("Given an unregistered event type, when registering a version, then it is rejected")
    void givenUnregisteredEventType_whenRegisteringVersion_thenItIsRejected() {
        assertThatThrownBy(() -> registry.registerVersion("X", 2, UNCHANGED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No schema version registered for event type X");
    }

    @ParameterizedTest(name = "version {0}")
    @ValueSource(ints = {2, 4})
    @DisplayName("Given an event type at version 2, when registering a version other than 3, then it is rejected naming 3")
    void givenEventTypeAtVersion2_whenRegisteringVersionOtherThan3_thenItIsRejectedNaming3(int version) {
        registry.registerEvent("X");
        registry.registerVersion("X", 2, UNCHANGED);

        assertThatThrownBy(() -> registry.registerVersion("X", version, UNCHANGED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("next version to register is 3, but was " + version);
    }

    @Test
    @DisplayName("Given a registered event type, when registering a version without an upcaster, then it is rejected")
    void givenRegisteredEventType_whenRegisteringVersionWithoutUpcaster_thenItIsRejected() {
        registry.registerEvent("X");

        assertThatThrownBy(() -> registry.registerVersion("X", 2, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("needs the upcaster");
    }
}