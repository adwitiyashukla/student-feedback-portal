package com.adwitiya.feedbackportal.web.dto.response;

import com.adwitiya.feedbackportal.domain.enums.FeedbackCategory;
import com.adwitiya.feedbackportal.domain.enums.FeedbackPriority;
import com.adwitiya.feedbackportal.domain.enums.SentimentLabel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Enrichment returned by the Python analytics service.
 *
 * <p>Unknown properties are ignored on purpose so the Python side can add
 * fields without breaking this deserialiser.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AnalysisResult(

        @JsonProperty("sentiment_label") String sentimentLabel,
        @JsonProperty("sentiment_score") Double sentimentScore,
        @JsonProperty("category") String category,
        @JsonProperty("category_confidence") Double categoryConfidence,
        @JsonProperty("priority") String priority,
        @JsonProperty("keywords") java.util.List<String> keywords,
        @JsonProperty("model_version") String modelVersion
) {

    public SentimentLabel sentiment() {
        return SentimentLabel.fromLabel(sentimentLabel);
    }

    public FeedbackCategory suggestedCategory() {
        return FeedbackCategory.fromLabel(category);
    }

    public FeedbackPriority suggestedPriority() {
        return FeedbackPriority.fromLabel(priority);
    }

    public double confidence() {
        return categoryConfidence == null ? 0.0 : categoryConfidence;
    }
}
