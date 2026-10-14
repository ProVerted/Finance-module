package com.provert.payroll.security.service;

import com.provert.payroll.common.exception.BadRequestException;
import com.provert.payroll.common.exception.ConflictException;
import com.provert.payroll.security.dto.AuthRequest;
import com.provert.payroll.security.dto.AuthResponse;
import com.provert.payroll.security.dto.RegisterRequest;
import com.provert.payroll.security.entity.Role;
import com.provert.payroll.security.entity.UserAccount;
import com.provert.payroll.security.repository.UserAccountRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userAccountRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username is already taken: " + request.getUsername(), "USERNAME_TAKEN");
        }

        UserAccount user = new UserAccount();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setEnabled(true);
        user.setActive(true);

        UserAccount saved = userAccountRepository.save(user);
        String token = jwtService.generateToken(saved.getUsername(), saved.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .username(saved.getUsername())
                .role(saved.getRole().name())
                .build();
    }

    public AuthResponse login(AuthRequest request) {
        UserAccount user = userAccountRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadRequestException("Invalid username or password", "INVALID_CREDENTIALS"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException("Invalid username or password", "INVALID_CREDENTIALS");
        }

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new BadRequestException("Account is disabled", "ACCOUNT_DISABLED");
        }

        String token = jwtService.generateToken(user.getUsername(), user.getRole().name());

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();
    }

    @PostConstruct
    public void seedInitialUsers() {
        if (userAccountRepository.count() == 0) {
            seedUser("finance_mgr", "password123", Role.ROLE_FINANCE_MANAGER);
            seedUser("hr_admin", "password123", Role.ROLE_HR_ADMIN);
            seedUser("employee1", "password123", Role.ROLE_EMPLOYEE);
        }
    }

    private void seedUser(String username, String rawPassword, Role role) {
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setEnabled(true);
        user.setActive(true);
        userAccountRepository.save(user);
    }
}
