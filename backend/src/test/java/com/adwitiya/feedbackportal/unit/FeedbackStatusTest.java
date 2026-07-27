package com.adwitiya.feedbackportal.unit;

import com.adwitiya.feedbackportal.domain.enums.FeedbackStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The workflow state machine.
 */
@DisplayName("FeedbackStatus state machine")
class FeedbackStatusTest {

    @Nested
    @DisplayName("permitted transitions")
    class Permitted {

        @ParameterizedTest(name = "{0} -> {1} is allowed")
        @CsvSource({
                "OPEN, IN_PROGRESS",
                "OPEN, AWAITING_STUDENT",
                "OPEN, RESOLVED",
                "OPEN, REJECTED",
                "IN_PROGRESS, RESOLVED",
                "IN_PROGRESS, AWAITING_STUDENT",
                "AWAITING_STUDENT, IN_PROGRESS",
                "RESOLVED, CLOSED",
                "RESOLVED, IN_PROGRESS",
        })
        void allowsForwardAndReopenTransitions(FeedbackStatus from, FeedbackStatus to) {
            assertThat(from.canTransitionTo(to)).isTrue();
        }
    }

    @Nested
    @DisplayName("rejected transitions")
    class Rejected {

        @ParameterizedTest(name = "{0} -> {1} is rejected")
        @CsvSource({
                "OPEN, OPEN",
                "OPEN, CLOSED",
                "CLOSED, OPEN",
                "CLOSED, IN_PROGRESS",
                "REJECTED, OPEN",
                "REJECTED, RESOLVED",
                "IN_PROGRESS, OPEN",
                "RESOLVED, OPEN",
        })
        void rejectsIllegalTransitions(FeedbackStatus from, FeedbackStatus to) {
            assertThat(from.canTransitionTo(to)).isFalse();
        }

        @ParameterizedTest
        @EnumSource(FeedbackStatus.class)
        void nullTargetIsAlwaysRejected(FeedbackStatus from) {
            assertThat(from.canTransitionTo(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("terminal and active classification")
    class Classification {

        @Test
        void closedAndRejectedAreTerminal() {
            assertThat(FeedbackStatus.CLOSED.isTerminal()).isTrue();
            assertThat(FeedbackStatus.REJECTED.isTerminal()).isTrue();
            assertThat(FeedbackStatus.CLOSED.allowedTransitions()).isEmpty();
            assertThat(FeedbackStatus.REJECTED.allowedTransitions()).isEmpty();
        }

        @ParameterizedTest
        @EnumSource(value = FeedbackStatus.class, names = {"OPEN", "IN_PROGRESS", "AWAITING_STUDENT"})
        void unfinishedStatesCountAsActive(FeedbackStatus status) {
            assertThat(status.isActive()).isTrue();
            assertThat(status.isTerminal()).isFalse();
        }

        @ParameterizedTest
        @EnumSource(value = FeedbackStatus.class, names = {"RESOLVED", "CLOSED", "REJECTED"})
        void finishedStatesAreNotActive(FeedbackStatus status) {
            assertThat(status.isActive()).isFalse();
        }

        @Test
        void everyTerminalStateIsUnreachableFromItself() {
            for (FeedbackStatus status : FeedbackStatus.values()) {
                assertThat(status.canTransitionTo(status))
                        .as("%s should not transition to itself", status)
                        .isFalse();
            }
        }
    }
}
