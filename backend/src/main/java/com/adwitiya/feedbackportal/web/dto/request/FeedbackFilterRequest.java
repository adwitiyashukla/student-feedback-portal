package com.adwitiya.feedbackportal.web.dto.request;

import com.adwitiya.feedbackportal.domain.enums.FeedbackCategory;
import com.adwitiya.feedbackportal.domain.enums.FeedbackPriority;
import com.adwitiya.feedbackportal.domain.enums.FeedbackStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;

/**
 * Query parameters for the feedback list endpoints. Every field is optional;
 * unset fields are simply omitted from the generated SQL.
 */
@Schema(description = "Optional filters for listing feedback")
public record FeedbackFilterRequest(

        FeedbackStatus status,

        FeedbackCategory category,

        FeedbackPriority priority,

        Long departmentId,

        Long assignedToId,

        @Schema(description = "Free-text match on ticket number, title or description")
        String search,

        @Schema(description = "Only tickets past their SLA deadline")
        Boolean overdue,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant from,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant to
) {

    /**
     * Normalises {@code overdue} so callers never see null.
     *
     * <p>This field was declared {@code boolean}. An unticked checkbox submits
     * nothing at all, so the parameter arrived absent, and Spring cannot bind
     * null to a primitive - every visit to the queue without an explicit
     * {@code ?overdue=} failed with a 400 before the controller ever ran,
     * which contradicted this type's promise that every field is optional.
     * {@code Boolean} accepts the absent case; normalising here keeps
     * {@code overdue()} non-null for callers and templates.</p>
     */
    public FeedbackFilterRequest {
        overdue = overdue != null && overdue;
    }
}
