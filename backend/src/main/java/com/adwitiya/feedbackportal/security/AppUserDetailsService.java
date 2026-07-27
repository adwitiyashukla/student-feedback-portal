package com.adwitiya.feedbackportal.security;

import com.adwitiya.feedbackportal.domain.entity.User;
import com.adwitiya.feedbackportal.repository.AdminRepository;
import com.adwitiya.feedbackportal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads a principal by email for both the JWT filter and form login.
 */
@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("No account for " + email));

        Long departmentId = user.getRole().isStaff()
                ? adminRepository.findByUserId(user.getId())
                        .map(admin -> admin.getDepartment().getId())
                        .orElse(null)
                : null;

        return AppUserDetails.from(user, departmentId);
    }
}
