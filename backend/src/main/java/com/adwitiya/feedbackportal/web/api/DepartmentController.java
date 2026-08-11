package com.adwitiya.feedbackportal.web.api;

import com.adwitiya.feedbackportal.service.DepartmentService;
import com.adwitiya.feedbackportal.web.dto.request.CreateDepartmentRequest;
import com.adwitiya.feedbackportal.web.dto.response.DepartmentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Departments", description = "Routing destinations for feedback")
@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
public class DepartmentController {
    private final DepartmentService departmentService;

    @Operation(summary = "List active departments")
    @GetMapping
    public List<DepartmentResponse> listActive() {
        return departmentService.listActive();
    }

    @Operation(summary = "List all departments including inactive ones")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/all")
    public List<DepartmentResponse> listAll() {
        return departmentService.listAll();
    }

    @Operation(summary = "Read one department")
    @GetMapping("/{id}")
    public DepartmentResponse get(@PathVariable Long id) {
        return departmentService.get(id);
    }

    @Operation(summary = "Create a department")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping
    public ResponseEntity<DepartmentResponse> create(@Valid @RequestBody CreateDepartmentRequest request) {
        return ResponseEntity.status(201).body(departmentService.create(request));
    }

    @Operation(summary = "Activate or retire a department")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PatchMapping("/{id}/active")
    public DepartmentResponse setActive(@PathVariable Long id, @RequestParam boolean active) {
        return departmentService.setActive(id, active);
    }
}
