
package com.laf.cobabe_code_lms.services;

import com.laf.cobabe_code_lms.models.Teacher;
import com.laf.cobabe_code_lms.models.TeacherPasswordResetToken;
import com.laf.cobabe_code_lms.repositories.TeacherPasswordResetTokens;
import com.laf.cobabe_code_lms.repositories.Teachers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

@Service
public class TeacherPasswordResetService {

    private final TeacherPasswordResetTokens tokens;
    private final Teachers teachers;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${cobabe.public-base-url:http://localhost:8080}")
    private String publicBaseUrl;

    public TeacherPasswordResetService(
            TeacherPasswordResetTokens tokens,
            Teachers teachers,
            EmailService emailService,
            PasswordEncoder passwordEncoder) {

        this.tokens = tokens;
        this.teachers = teachers;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    // Request a password reset email.
    // Returns without revealing whether the address exists.
    public void requestPasswordReset(String emailAddress) {

        if (emailAddress == null || emailAddress.isBlank()) {
            return;
        }

        String normalizedEmail =
                emailAddress.trim().toLowerCase(Locale.ROOT);

        Optional<Teacher> matchingTeacher = teachers.findAll()
                .stream()
                .filter(teacher ->
                        teacher.recoveryEmail != null
                                && teacher.recoveryEmailVerified
                                && teacher.recoveryEmail.trim()
                                .equalsIgnoreCase(normalizedEmail))
                .findFirst();

        if (matchingTeacher.isEmpty()) {
            return;
        }

        Teacher teacher = matchingTeacher.get();

        // Generate a secure random token.
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        TeacherPasswordResetToken record =
                new TeacherPasswordResetToken();

        record.teacher = teacher;
        record.tokenHash = hashToken(rawToken);
        record.createdAt = Instant.now();
        record.expiresAt = record.createdAt.plus(
                15, ChronoUnit.MINUTES);
        record.used = false;

        tokens.save(record);

        String resetLink = publicBaseUrl
                + "/teacher/reset-password?token="
                + rawToken;

        String message = """
                A password reset was requested for your
                Cobabe Code LMS teacher account.

                Click the link below to reset your password:

                %s

                This link expires in 15 minutes and can
                only be used once.

                If you did not request this reset,
                you can safely ignore this email.
                """.formatted(resetLink);

        emailService.sendEmail(
                teacher.recoveryEmail,
                "Cobabe Code LMS - Password Reset",
                message);
    }

    // Check whether a reset token is valid.
    @Transactional(readOnly = true)
    public boolean isValidToken(String rawToken) {

        return findValidToken(rawToken).isPresent();
    }

    // Change the password and consume the token.
    @Transactional
    public boolean resetPassword(
            String rawToken,
            String newPassword) {

        if (newPassword == null
                || newPassword.isBlank()) {
            return false;
        }

        Optional<TeacherPasswordResetToken> matchingToken =
                findValidToken(rawToken);

        if (matchingToken.isEmpty()) {
            return false;
        }

        TeacherPasswordResetToken record =
                matchingToken.get();

        Teacher teacher = record.teacher;

        // Do not allow a reset if the recovery
        // email is no longer verified.
        if (!teacher.recoveryEmailVerified
                || teacher.recoveryEmail == null) {
            return false;
        }

        teacher.passwordHash =
                passwordEncoder.encode(newPassword);

        record.used = true;

        teachers.save(teacher);
        tokens.save(record);

        return true;
    }

    private Optional<TeacherPasswordResetToken> findValidToken(
            String rawToken) {

        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }

        // Reject malformed tokens.
        if (!rawToken.matches("[A-Za-z0-9_-]{43}")) {
            return Optional.empty();
        }

        return tokens.findByTokenHash(hashToken(rawToken))
                .filter(record -> !record.used)
                .filter(record ->
                        record.expiresAt.isAfter(Instant.now()));
    }

    private String hashToken(String rawToken) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    rawToken.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Unable to hash password reset token", e);
        }
    }
}
