package com.adwitiya.feedbackportal.unit;

import com.adwitiya.feedbackportal.domain.entity.Department;
import com.adwitiya.feedbackportal.domain.entity.Feedback;
import com.adwitiya.feedbackportal.domain.entity.Student;
import com.adwitiya.feedbackportal.domain.entity.User;
import com.adwitiya.feedbackportal.domain.enums.FeedbackCategory;
import com.adwitiya.feedbackportal.domain.enums.FeedbackPriority;
import com.adwitiya.feedbackportal.domain.enums.FeedbackStatus;
import com.adwitiya.feedbackportal.domain.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Feedback aggregate")
class FeedbackEntityTest {

    private Feedback feedback;
    private User actor;

    @BeforeEach
    void setUp() {
        Department department = Department.builder().id(1L).code("CSE").name("Computer Science").active(true).build();
        User studentUser = User.builder().id(10L).email("s@u.edu").fullName("A Student").role(Role.STUDENT).build();
        Student student = Student.builder().userId(10L).user(studentUser).rollNumber("2023CS1001")
                .department(department).build();

        actor = User.builder().id(20L).email("a@u.edu").fullName("An Admin").role(Role.ADMIN).build();

        feedback = Feedback.builder()
                .id(100L)
                .ticketNumber("FB-2026-000100")
                .title("Lab computers are slow")
                .description("They take ten minutes to boot.")
                .category(FeedbackCategory.IT_SUPPORT)
                .priority(FeedbackPriority.MEDIUM)
                .status(FeedbackStatus.OPEN)
                .submittedBy(student)
                .department(department)
                .build();
        feedback.setCreatedAt(Instant.now().minus(2, ChronoUnit.DAYS));
    }

    @Nested
    @DisplayName("transitionTo")
    class Transitions {

        @Test
        void appliesALegalTransitionAndRecordsHistory() {
            feedback.transitionTo(FeedbackStatus.IN_PROGRESS, actor, "Picked up");

            assertThat(feedback.getStatus()).isEqualTo(FeedbackStatus.IN_PROGRESS);
            assertThat(feedback.getStatusHistory()).hasSize(1);
            assertThat(feedback.getStatusHistory().getFirst().getFromStatus()).isEqualTo(FeedbackStatus.OPEN);
            assertThat(feedback.getStatusHistory().getFirst().getToStatus()).isEqualTo(FeedbackStatus.IN_PROGRESS);
            assertThat(feedback.getStatusHistory().getFirst().getChangedBy()).isEqualTo(actor);
            assertThat(feedback.getStatusHistory().getFirst().getNote()).isEqualTo("Picked up");
        }

        @Test
        void refusesAnIllegalTransitionAndLeavesStateUntouched() {
            assertThatThrownBy(() -> feedback.transitionTo(FeedbackStatus.CLOSED, actor, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("FB-2026-000100");

            assertThat(feedback.getStatus()).isEqualTo(FeedbackStatus.OPEN);
            assertThat(feedback.getStatusHistory()).isEmpty();
        }

        @Test
        void stampsResolvedAtOnResolution() {
            feedback.transitionTo(FeedbackStatus.RESOLVED, actor, null);
            assertThat(feedback.getResolvedAt()).isNotNull();
            assertThat(feedback.getClosedAt()).isNull();
        }

        @Test
        void stampsClosedAtOnClosure() {
            feedback.transitionTo(FeedbackStatus.RESOLVED, actor, null);
            feedback.transitionTo(FeedbackStatus.CLOSED, actor, "Confirmed");

            assertThat(feedback.getClosedAt()).isNotNull();
            assertThat(feedback.getStatusHistory()).hasSize(2);
        }

        @Test
        void reopeningClearsTheResolutionStamps() {
            feedback.transitionTo(FeedbackStatus.RESOLVED, actor, null);
            assertThat(feedback.getResolvedAt()).isNotNull();

            feedback.transitionTo(FeedbackStatus.IN_PROGRESS, actor, "Student reopened it");

            assertThat(feedback.getResolvedAt()).isNull();
            assertThat(feedback.getClosedAt()).isNull();
            assertThat(feedback.getStatus()).isEqualTo(FeedbackStatus.IN_PROGRESS);
        }
    }

    @Nested
    @DisplayName("SLA")
    class Sla {

        @Test
        void dueDateDerivesFromPriority() {
            feedback.setPriority(FeedbackPriority.URGENT);
            feedback.applySla();

            assertThat(feedback.getDueAt())
                    .isEqualTo(feedback.getCreatedAt().plus(FeedbackPriority.URGENT.getResolutionSla()));
        }

        @Test
        void higherPriorityMeansAnEarlierDeadline() {
            feedback.setPriority(FeedbackPriority.LOW);
            feedback.applySla();
            Instant lowDue = feedback.getDueAt();

            feedback.setPriority(FeedbackPriority.HIGH);
            feedback.applySla();

            assertThat(feedback.getDueAt()).isBefore(lowDue);
        }

        @Test
        void activeTicketPastItsDeadlineIsOverdue() {
            feedback.setDueAt(Instant.now().minus(1, ChronoUnit.HOURS));
            assertThat(feedback.isOverdue()).isTrue();
        }

        @Test
        void closedTicketIsNeverOverdue() {
            feedback.setDueAt(Instant.now().minus(10, ChronoUnit.DAYS));
            feedback.transitionTo(FeedbackStatus.RESOLVED, actor, null);
            feedback.transitionTo(FeedbackStatus.CLOSED, actor, null);

            assertThat(feedback.isOverdue()).isFalse();
        }

        @Test
        void ticketWithoutADeadlineIsNotOverdue() {
            feedback.setDueAt(null);
            assertThat(feedback.isOverdue()).isFalse();
        }
    }

    @Nested
    @DisplayName("priority ranking")
    class Ranking {

        @Test
        void weightsAreOrdered() {
            assertThat(FeedbackPriority.URGENT.isAtLeast(FeedbackPriority.HIGH)).isTrue();
            assertThat(FeedbackPriority.HIGH.isAtLeast(FeedbackPriority.HIGH)).isTrue();
            assertThat(FeedbackPriority.LOW.isAtLeast(FeedbackPriority.MEDIUM)).isFalse();
        }
    }
}
