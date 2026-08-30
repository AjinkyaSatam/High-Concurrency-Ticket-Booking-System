package com.ticketbooking.backend.service;

import com.ticketbooking.backend.dto.AuthResponse;
import com.ticketbooking.backend.dto.LoginRequest;
import com.ticketbooking.backend.dto.RegisterRequest;
import com.ticketbooking.backend.dto.UserResponse;
import com.ticketbooking.backend.entity.Role;
import com.ticketbooking.backend.entity.User;
import com.ticketbooking.backend.repository.RoleRepository;
import com.ticketbooking.backend.repository.UserRepository;
import com.ticketbooking.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("User with email already exists");
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
            .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_USER").build()));

        User user = User.builder()
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .fullName(request.getFullName())
            .roles(Set.of(userRole))
            .build();

        User savedUser = userRepository.save(user);
        String jwtToken = jwtService.generateToken(savedUser);

        Set<String> roleNames = savedUser.getRoles().stream()
            .map(Role::getName)
            .collect(Collectors.toSet());

        return AuthResponse.builder()
            .token(jwtToken)
            .id(savedUser.getId())
            .email(savedUser.getEmail())
            .fullName(savedUser.getFullName())
            .roles(roleNames)
            .build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        String jwtToken = jwtService.generateToken(user);

        Set<String> roleNames = user.getRoles().stream()
            .map(Role::getName)
            .collect(Collectors.toSet());

        return AuthResponse.builder()
            .token(jwtToken)
            .id(user.getId())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .roles(roleNames)
            .build();
    }

    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Set<String> roleNames = user.getRoles().stream()
            .map(Role::getName)
            .collect(Collectors.toSet());

        return UserResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .roles(roleNames)
            .build();
    }

    public AuthResponse refreshToken(com.ticketbooking.backend.dto.RefreshTokenRequest request) {
        String token = request.getRefreshToken();
        String userEmail = jwtService.extractUsername(token);
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (!jwtService.isTokenValid(token, user)) {
            throw new IllegalArgumentException("Expired or invalid refresh token");
        }

        String newAccessToken = jwtService.generateToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        Set<String> roleNames = user.getRoles().stream()
            .map(Role::getName)
            .collect(Collectors.toSet());

        return AuthResponse.builder()
            .token(newAccessToken)
            .id(user.getId())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .roles(roleNames)
            .build();
    }
}
