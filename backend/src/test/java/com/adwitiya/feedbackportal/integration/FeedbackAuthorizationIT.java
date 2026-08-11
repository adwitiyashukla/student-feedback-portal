package com.adwitiya.feedbackportal.integration;

import com.adwitiya.feedbackportal.domain.enums.FeedbackCategory;
import com.adwitiya.feedbackportal.web.dto.request.CreateFeedbackRequest;
import com.adwitiya.feedbackportal.web.dto.request.LoginRequest;
import com.adwitiya.feedbackportal.web.dto.request.UpdateFeedbackStatusRequest;
import com.adwitiya.feedbackportal.domain.enums.FeedbackStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Feedback authorisation")
class FeedbackAuthorizationIT extends AbstractIntegrationTest {
    private static final String PASSWORD = "Password#123";
    private static final String STUDENT_A = "aarav.sharma1@student.university.edu";
    private static final String STUDENT_B = "ananya.joshi2@student.university.edu";
    private static final String ADMIN = "priya.menon@university.edu";

    private static final long CSE_DEPARTMENT_ID = 1L;

    private static final long HOSTEL_DEPARTMENT_ID = 6L;

    private String tokenFor(String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(json(new LoginRequest(email, PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(body).get("accessToken").asText();
    }

    private long submitFeedbackAs(String token) throws Exception {
        String body = mockMvc.perform(post("/api/v1/feedback")
                        .header("Authorization", token)
                        .contentType("application/json")
                        .content(json(new CreateFeedbackRequest(
                                "Lab machines are unusably slow this week",
                                "Every machine in the Block C lab takes ten minutes to boot and freezes constantly.",
                                FeedbackCategory.IT_SUPPORT, CSE_DEPARTMENT_ID, false))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    @Test
    @DisplayName("the server assigns the ticket number, status and SLA - not the client")
    void serverOwnsWorkflowFields() throws Exception {
        String token = tokenFor(STUDENT_A);

        mockMvc.perform(post("/api/v1/feedback")
                        .header("Authorization", token)
                        .contentType("application/json")
                        .content(json(new CreateFeedbackRequest(
                                "Projector in seminar hall two is dead",
                                "It has not worked for four consecutive guest lectures now.",
                                FeedbackCategory.INFRASTRUCTURE, CSE_DEPARTMENT_ID, false))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketNumber").value(org.hamcrest.Matchers.matchesPattern("FB-\\d{4}-\\d{6}")))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.satisfactionRating").doesNotExist());
    }

    @Test
    @DisplayName("a student cannot read another student's ticket")
    void studentsCannotReadEachOthersFeedback() throws Exception {
        long feedbackId = submitFeedbackAs(tokenFor(STUDENT_A));

        mockMvc.perform(get("/api/v1/feedback/{id}", feedbackId)
                        .header("Authorization", tokenFor(STUDENT_B)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("a student's list is scoped to their own submissions")
    void listIsScopedToTheCaller() throws Exception {
        long mine = submitFeedbackAs(tokenFor(STUDENT_A));
        long theirs = submitFeedbackAs(tokenFor(STUDENT_B));

        mockMvc.perform(get("/api/v1/feedback")
                        .header("Authorization", tokenFor(STUDENT_A))
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()))

                .andExpect(jsonPath("$.content[?(@.id == %d)]".formatted(mine)).isNotEmpty())

                .andExpect(jsonPath("$.content[?(@.id == %d)]".formatted(theirs)).isEmpty());
    }

    @Test
    @DisplayName("passing another department's id as a filter does not widen a department admin's scope")
    void departmentFilterCannotEscalate() throws Exception {
        mockMvc.perform(get("/api/v1/feedback")
                        .header("Authorization", tokenFor(ADMIN))
                        .param("departmentId", String.valueOf(HOSTEL_DEPARTMENT_ID))
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.departmentName == 'Hostel Administration')]").isEmpty());
    }

    @Test
    @DisplayName("a student cannot change a ticket's status, even their own")
    void studentsCannotDriveTheWorkflow() throws Exception {
        String token = tokenFor(STUDENT_A);
        long feedbackId = submitFeedbackAs(token);

        mockMvc.perform(patch("/api/v1/feedback/{id}/status", feedbackId)
                        .header("Authorization", token)
                        .contentType("application/json")
                        .content(json(new UpdateFeedbackStatusRequest(FeedbackStatus.RESOLVED, "done"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("an illegal workflow transition is rejected with 422")
    void illegalTransitionIsRejected() throws Exception {
        String studentToken = tokenFor(STUDENT_A);
        String adminToken = tokenFor(ADMIN);
        long feedbackId = submitFeedbackAs(studentToken);

        mockMvc.perform(patch("/api/v1/feedback/{id}/status", feedbackId)
                        .header("Authorization", adminToken)
                        .contentType("application/json")
                        .content(json(new UpdateFeedbackStatusRequest(FeedbackStatus.CLOSED, null))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Unprocessable"));
    }

    @Test
    @DisplayName("a legal transition succeeds and is recorded in the history")
    void legalTransitionIsRecorded() throws Exception {
        String adminToken = tokenFor(ADMIN);
        long feedbackId = submitFeedbackAs(tokenFor(STUDENT_A));

        mockMvc.perform(patch("/api/v1/feedback/{id}/status", feedbackId)
                        .header("Authorization", adminToken)
                        .contentType("application/json")
                        .content(json(new UpdateFeedbackStatusRequest(
                                FeedbackStatus.IN_PROGRESS, "Investigating with the vendor"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.history.length()").value(greaterThan(0)))
                .andExpect(jsonPath("$.history[0].toStatus").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("a student cannot reach the user-administration endpoints")
    void studentsCannotAdministerUsers() throws Exception {
        mockMvc.perform(get("/api/v1/users/students").header("Authorization", tokenFor(STUDENT_A)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("a department admin cannot reach super-admin endpoints")
    void departmentAdminsCannotReachSuperAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/users/admins").header("Authorization", tokenFor(ADMIN)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("submissions are validated before they reach the database")
    void submissionValidationApplies() throws Exception {
        mockMvc.perform(post("/api/v1/feedback")
                        .header("Authorization", tokenFor(STUDENT_A))
                        .contentType("application/json")
                        .content("""
                                {"title":"short","description":"too short","category":"OTHER","departmentId":1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title", notNullValue()))
                .andExpect(jsonPath("$.errors.description", notNullValue()));
    }
}
