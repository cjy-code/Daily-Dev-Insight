-- date: 2026-04-15
-- desc: tech_news attachment_image_path column migration + detailed dummy data seed (oracle)

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*)
      INTO v_count
      FROM user_tab_columns
     WHERE table_name = 'TECH_NEWS'
       AND column_name = 'ATTACHMENT_IMAGE_PATH';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE tech_news ADD (attachment_image_path VARCHAR2(500))';
    END IF;
END;
/

MERGE INTO tech_news target
USING (
    SELECT 12001 AS id,
           TRUNC(SYSDATE) AS news_date,
           'Redis Labs' AS source,
           'Redis 8 RC 공개: 벡터 검색·JSON·스트림 처리 성능 대폭 개선' AS title,
           'https://redis.io/blog/' AS url,
           '/images/news/redis-logo.svg' AS attachment_image_path,
           q'~Redis 8 릴리스 후보에서는 벡터 검색 인덱스 구축 시간 단축, JSON Path 연산 최적화, 대규모 Pub/Sub 환경에서의 메모리 사용량 개선이 핵심입니다. 운영 환경에서는 키 만료 정책과 AOF 재작성 주기를 함께 점검해 성능 향상 효과를 안정적으로 가져갈 수 있습니다.~' AS summary,
           128 AS view_count
      FROM dual
) source
ON (target.id = source.id)
WHEN MATCHED THEN
    UPDATE SET
        target.news_date = source.news_date,
        target.source = source.source,
        target.title = source.title,
        target.url = source.url,
        target.attachment_image_path = source.attachment_image_path,
        target.summary = source.summary,
        target.view_count = source.view_count,
        target.created_at = SYSTIMESTAMP
WHEN NOT MATCHED THEN
    INSERT (id, news_date, source, title, url, attachment_image_path, summary, view_count, created_at)
    VALUES (source.id, source.news_date, source.source, source.title, source.url, source.attachment_image_path, source.summary, source.view_count, SYSTIMESTAMP);

MERGE INTO tech_news target
USING (
    SELECT 12002 AS id,
           TRUNC(SYSDATE) AS news_date,
           'OpenJDK' AS source,
           'JDK 23 성능 리포트: 가비지 컬렉션 튜닝 가이드 업데이트' AS title,
           'https://openjdk.org/' AS url,
           NULL AS attachment_image_path,
           q'~ZGC/Generational ZGC 비교 자료가 확장되며, 서비스 특성별 힙 사이징 권장안이 구체화되었습니다. 배치성 워크로드와 API 서버 워크로드를 분리해 측정해야 실제 개선 폭을 정확히 확인할 수 있습니다.~' AS summary,
           93 AS view_count
      FROM dual
) source
ON (target.id = source.id)
WHEN MATCHED THEN
    UPDATE SET
        target.news_date = source.news_date,
        target.source = source.source,
        target.title = source.title,
        target.url = source.url,
        target.attachment_image_path = source.attachment_image_path,
        target.summary = source.summary,
        target.view_count = source.view_count,
        target.created_at = SYSTIMESTAMP
WHEN NOT MATCHED THEN
    INSERT (id, news_date, source, title, url, attachment_image_path, summary, view_count, created_at)
    VALUES (source.id, source.news_date, source.source, source.title, source.url, source.attachment_image_path, source.summary, source.view_count, SYSTIMESTAMP);

MERGE INTO tech_news target
USING (
    SELECT 12003 AS id,
           TRUNC(SYSDATE) AS news_date,
           'Kubernetes' AS source,
           'Kubernetes 네트워크 정책 베스트 프랙티스 2026 업데이트' AS title,
           'https://kubernetes.io/blog/' AS url,
           '/images/default-thumb.svg' AS attachment_image_path,
           q'~네임스페이스 단위 기본 차단 정책, 서비스 계정 분리, egress 화이트리스트 전략을 조합한 운영 예시가 공개되었습니다. 특히 멀티테넌트 클러스터에서는 정책 우선순위 충돌을 로그 기반으로 상시 검증하는 방식이 강조됩니다.~' AS summary,
           77 AS view_count
      FROM dual
) source
ON (target.id = source.id)
WHEN MATCHED THEN
    UPDATE SET
        target.news_date = source.news_date,
        target.source = source.source,
        target.title = source.title,
        target.url = source.url,
        target.attachment_image_path = source.attachment_image_path,
        target.summary = source.summary,
        target.view_count = source.view_count,
        target.created_at = SYSTIMESTAMP
