package com.adwitiya.feedbackportal.service;

import com.adwitiya.feedbackportal.domain.entity.Admin;
import com.adwitiya.feedbackportal.domain.entity.Attachment;
import com.adwitiya.feedbackportal.domain.entity.Department;
import com.adwitiya.feedbackportal.domain.entity.Feedback;
import com.adwitiya.feedbackportal.domain.entity.FeedbackComment;
import com.adwitiya.feedbackportal.domain.entity.Student;
import com.adwitiya.feedbackportal.domain.entity.User;
import com.adwitiya.feedbackportal.domain.enums.FeedbackStatus;
import com.adwitiya.feedbackportal.domain.enums.NotificationType;
import com.adwitiya.feedbackportal.exception.BusinessRuleException;
import com.adwitiya.feedbackportal.exception.ResourceNotFoundException;
import com.adwitiya.feedbackportal.integration.AnalyticsClient;
import com.adwitiya.feedbackportal.repository.AdminRepository;
import com.adwitiya.feedbackportal.repository.AttachmentRepository;
import com.adwitiya.feedbackportal.repository.DepartmentRepository;
import com.adwitiya.feedbackportal.repository.FeedbackCommentRepository;
import com.adwitiya.feedbackportal.repository.FeedbackRepository;
import com.adwitiya.feedbackportal.repository.FeedbackSpecifications;
import com.adwitiya.feedbackportal.repository.FeedbackStatusHistoryRepository;
import com.adwitiya.feedbackportal.repository.StudentRepository;
import com.adwitiya.feedbackportal.repository.UserRepository;
import com.adwitiya.feedbackportal.security.AppUserDetails;
import com.adwitiya.feedbackportal.web.dto.request.AddCommentRequest;
import com.adwitiya.feedbackportal.web.dto.request.CreateFeedbackRequest;
import com.adwitiya.feedbackportal.web.dto.request.FeedbackFilterRequest;
import com.adwitiya.feedbackportal.web.dto.request.RateFeedbackRequest;
import com.adwitiya.feedbackportal.web.dto.request.UpdateFeedbackStatusRequest;
import com.adwitiya.feedbackportal.web.dto.response.AttachmentResponse;
import com.adwitiya.feedbackportal.web.dto.response.CommentResponse;
import com.adwitiya.feedbackportal.web.dto.response.FeedbackDetailResponse;
import com.adwitiya.feedbackportal.web.dto.response.FeedbackSummaryResponse;
import com.adwitiya.feedbackportal.web.dto.response.PageResponse;
import com.adwitiya.feedbackportal.web.mapper.FeedbackMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * The feedback lifecycle: submit, list, read, comment, transition, rate.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private static final List<FeedbackStatus> ACTIVE_STATUSES =
            List.of(FeedbackStatus.OPEN, FeedbackStatus.IN_PROGRESS, FeedbackStatus.AWAITING_STUDENT);

    private final FeedbackRepository feedbackRepository;
    private final FeedbackCommentRepository commentRepository;
    private final FeedbackStatusHistoryRepository historyRepository;
    private final AttachmentRepository attachmentRepository;
    private final StudentRepository studentRepository;
    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    private final TicketNumberService ticketNumberService;
    private final AnalyticsClient analyticsClient;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final FileStorageService fileStorageService;
    private final FeedbackMapper mapper;

    // ==================================================================
    //  Submission
    // ==================================================================

    /**
     * Records a new piece of feedback on behalf of the signed-in student.
     *
     * <p>The ticket number, status, SLA deadline and assignee are all decided
     * here. Enrichment by the analytics service is scheduled for after the
     * transaction commits so a slow model cannot lengthen the request.</p>
     */
    @Transactional
    public FeedbackDetailResponse submit(CreateFeedbackRequest request, AppUserDetails principal) {
        Student student = studentRepository.findByUserId(principal.id())
                .orElseThrow(() -> new BusinessRuleException("Only students can submit feedback"));

        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> ResourceNotFoundException.of("Department", request.departmentId()));
        if (!department.isActive()) {
            throw new BusinessRuleException("Department '%s' is not accepting feedback".formatted(department.getName()));
        }

        Feedback feedback = Feedback.builder()
                .ticketNumber(ticketNumberService.nextTicketNumber())
                .title(request.title().trim())
                .description(request.description().trim())
                .category(request.category())
                .status(FeedbackStatus.OPEN)
                .submittedBy(student)
                .department(department)
                .anonymous(request.anonymous())
                .build();

        autoAssign(feedback, department);
        Feedback saved = feedbackRepository.save(feedback);
        saved.applySla();

        auditService.record("FEEDBACK_SUBMITTED", "Feedback", saved.getId(), saved.getTicketNumber());
        notifyAssignee(saved, NotificationType.FEEDBACK_ASSIGNED,
                "New feedback assigned",
                "%s has been routed to your department.".formatted(saved.getTicketNumber()));

        enrichAfterCommit(saved.getId());

        return mapper.toDetail(saved, List.of(), List.of(), List.of(), true);
    }

    /**
     * Calls the analytics service once the feedback row is durably committed.
     *
     * <p>Registering the callback rather than calling inline keeps the model
     * off the critical path and guarantees it never sees a row that was later
     * rolled back.</p>
     */
    private void enrichAfterCommit(Long feedbackId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            applyAnalysis(feedbackId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                applyAnalysis(feedbackId);
            }
        });
    }

    /** Applies sentiment and category suggestions. Best-effort by design. */
    @Transactional
    public void applyAnalysis(Long feedbackId) {
        feedbackRepository.findById(feedbackId).ifPresent(feedback ->
                analyticsClient.analyse(feedback.getTitle(), feedback.getDescription()).ifPresent(result -> {
                    feedback.setSentimentLabel(result.sentiment());
                    feedback.setSentimentScore(result.sentimentScore());
                    feedback.setSuggestedCategory(result.suggestedCategory());
                    feedback.setSuggestedPriority(result.suggestedPriority());
                    feedback.setAnalysisConfidence(result.confidence());
                    feedback.setAnalysedAt(java.time.Instant.now());

                    // Only let the model raise urgency, never lower what a human set.
                    if (result.suggestedPriority().isAtLeast(feedback.getPriority())
                            && result.confidence() >= 0.6) {
                        feedback.setPriority(result.suggestedPriority());
                        feedback.applySla();
                    }
                    feedbackRepository.save(feedback);
                    log.debug("Enriched {} as {} ({})", feedback.getTicketNumber(),
                            result.sentimentLabel(), result.category());
                }));
    }

    /** Routes new feedback to the least-loaded administrator in the department. */
    private void autoAssign(Feedback feedback, Department department) {
        adminRepository.findLeastLoadedAdminIds(department.getId(), PageRequest.of(0, 1)).stream()
                .findFirst()
                .flatMap(adminRepository::findByUserId)
                .ifPresent(feedback::setAssignedTo);
    }

    // ==================================================================
    //  Reads
    // ==================================================================

    /**
     * Lists feedback the caller is entitled to see.
     *
     * <p>The visibility rule is applied as an extra specification rather than
     * being left to the caller's filter, so an over-broad request narrows to
     * the caller's own scope instead of leaking.</p>
     */
    @Transactional(readOnly = true)
    public PageResponse<FeedbackSummaryResponse> list(FeedbackFilterRequest filter,
                                                      AppUserDetails principal,
                                                      Pageable pageable) {
        Specification<Feedback> specification = visibilityScope(principal)
                .and(FeedbackSpecifications.hasStatus(filter.status()))
                .and(FeedbackSpecifications.hasCategory(filter.category()))
                .and(FeedbackSpecifications.hasPriority(filter.priority()))
                .and(FeedbackSpecifications.assignedTo(filter.assignedToId()))
                .and(FeedbackSpecifications.textContains(filter.search()))
                .and(FeedbackSpecifications.createdBetween(filter.from(), filter.to()))
                .and(FeedbackSpecifications.overdue(filter.overdue()));

        // A super-administrator may additionally narrow by department.
        if (principal.isSuperAdmin() && filter.departmentId() != null) {
            specification = specification.and(FeedbackSpecifications.inDepartment(filter.departmentId()));
        }

        Page<Feedback> page = feedbackRepository.findAll(specification, pageable);
        return PageResponse.from(page, mapper::toSummary);
    }

    /**
     * Builds the visibility predicate for a principal:
     * students see their own tickets, department staff see their department's,
     * super-administrators see everything.
     */
    private Specification<Feedback> visibilityScope(AppUserDetails principal) {
        if (principal.isSuperAdmin()) {
            return FeedbackSpecifications.all();
        }
        if (principal.isStaff()) {
            return FeedbackSpecifications.inDepartment(principal.departmentId());
        }
        return FeedbackSpecifications.submittedBy(principal.id());
    }

    @Transactional(readOnly = true)
    public FeedbackDetailResponse get(Long feedbackId, AppUserDetails principal) {
        Feedback feedback = loadVisible(feedbackId, principal);

        boolean staff = principal.isStaff();
        List<FeedbackComment> comments = staff
                ? commentRepository.findByFeedbackIdOrderByCreatedAtAsc(feedbackId)
                : commentRepository.findByFeedbackIdAndInternalNoteFalseOrderByCreatedAtAsc(feedbackId);

        return mapper.toDetail(
                feedback,
                comments,
                historyRepository.findByFeedbackIdOrderByChangedAtAsc(feedbackId),
                attachmentRepository.findByFeedbackId(feedbackId),
                // The submitter always sees their own name; staff do not, if anonymous.
                !staff);
    }

    @Transactional(readOnly = true)
    public PageResponse<FeedbackSummaryResponse> search(String term, AppUserDetails principal, Pageable pageable) {
        if (term == null || term.isBlank()) {
            return list(new FeedbackFilterRequest(null, null, null, null, null, null, false, null, null),
                    principal, pageable);
        }
        // Full-text search is unfiltered at the index level, so re-apply scope in Java.
        Page<Feedback> page = feedbackRepository.fullTextSearch(term.trim(), pageable);
        List<FeedbackSummaryResponse> visible = page.getContent().stream()
                .filter(feedback -> canView(feedback, principal))
                .map(mapper::toSummary)
                .toList();

        return new PageResponse<>(visible, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
    }

    // ==================================================================
    //  Workflow
    // ==================================================================

    /**
     * Moves a ticket to a new state.
     *
     * @throws BusinessRuleException if the transition is not permitted by the state machine
     */
    @Transactional
    public FeedbackDetailResponse changeStatus(Long feedbackId,
                                               UpdateFeedbackStatusRequest request,
                                               AppUserDetails principal) {
        Feedback feedback = loadManageable(feedbackId, principal);
        User actor = requireUser(principal.id());

        FeedbackStatus previous = feedback.getStatus();
        try {
            feedback.transitionTo(request.status(), actor, request.note());
        } catch (IllegalStateException ex) {
            throw new BusinessRuleException(ex.getMessage());
        }
        feedbackRepository.save(feedback);

        auditService.record("FEEDBACK_STATUS_CHANGED", "Feedback", feedbackId,
                "%s -> %s".formatted(previous, request.status()));

        notifySubmitter(feedback,
                request.status() == FeedbackStatus.RESOLVED
                        ? NotificationType.FEEDBACK_RESOLVED
                        : NotificationType.FEEDBACK_STATUS_CHANGED,
                "Status updated to " + request.status(),
                request.note() != null && !request.note().isBlank()
                        ? request.note()
                        : "Your feedback moved from %s to %s.".formatted(previous, request.status()));

        return get(feedbackId, principal);
    }

    /** Reassigns a ticket to a named administrator within the same department. */
    @Transactional
    public FeedbackDetailResponse assign(Long feedbackId, Long adminUserId, AppUserDetails principal) {
        Feedback feedback = loadManageable(feedbackId, principal);
        Admin assignee = adminRepository.findByUserId(adminUserId)
                .orElseThrow(() -> ResourceNotFoundException.of("Administrator", adminUserId));

        if (!assignee.getDepartment().getId().equals(feedback.getDepartment().getId())) {
            throw new BusinessRuleException("An administrator can only be assigned feedback from their own department");
        }

        feedback.setAssignedTo(assignee);
        feedbackRepository.save(feedback);
        auditService.record("FEEDBACK_ASSIGNED", "Feedback", feedbackId, assignee.getEmail());

        notificationService.notifyAboutFeedback(assignee.getUser(), feedback,
                NotificationType.FEEDBACK_ASSIGNED, "Feedback assigned to you",
                "%s is now assigned to you.".formatted(feedback.getTicketNumber()));

        return get(feedbackId, principal);
    }

    /** Adds a message to the thread. Students may not write internal notes. */
    @Transactional
    public CommentResponse addComment(Long feedbackId, AddCommentRequest request, AppUserDetails principal) {
        Feedback feedback = loadVisible(feedbackId, principal);
        if (feedback.getStatus().isTerminal()) {
            throw new BusinessRuleException("This ticket is closed and cannot receive new comments");
        }

        boolean internal = request.internalNote() && principal.isStaff();
        User author = requireUser(principal.id());

        FeedbackComment comment = FeedbackComment.builder()
                .feedback(feedback)
                .author(author)
                .body(request.body().trim())
                .internalNote(internal)
                .build();
        FeedbackComment saved = commentRepository.save(comment);

        if (!internal) {
            if (principal.isStaff()) {
                notifySubmitter(feedback, NotificationType.FEEDBACK_COMMENTED,
                        "New reply on " + feedback.getTicketNumber(), truncate(request.body(), 400));
            } else {
                notifyAssignee(feedback, NotificationType.FEEDBACK_COMMENTED,
                        "Student replied on " + feedback.getTicketNumber(), truncate(request.body(), 400));
            }
        }

        return mapper.toComment(saved);
    }

    /** Records the student's satisfaction rating once a ticket is resolved. */
    @Transactional
    public FeedbackDetailResponse rate(Long feedbackId, RateFeedbackRequest request, AppUserDetails principal) {
        Feedback feedback = loadVisible(feedbackId, principal);

        if (feedback.getSubmittedBy() == null || !feedback.getSubmittedBy().getUserId().equals(principal.id())) {
            throw new BusinessRuleException("Only the student who submitted this feedback can rate it");
        }
        if (feedback.getStatus() != FeedbackStatus.RESOLVED && feedback.getStatus() != FeedbackStatus.CLOSED) {
            throw new BusinessRuleException("Feedback can only be rated once it has been resolved");
        }

        feedback.setSatisfactionRating(request.rating());
        feedbackRepository.save(feedback);
        auditService.record("FEEDBACK_RATED", "Feedback", feedbackId, String.valueOf(request.rating()));

        return get(feedbackId, principal);
    }

    // ==================================================================
    //  Attachments
    // ==================================================================

    @Transactional
    public AttachmentResponse attach(Long feedbackId, MultipartFile file, AppUserDetails principal) {
        Feedback feedback = loadVisible(feedbackId, principal);
        String storageKey = fileStorageService.store(file, "feedback/" + feedbackId);

        Attachment attachment = Attachment.builder()
                .feedback(feedback)
                .fileName(sanitiseFileName(file.getOriginalFilename()))
                .contentType(file.getContentType())
                .sizeBytes(file.getSize())
                .storageKey(storageKey)
                .uploadedBy(requireUser(principal.id()))
                .build();

        return mapper.toAttachment(attachmentRepository.save(attachment));
    }

    @Transactional(readOnly = true)
    public Attachment loadAttachment(Long feedbackId, Long attachmentId, AppUserDetails principal) {
        loadVisible(feedbackId, principal);
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Attachment", attachmentId));

        if (!attachment.getFeedback().getId().equals(feedbackId)) {
            throw ResourceNotFoundException.of("Attachment", attachmentId);
        }
        return attachment;
    }

    @Transactional(readOnly = true)
    public byte[] downloadAttachment(Attachment attachment) {
        return fileStorageService.retrieve(attachment.getStorageKey());
    }

    // ==================================================================
    //  Counts used by the dashboards
    // ==================================================================

    @Transactional(readOnly = true)
    public long countActiveForStudent(Long studentUserId) {
        return feedbackRepository.count(FeedbackSpecifications.submittedBy(studentUserId)
                .and(FeedbackSpecifications.hasStatusIn(ACTIVE_STATUSES)));
    }

    @Transactional(readOnly = true)
    public long countAssignedActive(Long adminUserId) {
        return feedbackRepository.countByAssignedToUserIdAndStatusIn(adminUserId, ACTIVE_STATUSES);
    }

    // ==================================================================
    //  Access helpers
    // ==================================================================

    private Feedback loadVisible(Long feedbackId, AppUserDetails principal) {
        Feedback feedback = feedbackRepository.findWithDetailsById(feedbackId)
                .orElseThrow(() -> ResourceNotFoundException.of("Feedback", feedbackId));

        if (!canView(feedback, principal)) {
            // 404 rather than 403: an outsider should not learn that the id exists.
            throw ResourceNotFoundException.of("Feedback", feedbackId);
        }
        return feedback;
    }

    private Feedback loadManageable(Long feedbackId, AppUserDetails principal) {
        Feedback feedback = loadVisible(feedbackId, principal);
        if (!principal.isStaff()) {
            throw new BusinessRuleException("Only department staff can change a ticket's workflow state");
        }
        return feedback;
    }

    private boolean canView(Feedback feedback, AppUserDetails principal) {
        if (principal.isSuperAdmin()) {
            return true;
        }
        if (principal.isStaff()) {
            return principal.departmentId() != null
                    && feedback.getDepartment() != null
                    && principal.departmentId().equals(feedback.getDepartment().getId());
        }
        return feedback.getSubmittedBy() != null
                && principal.id().equals(feedback.getSubmittedBy().getUserId());
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
    }

    private void notifySubmitter(Feedback feedback, NotificationType type, String title, String message) {
        if (feedback.getSubmittedBy() != null && feedback.getSubmittedBy().getUser() != null) {
            notificationService.notifyAboutFeedback(
                    feedback.getSubmittedBy().getUser(), feedback, type, title, message);
        }
    }

    private void notifyAssignee(Feedback feedback, NotificationType type, String title, String message) {
        if (feedback.getAssignedTo() != null && feedback.getAssignedTo().getUser() != null) {
            notificationService.notifyAboutFeedback(
                    feedback.getAssignedTo().getUser(), feedback, type, title, message);
        }
    }

    /** Strips any path component a client may have put in the filename. */
    private String sanitiseFileName(String original) {
        if (original == null || original.isBlank()) {
            return "attachment";
        }
        String base = original.replace('\\', '/');
        base = base.substring(base.lastIndexOf('/') + 1);
        base = base.replaceAll("[^A-Za-z0-9._-]", "_");
        return base.length() > 255 ? base.substring(base.length() - 255) : base;
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }
}
