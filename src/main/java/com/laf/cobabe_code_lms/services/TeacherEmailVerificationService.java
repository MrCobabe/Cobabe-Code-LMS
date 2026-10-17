
package com.laf.cobabe_code_lms.services;

import com.laf.cobabe_code_lms.models.Teacher;
import com.laf.cobabe_code_lms.models.TeacherEmailVerificationToken;
import com.laf.cobabe_code_lms.repositories.TeacherEmailVerificationTokens;
import com.laf.cobabe_code_lms.repositories.Teachers;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class TeacherEmailVerificationService {

    private final TeacherEmailVerificationTokens tokens;
    private final Teachers teachers;
    private final EmailService emailService;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${cobabe.public-base-url:http://localhost:8080}")
    private String publicBaseUrl;

    public TeacherEmailVerificationService(
            TeacherEmailVerificationTokens tokens,
            Teachers teachers,
            EmailService emailService) {

        this.tokens = tokens;
        this.teachers = teachers;
        this.emailService = emailService;
    }

    @Transactional
    public void sendVerificationEmail(Teacher teacher) {

        if (teacher.recoveryEmail == null ||
                teacher.recoveryEmail.isBlank()) {
            throw new IllegalArgumentException(
                    "Teacher has no recovery email configured.");
        }

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        TeacherEmailVerificationToken record =
                new TeacherEmailVerificationToken();

        record.teacher = teacher;
        record.emailAddress = teacher.recoveryEmail;
        record.tokenHash = hashToken(rawToken);
        record.createdAt = Instant.now();
        record.expiresAt = record.createdAt.plus(
                15, ChronoUnit.MINUTES);
        record.used = false;

        tokens.save(record);
        Teacher savedTeacher = teachers.findById(teacher.id)
                .orElseThrow();



        String verificationLink =
                publicBaseUrl.replaceAll("/+$", "")
                        + "/teacher/verify-email?token=" + rawToken;

        emailService.sendEmail(
                teacher.recoveryEmail,
                "Cobabe Code LMS - Verify Recovery Email",
                "Please verify your recovery email by visiting:\n\n"
                        + verificationLink
                        + "\n\nThis link expires in 15 minutes."
                        + "\nIf you did not request this, ignore this email."
        );
    }

    @Transactional
    public boolean verifyToken(String rawToken) {

        if (rawToken == null || rawToken.isBlank()) {
            return false;
        }

        String tokenHash = hashToken(rawToken);

        var optionalRecord = tokens.findByTokenHash(tokenHash);

        if (optionalRecord.isEmpty()) {
            System.out.println("Verification failed: token not found");
            return false;
        }

        TeacherEmailVerificationToken record =
                optionalRecord.get();



        if (record.used ||
                !record.expiresAt.isAfter(Instant.now())) {
            System.out.println("Verification failed: expired or used token");
            return false;
        }

        Long teacherId = record.teacher.id;

        Teacher teacher = teachers.findById(teacherId)
                .orElseThrow();

        if (teacher.recoveryEmail == null ||
                !teacher.recoveryEmail.equalsIgnoreCase(record.emailAddress)) {
            return false;
        }

        record.used = true;
        teacher.recoveryEmailVerified = true;

        tokens.save(record);
        teachers.save(teacher);

        return true;
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
                    "Unable to hash verification token", e);
        }
    }
}
