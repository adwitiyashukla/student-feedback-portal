package com.adwitiya.feedbackportal.security;

import com.adwitiya.feedbackportal.domain.entity.User;
import com.adwitiya.feedbackportal.domain.enums.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public record AppUserDetails(
        Long id,
        String email,
        String passwordHash,
        String fullName,
        Role role,
        Long departmentId,
        boolean enabled,
        boolean locked
) implements UserDetails, Serializable {
    public static AppUserDetails from(User user, Long departmentId) {
        return new AppUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getFullName(),
                user.getRole(),
                departmentId,
                user.isEnabled(),
                user.isCurrentlyLocked());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.authority()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !locked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public boolean isStaff() {
        return role.isStaff();
    }

    public boolean isSuperAdmin() {
        return role == Role.SUPER_ADMIN;
    }
}
