package com.dailydevinsight.admin.service;

import com.dailydevinsight.entity.TechNews;
import com.dailydevinsight.repository.TechNewsRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TechNewsPersistenceServiceTest {

    /**
     * @date 2026-08-10
     * @desc 기존 URL과 동일 실행 내 중복 URL을 제거하고 다음 ID부터 저장하는지 검증합니다.
     */
    @Test
    void persistArticles_ShouldRemoveDuplicatesAndAssignIds() {
        TechNewsRepository techNewsRepository = mock(TechNewsRepository.class);
        TechNewsPersistenceService service = new TechNewsPersistenceService(techNewsRepository);
        LocalDate targetDate = LocalDate.of(2026, 8, 10);
        NewsArticleData existingArticle = createArticle("https://example.com/existing", "기존 기사");
        NewsArticleData newArticle = createArticle("https://example.com/new", "신규 기사");

        when(techNewsRepository.findByUrlIn(any())).thenReturn(List.of(
                TechNews.builder().id(40L).url(existingArticle.getUrl()).build()
        ));
        when(techNewsRepository.findTopByOrderByIdDesc()).thenReturn(Optional.of(
                TechNews.builder().id(40L).build()
        ));

        List<TechNews> savedArticles = service.persistArticles(
                targetDate,
                "기본 소스",
                List.of(
                        new EnrichedArticle(existingArticle, "/thumbnail/existing.jpg"),
                        new EnrichedArticle(newArticle, "/thumbnail/new.jpg"),
                        new EnrichedArticle(newArticle, "/thumbnail/duplicate.jpg")
                ),
                false
        );

        assertEquals(1, savedArticles.size());
        TechNews savedArticle = savedArticles.get(0);
        assertEquals(41L, savedArticle.getId());
        assertEquals(targetDate, savedArticle.getNewsDate());
        assertEquals("신규 기사", savedArticle.getTitle());
        assertEquals("/thumbnail/new.jpg", savedArticle.getAttachmentImagePath());

        ArgumentCaptor<List<TechNews>> savedArticlesCaptor = ArgumentCaptor.forClass(List.class);
        verify(techNewsRepository).saveAll(savedArticlesCaptor.capture());
        assertEquals(savedArticles, savedArticlesCaptor.getValue());
    }

    /**
     * @date 2026-08-10
     * @desc 저장 중 예외가 발생하면 트랜잭션이 커밋되지 않고 롤백되는지 검증합니다.
     */
    @Test
    void persistArticles_ShouldRollbackWhenSaveFails() {
        TechNewsRepository techNewsRepository = mock(TechNewsRepository.class);
        TechNewsPersistenceService targetService = new TechNewsPersistenceService(techNewsRepository);
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        TransactionStatus transactionStatus = mock(TransactionStatus.class);
        when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(transactionStatus);

        TransactionInterceptor transactionInterceptor = new TransactionInterceptor(
                (TransactionManager) transactionManager,
                new AnnotationTransactionAttributeSource()
        );
        ProxyFactory proxyFactory = new ProxyFactory(targetService);
        proxyFactory.addAdvice(transactionInterceptor);
        TechNewsPersistenceService service = (TechNewsPersistenceService) proxyFactory.getProxy();

        when(techNewsRepository.findByUrlIn(any())).thenReturn(List.of());
        when(techNewsRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        when(techNewsRepository.saveAll(any())).thenThrow(new IllegalStateException("저장 실패"));

        assertThrows(
                IllegalStateException.class,
                () -> service.persistArticles(
                        LocalDate.of(2026, 8, 10),
                        "테스트 소스",
                        List.of(new EnrichedArticle(createArticle("https://example.com/new", "신규 기사"), null)),
                        false
                )
        );

        verify(transactionManager).rollback(transactionStatus);
        verify(transactionManager, never()).commit(transactionStatus);
    }

    /**
     * @date 2026-08-10
     * @desc 저장 테스트용 수집 기사 데이터를 생성합니다.
     */
    private NewsArticleData createArticle(String url, String title) {
        return NewsArticleData.builder()
                .sourceName("테스트 소스")
                .title(title)
                .url(url)
                .summary("테스트 요약")
                .content("테스트 본문")
                .imageUrl("https://example.com/image.jpg")
                .build();
    }
}
