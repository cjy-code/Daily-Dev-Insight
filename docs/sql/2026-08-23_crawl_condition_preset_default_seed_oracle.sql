-- @date 2026-08-23
-- @desc 새 환경(빈 Oracle 스키마)에서도 크롤링 조건 프리셋 목록이 비어 있지 않도록
-- 'Default'와 키워드 필터 예시 'AI 키워드 필터' 프리셋을 시드합니다.
-- 참고용 기록 파일입니다. 실제 시드는 OracleSchemaMigrationRunner#ensureDefaultCrawlConditionPreset() /
-- #ensureAiKeywordCrawlConditionPreset()가 앱 기동 시(@PostConstruct) 자동으로 적용하며,
-- 이 파일을 수동 실행할 필요는 없습니다.
--
-- 주의: 최초 배포 시 소스로 GeekNews(https://feeds.feedburner.com/geeknews-feed)를 사용했으나,
-- 이 프로젝트의 RssNewsCrawlerClient는 RSS 2.0 <item> 태그만 파싱하는데 GeekNews는 Atom(<entry>)만
-- 제공해 수집 건수가 항상 0건이었습니다. 이미 있는 프리셋(잘못된 소스로 생성된)은 아래 UPDATE로
-- Hacker News(hnrss.org, 실제 RSS 2.0)로 보정하고, 없으면 INSERT로 새로 만듭니다.

UPDATE crawl_condition_preset
   SET source_name = 'Hacker News',
       source_url = 'https://hnrss.org/frontpage',
       updated_at = SYSTIMESTAMP
 WHERE preset_name = 'Default';

INSERT INTO crawl_condition_preset (
    id,
    preset_name,
    source_name,
    source_url,
    max_articles,
    keyword_match_type,
    connect_timeout_seconds,
    read_timeout_seconds,
    retry_count,
    is_active,
    created_at,
    updated_at
)
SELECT
    seq_crawl_condition_preset.NEXTVAL,
    'Default',
    'Hacker News',
    'https://hnrss.org/frontpage',
    20,
    'OR',
    5,
    5,
    1,
    1,
    SYSTIMESTAMP,
    SYSTIMESTAMP
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM crawl_condition_preset WHERE preset_name = 'Default');

UPDATE crawl_condition_preset
   SET source_name = 'Hacker News',
       source_url = 'https://hnrss.org/frontpage',
       include_keywords = 'AI,Security,Database',
       include_keyword_operators = 'OR,OR,OR',
       exclude_keywords = 'Hiring',
       updated_at = SYSTIMESTAMP
 WHERE preset_name = 'AI 키워드 필터';

INSERT INTO crawl_condition_preset (
    id,
    preset_name,
    source_name,
    source_url,
    max_articles,
    keyword_match_type,
    include_keywords,
    include_keyword_operators,
    exclude_keywords,
    connect_timeout_seconds,
    read_timeout_seconds,
    retry_count,
    is_active,
    created_at,
    updated_at
)
SELECT
    seq_crawl_condition_preset.NEXTVAL,
    'AI 키워드 필터',
    'Hacker News',
    'https://hnrss.org/frontpage',
    20,
    'OR',
    'AI,Security,Database',
    'OR,OR,OR',
    'Hiring',
    5,
    5,
    1,
    1,
    SYSTIMESTAMP,
    SYSTIMESTAMP
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM crawl_condition_preset WHERE preset_name = 'AI 키워드 필터');

COMMIT;
