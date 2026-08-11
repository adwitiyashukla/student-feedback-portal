package com.adwitiya.feedbackportal.web.api;

import com.adwitiya.feedbackportal.domain.entity.Attachment;
import com.adwitiya.feedbackportal.security.AppUserDetails;
import com.adwitiya.feedbackportal.service.FeedbackService;
import com.adwitiya.feedbackportal.web.dto.request.AddCommentRequest;
import com.adwitiya.feedbackportal.web.dto.request.AssignFeedbackRequest;
import com.adwitiya.feedbackportal.web.dto.request.CreateFeedbackRequest;
import com.adwitiya.feedbackportal.web.dto.request.FeedbackFilterRequest;
import com.adwitiya.feedbackportal.web.dto.request.RateFeedbackRequest;
import com.adwitiya.feedbackportal.web.dto.request.UpdateFeedbackStatusRequest;
import com.adwitiya.feedbackportal.web.dto.response.AttachmentResponse;
import com.adwitiya.feedbackportal.web.dto.response.CommentResponse;
import com.adwitiya.feedbackportal.web.dto.response.FeedbackDetailResponse;
import com.adwitiya.feedbackportal.web.dto.response.FeedbackSummaryResponse;
import com.adwitiya.feedbackportal.web.dto.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@Tag(name = "Feedback", description = "Submit, track and resolve student feedback")
@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
public class FeedbackController {
    private final FeedbackService feedbackService;

    @Operation(summary = "Submit new feedback",
            description = "Students only. The ticket number, status, SLA and initial assignee are set by the server.")
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping
    public ResponseEntity<FeedbackDetailResponse> submit(@Valid @RequestBody CreateFeedbackRequest request,
                                                         @AuthenticationPrincipal AppUserDetails principal) {
        FeedbackDetailResponse created = feedbackService.submit(request, principal);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "List feedback visible to the caller",
            description = "Students see their own tickets, department staff see their department's, "
                    + "super-administrators see everything.")
    @GetMapping
    public PageResponse<FeedbackSummaryResponse> list(
            @ModelAttribute FeedbackFilterRequest filter,
            @AuthenticationPrincipal AppUserDetails principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return feedbackService.list(filter, principal, pageable);
    }

    @Operation(summary = "Full-text search over titles and descriptions")
    @GetMapping("/search")
    public PageResponse<FeedbackSummaryResponse> search(
            @RequestParam("q") String term,
            @AuthenticationPrincipal AppUserDetails principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return feedbackService.search(term, principal, pageable);
    }

    @Operation(summary = "Read one piece of feedback with its full thread")
    @GetMapping("/{id}")
    public FeedbackDetailResponse get(@PathVariable Long id,
                                      @AuthenticationPrincipal AppUserDetails principal) {
        return feedbackService.get(id, principal);
    }

    @Operation(summary = "Move a ticket to a new workflow state",
            description = "Staff only. Rejected with 422 if the state machine does not allow the transition.")
    @PreAuthorize("@feedbackAccess.canManage(#id, principal)")
    @PatchMapping("/{id}/status")
    public FeedbackDetailResponse changeStatus(@PathVariable Long id,
                                               @Valid @RequestBody UpdateFeedbackStatusRequest request,
                                               @AuthenticationPrincipal AppUserDetails principal) {
        return feedbackService.changeStatus(id, request, principal);
    }

    @Operation(summary = "Assign a ticket to an administrator in the same department")
    @PreAuthorize("@feedbackAccess.canManage(#id, principal)")
    @PatchMapping("/{id}/assignee")
    public FeedbackDetailResponse assign(@PathVariable Long id,
                                         @Valid @RequestBody AssignFeedbackRequest request,
                                         @AuthenticationPrincipal AppUserDetails principal) {
        return feedbackService.assign(id, request.adminUserId(), principal);
    }

    @Operation(summary = "Add a message to the thread",
            description = "`internalNote: true` is honoured for staff only and is never shown to the student.")
    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentResponse> comment(@PathVariable Long id,
                                                   @Valid @RequestBody AddCommentRequest request,
                                                   @AuthenticationPrincipal AppUserDetails principal) {
        return ResponseEntity.status(201).body(feedbackService.addComment(id, request, principal));
    }

    @Operation(summary = "Rate the resolution", description = "The submitting student only, once resolved.")
    @PreAuthorize("@feedbackAccess.isSubmitter(#id, principal)")
    @PostMapping("/{id}/rating")
    public FeedbackDetailResponse rate(@PathVariable Long id,
                                       @Valid @RequestBody RateFeedbackRequest request,
                                       @AuthenticationPrincipal AppUserDetails principal) {
        return feedbackService.rate(id, request, principal);
    }

    @Operation(summary = "Attach a file", description = "Images and PDFs up to 5 MB.")
    @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttachmentResponse> attach(@PathVariable Long id,
                                                     @RequestPart("file") MultipartFile file,
                                                     @AuthenticationPrincipal AppUserDetails principal) {
        return ResponseEntity.status(201).body(feedbackService.attach(id, file, principal));
    }

    @Operation(summary = "Download an attachment")
    @GetMapping("/{id}/attachments/{attachmentId}")
    public ResponseEntity<byte[]> download(@PathVariable Long id,
                                           @PathVariable Long attachmentId,
                                           @AuthenticationPrincipal AppUserDetails principal) {
        Attachment attachment = feedbackService.loadAttachment(id, attachmentId, principal);
        byte[] content = feedbackService.downloadAttachment(attachment);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(attachment.getFileName()).build().toString())

                .header("X-Content-Type-Options", "nosniff")
                .body(content);
    }
}
