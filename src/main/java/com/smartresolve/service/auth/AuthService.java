package com.smartresolve.service.auth;

import com.smartresolve.dto.request.LoginRequest;
import com.smartresolve.dto.request.RegisterRequest;
import com.smartresolve.dto.response.AuthResponse;
import com.smartresolve.entity.SupportTeam;
import com.smartresolve.entity.User;
import com.smartresolve.enums.Role;
import com.smartresolve.repository.SupportTeamRepository;
import com.smartresolve.repository.UserRepository;
import com.smartresolve.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final SupportTeamRepository supportTeamRepository;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            SupportTeamRepository supportTeamRepository
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.supportTeamRepository = supportTeamRepository;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "An account with this email already exists"
            );
        }

        Role role = request.role() == null ? Role.EMPLOYEE : request.role();
        SupportTeam team = resolveTeam(role, request.teamId());

        User user = User.builder()
                .fullName(request.fullName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .role(role)
                .team(team)
                .active(true)
                .build();

        User savedUser = userRepository.save(user);

        String token = jwtService.generateToken(
                org.springframework.security.core.userdetails.User
                        .withUsername(savedUser.getEmail())
                        .password(savedUser.getPassword())
                        .roles(savedUser.getRole().name())
                        .build()
        );

        return new AuthResponse(
                token,
                "Bearer",
                savedUser.getId(),
                savedUser.getFullName(),
                savedUser.getEmail(),
                savedUser.getRole(),
                savedUser.getTeam() == null ? null : savedUser.getTeam().getId(),
                savedUser.getTeam() == null ? null : savedUser.getTeam().getTeamName()
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {

        String email = request.email().trim().toLowerCase();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        email,
                        request.password()
                )
        );

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found")
                );

        var userDetails =
                org.springframework.security.core.userdetails.User
                        .withUsername(user.getEmail())
                        .password(user.getPassword())
                        .roles(user.getRole().name())
                        .build();

        String token = jwtService.generateToken(userDetails);

        return new AuthResponse(
                token,
                "Bearer",
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getTeam() == null ? null : user.getTeam().getId(),
                user.getTeam() == null ? null : user.getTeam().getTeamName()
        );
    }

    private SupportTeam resolveTeam(Role role, java.util.UUID teamId) {
        if (role != Role.SUPPORT_ENGINEER) {
            if (teamId != null) {
                throw new IllegalArgumentException(
                        "Only support engineers can be assigned to a team");
            }
            return null;
        }

        if (teamId == null) {
            throw new IllegalArgumentException(
                    "teamId is required when role is SUPPORT_ENGINEER");
        }

        SupportTeam team = supportTeamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Support team not found"));
        if (!team.isActive()) {
            throw new IllegalArgumentException(
                    "Support team is inactive");
        }
        return team;
    }
}
