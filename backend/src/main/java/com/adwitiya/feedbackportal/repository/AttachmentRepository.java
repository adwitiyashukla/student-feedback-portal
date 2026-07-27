package com.adwitiya.feedbackportal.repository;

import com.adwitiya.feedbackportal.domain.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByFeedbackId(Long feedbackId);

    long countByFeedbackId(Long feedbackId);
}
