package com.adwitiya.feedbackportal.unit;

import com.adwitiya.feedbackportal.repository.FeedbackRepository;
import com.adwitiya.feedbackportal.service.TicketNumberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Year;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * Ticket numbering.
 *
 * <p>Replaces {@code new Random().nextInt(90000) + 10000} evaluated in the
 * student's browser and posted as a form field.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TicketNumberService")
class TicketNumberServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    @InjectMocks
    private TicketNumberService ticketNumberService;

    @Test
    void startsAtOneForAFreshYear() {
        given(feedbackRepository.findMaxTicketSequenceForYear(anyString())).willReturn(Optional.empty());
        given(feedbackRepository.existsByTicketNumber(anyString())).willReturn(false);

        assertThat(ticketNumberService.nextTicketNumber())
                .isEqualTo("FB-%d-000001".formatted(Year.now().getValue()));
    }

    @Test
    void continuesFromTheHighestExistingSequence() {
        given(feedbackRepository.findMaxTicketSequenceForYear(anyString())).willReturn(Optional.of(41L));
        given(feedbackRepository.existsByTicketNumber(anyString())).willReturn(false);

        assertThat(ticketNumberService.nextTicketNumber())
                .isEqualTo("FB-%d-000042".formatted(Year.now().getValue()));
    }

    @Test
    void skipsPastACollision() {
        given(feedbackRepository.findMaxTicketSequenceForYear(anyString())).willReturn(Optional.of(10L));
        // Two concurrent submissions agreed on MAX = 10; 11 is already taken.
        given(feedbackRepository.existsByTicketNumber(anyString()))
                .willReturn(true)
                .willReturn(false);

        assertThat(ticketNumberService.nextTicketNumber())
                .isEqualTo("FB-%d-000012".formatted(Year.now().getValue()));
    }

    @Test
    void isZeroPaddedAndSortable() {
        given(feedbackRepository.findMaxTicketSequenceForYear(anyString())).willReturn(Optional.of(8L));
        given(feedbackRepository.existsByTicketNumber(anyString())).willReturn(false);

        String ticket = ticketNumberService.nextTicketNumber();

        assertThat(ticket).matches("FB-\\d{4}-\\d{6}");
        assertThat(ticket).endsWith("000009");
    }
}
