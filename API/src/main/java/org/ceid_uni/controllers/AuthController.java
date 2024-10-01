package org.ceid_uni.controllers;

import io.swagger.v3.oas.annotations.Operation;
import org.ceid_uni.dto.request.*;
import org.ceid_uni.dto.response.ProxmoxLoginResponse;
import org.ceid_uni.exception.ApplicationException;
import org.ceid_uni.exception.InvalidRoleException;
import org.ceid_uni.models.*;
import org.ceid_uni.dto.response.MessageResponse;
import org.ceid_uni.repository.RoleRepository;
import org.ceid_uni.repository.UserRepository;
import org.ceid_uni.security.auth.PromoxUtils;
import org.ceid_uni.security.services.*;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private AuthenticationManager authenticationManager;

    private UserRepository userRepository;

    private RoleRepository roleRepository;

    private PasswordEncoder encoder;

    private PromoxUtils promoxUtils;

    private IResetPasswordService resetPasswordService;

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @PostMapping("/signin")
    @Operation(summary = "User Sign in",
            description = "Retrieve the CSRF prevention token and ticket from the response, then add them in the headers of all subsequent requests to Promox.")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        ProxmoxLoginResponse promoxTicket = promoxUtils.verifyPromoxTicket(loginRequest.getUsername(), loginRequest.getPassword());
        if (promoxTicket != null) {
            logger.info("Valid proxmox ticket received from cluster for username {}", loginRequest.getUsername());
            Authentication authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            List<String> roles = userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            promoxTicket.setRoles(roles);

            return ResponseEntity.ok(promoxTicket);

        }
        logger.warn("No ticket received from proxmox cluster for username {}", loginRequest.getUsername());
        throw new ApplicationException("Username was not found in Proxmox!");
    }

    @PostMapping("/signup")
    @Operation(summary = "Register a user",
            description = "Ensure that the provided username exists within the system, and provide the corresponding credentials for authentication")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            throw new ApplicationException("Username is already taken!");
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new ApplicationException("Email is already in use!");
        }

        if (promoxUtils.verifyPromoxTicket(signUpRequest.getUsername(), signUpRequest.getPassword()) == null) {
            throw new ApplicationException("User was not found in promox!");
        }
        logger.info("Registration request has passed all validations successfully for username {}", signUpRequest.getUsername());
        User user = new User(signUpRequest.getUsername(), signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword()));

        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName(ERole.ROLE_USER).orElseThrow(() -> new InvalidRoleException("Error: Role is not found."));
        roles.add(userRole);
        user.setRoles(roles);
        userRepository.save(user);
        logger.info("Role user added by default. Please add manually to this user {} any super privileged roles.", signUpRequest.getUsername());

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    @PostMapping("/resetpassword")
    @Operation(summary = "Reset the password",
            description = "You will receive an email containing a reset token.")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        String email = request.getEmail();
        if (userRepository.existsByEmail(email)) {
            Optional<User> userOptional = userRepository.findByEmail(email);
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                resetPasswordService.sendMail(user);
                logger.info("A mail with a reset token was sent to user {}", user.getUsername());
            }
            return ResponseEntity.ok(new MessageResponse("Mail was sent successfully!"));
        }

        throw new ApplicationException("User with email " + email + " not found.");

    }

    @PostMapping("/changepassword")
    @Operation(summary = "Change the password",
            description = "Ensure that the password matches with the password in promox.")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ChangePasswordRequest request) {
        String token = request.getToken();
        if (resetPasswordService.existsByToken(token)) {
            logger.info("Valid token found inside the database.");
            Optional<ResetPassword> passwordOptional = resetPasswordService.findByToken(token);
            if (passwordOptional.isPresent()) {
                ResetPassword password = passwordOptional.get();
                User user = password.getUser();
                if (promoxUtils.verifyPromoxTicket(user.getUsername(), request.getNewPassword()) == null) {
                    throw new ApplicationException("Error: Current password is not aligned with the current password in promox!");
                }
                resetPasswordService.changePassword(user, encoder.encode(request.getNewPassword()));
                logger.info("Password was changed successfully for username {}", user.getUsername());
                return ResponseEntity.ok(new MessageResponse("Password was changed successfully!"));
            }
        }
        throw new ApplicationException("Password with token " + token + " not found.");
    }

    @DeleteMapping("/signout")
    @Operation(summary = "Sign out",
            description = "Does nothing because the expiration of a ticket takes place in promox.")
    public ResponseEntity<?> logoutUser() {
        return ResponseEntity.ok(new MessageResponse("Log out successful!"));
    }

    @Autowired
    public void setAuthenticationManager(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Autowired
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Autowired
    public void setRoleRepository(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Autowired
    public void setEncoder(PasswordEncoder encoder) {
        this.encoder = encoder;
    }

    @Autowired
    public void setResetPasswordService(ResetPasswordService resetPasswordService) {
        this.resetPasswordService = resetPasswordService;
    }

    @Autowired
    public void setPromoxUtils(PromoxUtils promoxUtils) {
        this.promoxUtils = promoxUtils;
    }

}