WHEN NOT MATCHED THEN
    INSERT (id, news_date, source, title, url, attachment_image_path, summary, view_count, created_at)
    VALUES (source.id, source.news_date, source.source, source.title, source.url, source.attachment_image_path, source.summary, source.view_count, SYSTIMESTAMP);

MERGE INTO tech_news target
USING (
    SELECT 12004 AS id,
           TRUNC(SYSDATE) AS news_date,
           'Spring' AS source,
           'Spring Boot 운영 체크리스트: Health, Metrics, Trace 표준화' AS title,
           'https://spring.io/blog' AS url,
           NULL AS attachment_image_path,
           q'~운영 장애 대응 시간을 줄이기 위해 Actuator 엔드포인트 노출 정책, 메트릭 태그 표준, 트레이스 샘플링 비율을 팀 공통 규칙으로 맞추는 사례가 공유되었습니다. API SLA를 가진 서비스는 지연 구간을 퍼센타일 기준으로 모니터링하는 구성이 권장됩니다.~' AS summary,
           65 AS view_count
      FROM dual
) source
ON (target.id = source.id)
WHEN MATCHED THEN
    UPDATE SET
        target.news_date = source.news_date,
        target.source = source.source,
        target.title = source.title,
        target.url = source.url,
        target.attachment_image_path = source.attachment_image_path,
        target.summary = source.summary,
        target.view_count = source.view_count,
        target.created_at = SYSTIMESTAMP
WHEN NOT MATCHED THEN
    INSERT (id, news_date, source, title, url, attachment_image_path, summary, view_count, created_at)
    VALUES (source.id, source.news_date, source.source, source.title, source.url, source.attachment_image_path, source.summary, source.view_count, SYSTIMESTAMP);

MERGE INTO tech_news target
USING (
    SELECT 12005 AS id,
           TRUNC(SYSDATE) AS news_date,
           'GitHub' AS source,
           'GitHub Actions 캐시 전략 재정비로 CI 시간 30% 단축 사례' AS title,
           'https://github.blog/' AS url,
           '/images/default-thumb.svg' AS attachment_image_path,
           q'~언어별 빌드 캐시 키를 단일 저장소에 통합하고, 브랜치별 fallback 키를 설계해 캐시 적중률을 개선한 사례입니다. 캐시 무효화 기준을 커밋 해시가 아닌 의존성 락 파일로 전환해 불필요한 재빌드를 줄였습니다.~' AS summary,
           52 AS view_count
      FROM dual
) source
ON (target.id = source.id)
WHEN MATCHED THEN
    UPDATE SET
        target.news_date = source.news_date,
        target.source = source.source,
        target.title = source.title,
        target.url = source.url,
        target.attachment_image_path = source.attachment_image_path,
        target.summary = source.summary,
        target.view_count = source.view_count,
        target.created_at = SYSTIMESTAMP
WHEN NOT MATCHED THEN
    INSERT (id, news_date, source, title, url, attachment_image_path, summary, view_count, created_at)
    VALUES (source.id, source.news_date, source.source, source.title, source.url, source.attachment_image_path, source.summary, source.view_count, SYSTIMESTAMP);

MERGE INTO tech_news target
USING (
    SELECT 12006 AS id,
           TRUNC(SYSDATE) AS news_date,
           'Cloud Native' AS source,
           '서비스 메시 없는 트래픽 제어: Gateway API 기반 패턴 증가' AS title,
           'https://gateway-api.sigs.k8s.io/' AS url,
           NULL AS attachment_image_path,
           q'~소규모 팀에서는 서비스 메시 도입 대신 Gateway API와 표준 Ingress 조합으로 트래픽 정책을 단순화하는 추세가 확산되고 있습니다. 점진적 롤아웃은 헤더 기반 라우팅과 카나리 지표를 결합해 운영 복잡도를 낮추는 방식이 주류입니다.~' AS summary,
           41 AS view_count
      FROM dual
) source
ON (target.id = source.id)
WHEN MATCHED THEN
    UPDATE SET
        target.news_date = source.news_date,
        target.source = source.source,
        target.title = source.title,
        target.url = source.url,
        target.attachment_image_path = source.attachment_image_path,
        target.summary = source.summary,
        target.view_count = source.view_count,
        target.created_at = SYSTIMESTAMP
WHEN NOT MATCHED THEN
    INSERT (id, news_date, source, title, url, attachment_image_path, summary, view_count, created_at)
    VALUES (source.id, source.news_date, source.source, source.title, source.url, source.attachment_image_path, source.summary, source.view_count, SYSTIMESTAMP);

COMMIT;
