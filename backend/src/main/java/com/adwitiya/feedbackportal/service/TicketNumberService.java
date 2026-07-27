package com.adwitiya.feedbackportal.service;

import com.adwitiya.feedbackportal.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

/**
 * Mints human-readable ticket numbers of the form {@code FB-2026-000042}.
 */
@Service
@RequiredArgsConstructor
public class TicketNumberService {

    private static final String PREFIX = "FB";

    private final FeedbackRepository feedbackRepository;

    /**
     * @return the next unused ticket number for the current calendar year
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public String nextTicketNumber() {
        String year = String.valueOf(Year.now().getValue());
        long next = feedbackRepository.findMaxTicketSequenceForYear(year).orElse(0L) + 1;

        String candidate = format(year, next);
        // Defensive: concurrent submissions can agree on the same MAX.
        while (feedbackRepository.existsByTicketNumber(candidate)) {
            next++;
            candidate = format(year, next);
        }
        return candidate;
    }

    private String format(String year, long sequence) {
        return "%s-%s-%06d".formatted(PREFIX, year, sequence);
    }
}
