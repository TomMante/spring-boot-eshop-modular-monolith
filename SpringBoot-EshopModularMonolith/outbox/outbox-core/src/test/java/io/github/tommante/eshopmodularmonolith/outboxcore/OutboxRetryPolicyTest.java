package io.github.tommante.eshopmodularmonolith.outboxcore;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxRetryPolicyTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final OutboxRetryPolicy policy =
            new OutboxRetryPolicy(8, Duration.ofSeconds(5), Duration.ofMinutes(5));

    @Test
    @DisplayName("Given the first failure, when computing the next attempt, then it is the initial delay away")
    void givenFirstFailure_whenComputingNextAttempt_thenItIsTheInitialDelayAway() {
        assertThat(policy.nextAttemptAfter(1, NOW)).isEqualTo(NOW.plusSeconds(5));
    }

    @Test
    @DisplayName("Given the third failure, when computing the next attempt, then the delay has doubled twice")
    void givenThirdFailure_whenComputingNextAttempt_thenTheDelayHasDoubledTwice() {
        assertThat(policy.nextAttemptAfter(3, NOW)).isEqualTo(NOW.plusSeconds(20));
    }

    @Test
    @DisplayName("Given a doubled delay above the cap, when computing the next attempt, then it is capped at the max delay")
    void givenDoubledDelayAboveCap_whenComputingNextAttempt_thenItIsCappedAtMaxDelay() {
        assertThat(policy.nextAttemptAfter(7, NOW)).isEqualTo(NOW.plus(Duration.ofMinutes(5)));
    }

    @Test
    @DisplayName("Given a failure count far beyond the cap, when computing the next attempt, then it stays at the max delay")
    void givenFailureCountFarBeyondCap_whenComputingNextAttempt_thenItStaysAtMaxDelay() {
        assertThat(policy.nextAttemptAfter(64, NOW)).isEqualTo(NOW.plus(Duration.ofMinutes(5)));
    }

    @Test
    @DisplayName("Given one failure fewer than max attempts, when checking exhaustion, then it is not exhausted")
    void givenOneFailureFewerThanMaxAttempts_whenCheckingExhaustion_thenItIsNotExhausted() {
        assertThat(policy.isExhausted(7)).isFalse();
    }

    @Test
    @DisplayName("Given failures equal to max attempts, when checking exhaustion, then it is exhausted")
    void givenFailuresEqualToMaxAttempts_whenCheckingExhaustion_thenItIsExhausted() {
        assertThat(policy.isExhausted(8)).isTrue();
    }

    @Test
    @DisplayName("Given an initial delay longer than the max delay, when creating the policy, then it is rejected naming the property")
    void givenInitialDelayLongerThanMaxDelay_whenCreatingPolicy_thenItIsRejectedNamingTheProperty() {
        assertThatThrownBy(() -> new OutboxRetryPolicy(8, Duration.ofMinutes(10), Duration.ofMinutes(5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("eshop.outbox.retry.initial-delay")
                .hasMessageContaining("PT10M");
    }

    @Test
    @DisplayName("Given a zero initial delay, when creating the policy, then it is rejected naming the property and its value")
    void givenZeroInitialDelay_whenCreatingPolicy_thenItIsRejectedNamingThePropertyAndItsValue() {
        assertThatThrownBy(() -> new OutboxRetryPolicy(8, Duration.ZERO, Duration.ofMinutes(5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("eshop.outbox.retry.initial-delay")
                .hasMessageContaining("PT0S");
    }

    @Test
    @DisplayName("Given zero max attempts, when creating the policy, then it is rejected naming the property and its value")
    void givenZeroMaxAttempts_whenCreatingPolicy_thenItIsRejectedNamingThePropertyAndItsValue() {
        assertThatThrownBy(() -> new OutboxRetryPolicy(0, Duration.ofSeconds(5), Duration.ofMinutes(5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("eshop.outbox.retry.max-attempts")
                .hasMessageContaining("was 0");
    }

    @Test
    @DisplayName("Given no failure yet, when computing the next attempt, then it is rejected explaining the count starts at 1")
    void givenNoFailureYet_whenComputingNextAttempt_thenItIsRejectedExplainingTheCountStartsAtOne() {
        assertThatThrownBy(() -> policy.nextAttemptAfter(0, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 1, but was 0");
    }

}