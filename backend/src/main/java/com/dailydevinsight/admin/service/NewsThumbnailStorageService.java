package com.dailydevinsight.admin.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
public class NewsThumbnailStorageService {

    private static final int CONNECT_TIMEOUT_MILLIS = 5000;
    private static final int READ_TIMEOUT_MILLIS = 10000;
    private static final int MAX_DOWNLOAD_BYTES = 5 * 1024 * 1024;
    private static final DateTimeFormatter DIRECTORY_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final String userAgent;
    private final Path uploadRootDirectory;

    /**
     * @date 2026-04-17
     * @desc 썸네일 저장 루트 경로와 HTTP User-Agent 값을 초기화합니다.
     */
    public NewsThumbnailStorageService(
            @Value("${crawler.user-agent:DailyDevInsightBot/1.0}") String userAgent,
            @Value("${crawler.thumbnail-upload-dir:./uploads}") String thumbnailUploadDirectory
    ) {
        this.userAgent = userAgent;
        this.uploadRootDirectory = Paths.get(thumbnailUploadDirectory).toAbsolutePath().normalize();
    }

    /**
     * @date 2026-04-17
     * @desc 원격 이미지 URL을 다운로드하여 로컬 파일로 저장하고 서비스 경로를 반환합니다.
     */
    public String downloadAndStoreThumbnail(String imageUrl, LocalDate targetDate) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        try {
            String normalizedUrl = imageUrl.trim();
            String extension = resolveFileExtension(normalizedUrl, null);
            Path targetDirectory = resolveTargetDirectory(targetDate);
            Files.createDirectories(targetDirectory);
            String fileName = UUID.randomUUID() + extension;
            Path targetFile = targetDirectory.resolve(fileName);

            Path savedFile = downloadFile(normalizedUrl, targetFile);
            return buildPublicPath(targetDate, savedFile.getFileName().toString());
        } catch (Exception exception) {
            return null;
        }
    }

    /**
     * @date 2026-04-17
     * @desc 저장 대상 디렉터리 경로(/uploads/news/yyyyMMdd)를 계산합니다.
     */
    private Path resolveTargetDirectory(LocalDate targetDate) {
        String dateDirectory = targetDate.format(DIRECTORY_DATE_FORMATTER);
        return uploadRootDirectory.resolve("news").resolve(dateDirectory);
    }

    /**
     * @date 2026-04-17
     * @desc 저장된 썸네일 파일의 공개 URL 경로를 생성합니다.
     */
    private String buildPublicPath(LocalDate targetDate, String fileName) {
        String dateDirectory = targetDate.format(DIRECTORY_DATE_FORMATTER);
        return "/uploads/news/" + dateDirectory + "/" + fileName;
    }

    /**
     * @date 2026-04-17
     * @desc 원격 파일을 제한 용량 이내로 다운로드하여 지정 경로에 저장합니다.
     */
    private Path downloadFile(String imageUrl, Path targetFile) throws Exception {
        URL url = URI.create(imageUrl).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        connection.setReadTimeout(READ_TIMEOUT_MILLIS);
        connection.setRequestProperty("User-Agent", userAgent);
        connection.connect();

        int responseCode = connection.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            throw new IllegalStateException("이미지 응답 코드가 비정상입니다: " + responseCode);
        }

        String contentType = connection.getContentType();
        String extension = resolveFileExtension(imageUrl, contentType);
        Path finalizedTargetFile = targetFile;
        if (!targetFile.toString().endsWith(extension)) {
            finalizedTargetFile = targetFile.getParent().resolve(targetFile.getFileName().toString() + extension);
        }

        int downloadedSize = 0;
        try (InputStream inputStream = connection.getInputStream();
             OutputStream outputStream = Files.newOutputStream(finalizedTargetFile)) {
            byte[] buffer = new byte[8192];
            int readBytes;
            while ((readBytes = inputStream.read(buffer)) != -1) {
                downloadedSize += readBytes;
                if (downloadedSize > MAX_DOWNLOAD_BYTES) {
                    throw new IllegalStateException("이미지 파일 크기가 제한을 초과했습니다.");
                }
                outputStream.write(buffer, 0, readBytes);
            }
        }
        return finalizedTargetFile;
    }

    /**
     * @date 2026-04-17
     * @desc URL/Content-Type 기반으로 저장할 이미지 확장자를 결정합니다.
     */
    private String resolveFileExtension(String imageUrl, String contentType) {
        String extensionByContentType = resolveExtensionByContentType(contentType);
        if (!extensionByContentType.isBlank()) {
            return extensionByContentType;
        }

        String lowerUrl = imageUrl.toLowerCase(Locale.ROOT);
        if (lowerUrl.endsWith(".png")) {
            return ".png";
        }
        if (lowerUrl.endsWith(".webp")) {
            return ".webp";
        }
        if (lowerUrl.endsWith(".gif")) {
            return ".gif";
        }
        return ".jpg";
    }

    /**
     * @date 2026-04-17
     * @desc Content-Type 문자열을 이미지 파일 확장자로 변환합니다.
     */
    private String resolveExtensionByContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "";
        }
        String normalizedContentType = contentType.toLowerCase(Locale.ROOT);
        if (normalizedContentType.contains("image/png")) {
            return ".png";
        }
        if (normalizedContentType.contains("image/webp")) {
            return ".webp";
        }
        if (normalizedContentType.contains("image/gif")) {
            return ".gif";
        }
        if (normalizedContentType.contains("image/jpeg") || normalizedContentType.contains("image/jpg")) {
            return ".jpg";
        }
        return "";
    }
}
