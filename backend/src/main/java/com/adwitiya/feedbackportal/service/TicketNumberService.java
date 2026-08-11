package com.adwitiya.feedbackportal.service;

import com.adwitiya.feedbackportal.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

@Service
@RequiredArgsConstructor
public class TicketNumberService {
    private static final String PREFIX = "FB";

    private final FeedbackRepository feedbackRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public String nextTicketNumber() {
        String year = String.valueOf(Year.now().getValue());
        long next = feedbackRepository.findMaxTicketSequenceForYear(year).orElse(0L) + 1;

        String candidate = format(year, next);

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
