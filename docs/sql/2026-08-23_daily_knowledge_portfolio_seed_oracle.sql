-- @date 2026-08-23
-- @desc 포트폴리오 소개 문서 스크린샷용으로 daily_knowledge에 카테고리별 샘플 지식 6건을
-- 최근 6일(오늘 포함)로 날짜를 분산하여 시드합니다. title 기준으로 중복 삽입을 막는 idempotent 스크립트입니다.
-- attachment_image_path는 NULL로 두어 프런트에서 /images/default-thumb.svg로 자동 대체됩니다.

INSERT INTO daily_knowledge (
    knowledge_date, category, title, attachment_image_path, summary, detail, view_count, created_at
)
SELECT
    TRUNC(SYSDATE),
    'AI',
    'Retrieval-Augmented Generation: Grounding LLM Answers in Real Data',
    NULL,
    'RAG combines a retrieval step with generation so the model answers using your own documents instead of relying only on what it memorized during training.',
    'A typical RAG pipeline embeds your documents into a vector store, retrieves the most relevant chunks for a given query, and feeds them into the LLM prompt as context.\n\nThis reduces hallucination and lets you update knowledge without retraining the model - swapping the source documents is enough to change what the assistant knows.\n\nIn production, chunk size, embedding model choice, and retrieval ranking (top-k, reranking) have an outsized effect on answer quality, so they deserve as much tuning attention as the prompt itself.',
    142,
    SYSTIMESTAMP
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM daily_knowledge WHERE title = 'Retrieval-Augmented Generation: Grounding LLM Answers in Real Data'
);

INSERT INTO daily_knowledge (
    knowledge_date, category, title, attachment_image_path, summary, detail, view_count, created_at
)
SELECT
    TRUNC(SYSDATE) - 1,
    'Frontend',
    'React Server Components: Rethinking Where Your UI Renders',
    NULL,
    'Server Components let parts of a React tree render on the server and stream to the client, cutting the JavaScript bundle shipped for data-heavy views.',
    'Unlike traditional SSR, Server Components never re-render on the client - they produce a serialized tree that the client merges with interactive Client Components.\n\nThis split forces a clearer boundary between data-fetching/presentational code (server) and stateful/interactive code (client), which tends to simplify data loading logic.\n\nThe tradeoff is a steeper mental model: knowing which files can use hooks or browser APIs, and which can safely touch a database or secret keys, becomes an explicit architectural decision rather than an afterthought.',
    98,
    SYSTIMESTAMP - 1
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM daily_knowledge WHERE title = 'React Server Components: Rethinking Where Your UI Renders'
);

INSERT INTO daily_knowledge (
    knowledge_date, category, title, attachment_image_path, summary, detail, view_count, created_at
)
SELECT
    TRUNC(SYSDATE) - 2,
    'DevOps',
    'Blue-Green Deployments: Zero-Downtime Releases Explained',
    NULL,
    'Blue-green deployment keeps two identical production environments and switches traffic between them, so a release becomes a routing change instead of an in-place upgrade.',
    'While one environment (blue) serves live traffic, the new version is deployed to the idle environment (green) and fully tested before the switch.\n\nRollback is nearly instant: if the new version misbehaves, traffic is routed back to blue, which never stopped running the previous known-good build.\n\nThe main cost is running double the infrastructure during a release window, and care is needed for stateful components (databases, caches) that both environments must share safely.',
    76,
    SYSTIMESTAMP - 2
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM daily_knowledge WHERE title = 'Blue-Green Deployments: Zero-Downtime Releases Explained'
);

INSERT INTO daily_knowledge (
    knowledge_date, category, title, attachment_image_path, summary, detail, view_count, created_at
)
SELECT
    TRUNC(SYSDATE) - 3,
    'Data',
    'Change Data Capture: Streaming Database Changes in Real Time',
    NULL,
    'CDC captures row-level inserts, updates, and deletes from a database transaction log and streams them as events, without adding load to the source database via polling.',
    'Most CDC tools (e.g. Debezium) read the database''s write-ahead or binlog directly, turning every committed change into an event on a message bus like Kafka.\n\nThis pattern powers cache invalidation, search-index synchronization, and cross-service data replication without coupling the source database to every downstream consumer.\n\nOrdering and exactly-once delivery are the hard parts - most systems settle for at-least-once delivery and require consumers to handle duplicate events idempotently.',
    121,
    SYSTIMESTAMP - 3
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM daily_knowledge WHERE title = 'Change Data Capture: Streaming Database Changes in Real Time'
);

INSERT INTO daily_knowledge (
    knowledge_date, category, title, attachment_image_path, summary, detail, view_count, created_at
)
SELECT
    TRUNC(SYSDATE) - 4,
    'Security',
    'OAuth 2.0 vs OpenID Connect: Knowing What Each One Actually Solves',
    NULL,
    'OAuth 2.0 is an authorization framework for granting limited access to resources, while OpenID Connect is an identity layer built on top of it for authenticating who the user is.',
    'A common mistake is using a bare OAuth access token to identify a user - OAuth was never designed to prove identity, only to authorize scoped access to an API.\n\nOpenID Connect adds the ID token (a signed JWT with user claims) and a standardized userinfo endpoint, giving you a verifiable answer to "who is this user" alongside the access token.\n\nWhen integrating a login flow, reaching for an OIDC-compliant provider and validating the ID token signature/audience/expiry is safer than inventing an identity contract on top of raw OAuth.',
    89,
    SYSTIMESTAMP - 4
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM daily_knowledge WHERE title = 'OAuth 2.0 vs OpenID Connect: Knowing What Each One Actually Solves'
);

INSERT INTO daily_knowledge (
    knowledge_date, category, title, attachment_image_path, summary, detail, view_count, created_at
)
SELECT
    TRUNC(SYSDATE) - 5,
    'Backend',
    'Idempotency Keys: Making Retried API Calls Safe',
    NULL,
    'An idempotency key is a client-generated unique ID attached to a request so that retrying the same request (e.g. after a timeout) never applies its side effect twice.',
    'The server stores the result of the first request under the idempotency key; if the same key arrives again, it returns the stored result instead of re-executing the operation.\n\nThis matters most for money-moving or state-mutating endpoints (payments, order creation) where network retries are common but double-processing is unacceptable.\n\nKeys need a reasonable expiry window and should be scoped per client/endpoint, otherwise unrelated requests can collide or stale results can be served long after they stopped being valid.',
    103,
    SYSTIMESTAMP - 5
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM daily_knowledge WHERE title = 'Idempotency Keys: Making Retried API Calls Safe'
);

COMMIT;
