
package com.laf.cobabe_code_lms.services;

import com.laf.cobabe_code_lms.models.ShowcaseProject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.util.Locale;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class ShowcaseFileService {

    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    private static final long MAX_JAR_BYTES = 500L * 1024 * 1024;

    private final JdbcTemplate jdbc;

    public ShowcaseFileService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public void saveFile(
            ShowcaseProject project,
            String fileType,
            MultipartFile upload
    ) throws IOException {

        if (upload == null || upload.isEmpty()) {
            return;
        }

        if (project == null || project.id == null) {
            throw new IllegalArgumentException("Project must be saved first");
        }

        if (!"IMAGE".equals(fileType) && !"JAR".equals(fileType)) {
            throw new IllegalArgumentException("Unsupported file type");
        }

        long limit = "IMAGE".equals(fileType)
                ? MAX_IMAGE_BYTES
                : MAX_JAR_BYTES;

        if (upload.getSize() > limit) {
            throw new IllegalArgumentException("File exceeds size limit");
        }

        String originalName = upload.getOriginalFilename();

        if (originalName == null) {
            throw new IllegalArgumentException("Missing filename");
        }

        String normalizedName = originalName.replace('\\', '/');

        final String filename =
                normalizedName.substring(normalizedName.lastIndexOf('/') + 1);

        if (filename.isBlank() || filename.length() > 255) {
            throw new IllegalArgumentException("Invalid filename");
        }

        String lower = filename.toLowerCase(Locale.ROOT);

        String contentType;

        if ("IMAGE".equals(fileType)) {

            if (!(lower.endsWith(".png")
                    || lower.endsWith(".jpg")
                    || lower.endsWith(".jpeg")
                    || lower.endsWith(".webp"))) {
                throw new IllegalArgumentException(
                        "Screenshot must be PNG, JPG, or WebP");
            }

            contentType = detectImageType(upload);

        } else {

            if (!lower.endsWith(".jar")) {
                throw new IllegalArgumentException(
                        "Application must have a .jar extension");
            }

            validateJar(upload);
            contentType = "application/java-archive";
        }

        // Remove the previous file of this type, if present.
        jdbc.update(
                "DELETE FROM showcase_project_files " +
                        "WHERE project_id = ? AND file_type = ?",
                project.id, fileType
        );

        String fileId = UUID.randomUUID().toString();

        // Insert using a JDBC binary stream.
        // No upload.getBytes() call.
        try (InputStream stream = upload.getInputStream()) {

            jdbc.update(connection -> {

                PreparedStatement statement =
                        connection.prepareStatement(
                                "INSERT INTO showcase_project_files " +
                                        "(project_id, file_type, original_filename, " +
                                        "content_type, storage_type, external_path, " +
                                        "file_data, file_size) " +
                                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
                        );

                statement.setLong(1, project.id);
                statement.setString(2, fileType);
                statement.setString(3, filename);
                statement.setString(4, contentType);
                statement.setString(5, "DATABASE");
                statement.setString(6, null);
                statement.setBinaryStream(7, stream, upload.getSize());
                statement.setLong(8, upload.getSize());

                return statement;
            });
        }
    }

    @Transactional
    public void deleteFiles(Long projectId) {
        jdbc.update(
                "DELETE FROM showcase_project_files WHERE project_id = ?",
                projectId
        );
    }

    private String detectImageType(MultipartFile upload)
            throws IOException {

        try (InputStream stream = upload.getInputStream()) {

            byte[] header = stream.readNBytes(12);

            if (header.length >= 8 &&
                    (header[0] & 0xff) == 0x89 &&
                    header[1] == 'P' &&
                    header[2] == 'N' &&
                    header[3] == 'G') {
                return "image/png";
            }

            if (header.length >= 3 &&
                    (header[0] & 0xff) == 0xff &&
                    (header[1] & 0xff) == 0xd8 &&
                    (header[2] & 0xff) == 0xff) {
                return "image/jpeg";
            }

            if (header.length >= 12 &&
                    new String(header, 0, 4,
                            StandardCharsets.US_ASCII).equals("RIFF") &&
                    new String(header, 8, 4,
                            StandardCharsets.US_ASCII).equals("WEBP")) {
                return "image/webp";
            }
        }

        throw new IllegalArgumentException("Invalid screenshot format");
    }

    private void validateJar(MultipartFile upload)
            throws IOException {

        try (InputStream stream = upload.getInputStream()) {

            byte[] header = stream.readNBytes(4);

            if (header.length < 4 ||
                    header[0] != 'P' ||
                    header[1] != 'K' ||
                    header[2] != 3 ||
                    header[3] != 4) {

                throw new IllegalArgumentException(
                        "Invalid JAR archive");
            }
        }
    }

    public boolean hasFile(Long projectId, String fileType) {

        Integer count = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM showcase_project_files
                WHERE project_id = ?
                  AND file_type = ?
                """,
                Integer.class,
                projectId,
                fileType
        );

        return count != null && count > 0;
    }

}
