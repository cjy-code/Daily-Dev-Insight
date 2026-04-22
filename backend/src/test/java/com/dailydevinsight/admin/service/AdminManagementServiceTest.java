package com.dailydevinsight.admin.service;

import com.dailydevinsight.admin.repository.GenerationHistoryRepository;
import com.dailydevinsight.entity.DailyKnowledge;
import com.dailydevinsight.repository.DailyKnowledgeRepository;
import com.dailydevinsight.repository.InsightBookmarkRepository;
import com.dailydevinsight.repository.TechNewsRepository;
import com.dailydevinsight.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminManagementServiceTest {

    @TempDir
    Path tempDirectory;

    private DailyKnowledgeRepository dailyKnowledgeRepository;
    private TechNewsRepository techNewsRepository;
    private UserRepository userRepository;
    private GenerationHistoryRepository generationHistoryRepository;
    private InsightBookmarkRepository insightBookmarkRepository;
    private AdminManagementService adminManagementService;

    @BeforeEach
    void setUp() {
        dailyKnowledgeRepository = mock(DailyKnowledgeRepository.class);
        techNewsRepository = mock(TechNewsRepository.class);
        userRepository = mock(UserRepository.class);
        generationHistoryRepository = mock(GenerationHistoryRepository.class);
        insightBookmarkRepository = mock(InsightBookmarkRepository.class);

        adminManagementService = new AdminManagementService(
                dailyKnowledgeRepository,
                techNewsRepository,
                userRepository,
                generationHistoryRepository,
                insightBookmarkRepository
        );
        ReflectionTestUtils.setField(
                adminManagementService,
                "thumbnailUploadDirectory",
                tempDirectory.resolve("uploads").toString()
        );
    }

    /**
     * @date 2026-04-22
     * @desc 일일 지식 썸네일 업로드 시 공개 경로가 저장되는지 검증합니다.
     */
    @Test
    void updateKnowledgeThumbnail_ShouldSavePublicPath() {
        DailyKnowledge originalPost = createKnowledge(24L, "/uploads/knowledge/20260421/old.png");
        when(dailyKnowledgeRepository.findById(24L)).thenReturn(Optional.of(originalPost));

        MockMultipartFile thumbnailFile = new MockMultipartFile(
                "thumbnailFile",
                "new.png",
                "image/png",
                "new-image-content".getBytes()
        );

        adminManagementService.updateKnowledgeThumbnail(24L, thumbnailFile);

        ArgumentCaptor<DailyKnowledge> captor = ArgumentCaptor.forClass(DailyKnowledge.class);
        verify(dailyKnowledgeRepository).save(captor.capture());
        String storedPath = captor.getValue().getAttachmentImagePath();
        assertTrue(storedPath.startsWith("/uploads/knowledge/"));
        assertTrue(storedPath.endsWith(".png"));
    }

    /**
     * @date 2026-04-22
     * @desc 허용되지 않은 확장자 업로드 요청을 차단하는지 검증합니다.
     */
    @Test
    void updateKnowledgeThumbnail_ShouldRejectInvalidExtension() {
        DailyKnowledge originalPost = createKnowledge(24L, null);
        when(dailyKnowledgeRepository.findById(24L)).thenReturn(Optional.of(originalPost));

        MockMultipartFile thumbnailFile = new MockMultipartFile(
                "thumbnailFile",
                "bad.txt",
                "text/plain",
                "bad-content".getBytes()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> adminManagementService.updateKnowledgeThumbnail(24L, thumbnailFile)
        );
    }

    /**
     * @date 2026-04-22
     * @desc 썸네일 삭제 시 DB null 처리와 파일 삭제 시도 로직이 동작하는지 검증합니다.
     */
    @Test
    void deleteKnowledgeThumbnail_ShouldDeleteFileAndSaveNullPath() throws Exception {
        Path uploadRoot = tempDirectory.resolve("uploads");
        Path existingFile = uploadRoot.resolve("knowledge/20260422/existing.png");
        Files.createDirectories(existingFile.getParent());
        Files.writeString(existingFile, "existing");

        DailyKnowledge originalPost = createKnowledge(24L, "/uploads/knowledge/20260422/existing.png");
        when(dailyKnowledgeRepository.findById(24L)).thenReturn(Optional.of(originalPost));

        assertDoesNotThrow(() -> adminManagementService.deleteKnowledgeThumbnail(24L));

        ArgumentCaptor<DailyKnowledge> captor = ArgumentCaptor.forClass(DailyKnowledge.class);
        verify(dailyKnowledgeRepository).save(captor.capture());
        assertTrue(captor.getValue().getAttachmentImagePath() == null);
        assertTrue(Files.notExists(existingFile));
    }

    /**
     * @date 2026-04-22
     * @desc 테스트용 일일 지식 엔티티를 생성합니다.
     */
    private DailyKnowledge createKnowledge(Long id, String attachmentImagePath) {
        return DailyKnowledge.builder()
                .id(id)
                .knowledgeDate(LocalDate.of(2026, 4, 22))
                .category("Backend")
                .title("테스트")
                .attachmentImagePath(attachmentImagePath)
                .summary("요약")
                .detail("상세")
                .viewCount(0L)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
