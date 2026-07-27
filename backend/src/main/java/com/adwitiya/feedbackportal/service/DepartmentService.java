package com.adwitiya.feedbackportal.service;

import com.adwitiya.feedbackportal.config.CacheConfig;
import com.adwitiya.feedbackportal.domain.entity.Department;
import com.adwitiya.feedbackportal.exception.DuplicateResourceException;
import com.adwitiya.feedbackportal.exception.ResourceNotFoundException;
import com.adwitiya.feedbackportal.repository.DepartmentRepository;
import com.adwitiya.feedbackportal.web.dto.request.CreateDepartmentRequest;
import com.adwitiya.feedbackportal.web.dto.response.DepartmentResponse;
import com.adwitiya.feedbackportal.web.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Department reference data. Read-heavy and rarely written, so it is cached. */
@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserMapper mapper;

    @Cacheable(CacheConfig.CACHE_DEPARTMENTS)
    @Transactional(readOnly = true)
    public List<DepartmentResponse> listActive() {
        return departmentRepository.findByActiveTrueOrderByNameAsc()
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> listAll() {
        return departmentRepository.findAll().stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public DepartmentResponse get(Long id) {
        return departmentRepository.findById(id).map(mapper::toResponse)
                .orElseThrow(() -> ResourceNotFoundException.of("Department", id));
    }

    @CacheEvict(value = CacheConfig.CACHE_DEPARTMENTS, allEntries = true)
    @Transactional
    public DepartmentResponse create(CreateDepartmentRequest request) {
        if (departmentRepository.existsByCodeIgnoreCase(request.code())) {
            throw DuplicateResourceException.of("Department", "code", request.code());
        }
        Department department = departmentRepository.save(Department.builder()
                .code(request.code().trim().toUpperCase())
                .name(request.name().trim())
                .description(request.description())
                .active(true)
                .build());
        return mapper.toResponse(department);
    }

    @CacheEvict(value = CacheConfig.CACHE_DEPARTMENTS, allEntries = true)
    @Transactional
    public DepartmentResponse setActive(Long id, boolean active) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Department", id));
        department.setActive(active);
        return mapper.toResponse(departmentRepository.save(department));
    }
}
