-- "한 사용자, 한 대상 구성, 활성 링크 1개" 를 DB 가 강제한다.
--
-- 지금까지는 공유 버튼을 누를 때마다 새 링크가 생기고 이전 링크가 그대로 살아 있었다. 살아
-- 있는 공개 URL 의 개수에 상한이 없어, 시술기록이 몇 건이든 활성 링크는 얼마든지 늘어난다.
--
-- 대상 구성은 share_records·share_saved_styles 두 조인 테이블에 흩어져 있어 유니크 제약을
-- 직접 걸 수 없다. 그래서 정렬된 식별자 목록의 SHA-256 을 target_hash 로 비정규화해 shares
-- 한 테이블 안에서 비교한다.
--
-- 부분 인덱스의 조건은 status = 'ACTIVE' 뿐이다. 만료는 expires_at 비교라 인덱스 조건에
-- now() 를 쓸 수 없다(불변 표현식이 아니다). 그래서 만료됐지만 상태가 ACTIVE 인 행도 인덱스
-- 대상이다 — 애플리케이션이 발급 직전에 같은 대상의 ACTIVE 행을 만료 여부와 무관하게 모두
-- 폐기하므로 충돌하지 않는다.
ALTER TABLE shares ADD COLUMN target_hash VARCHAR(64);

-- 백필. 정렬 키를 uuid 가 아니라 text 로 두는 이유는 애플리케이션이 UUID 문자열을 정렬해
-- 같은 문자열을 만들기 때문이다. 두 정렬 기준이 어긋나면 같은 대상이 다른 해시를 갖는다.
UPDATE shares s
SET target_hash = encode(sha256(convert_to(t.payload, 'UTF8')), 'hex')
FROM (
    SELECT sh.share_id,
           coalesce((SELECT string_agg(r.record_id::text, ',' ORDER BY r.record_id::text)
                     FROM share_records r WHERE r.share_id = sh.share_id), '')
           || '|'
           || coalesce((SELECT string_agg(ss.saved_style_id::text, ',' ORDER BY ss.saved_style_id::text)
                        FROM share_saved_styles ss WHERE ss.share_id = sh.share_id), '') AS payload
    FROM shares sh
) t
WHERE s.share_id = t.share_id;

ALTER TABLE shares ALTER COLUMN target_hash SET NOT NULL;

-- 기존 중복 정리. 같은 대상을 가리키는 활성 링크 중 가장 최근 것만 남긴다. 사용자가 마지막에
-- 발급한 링크가 지금 쓰고 있는 링크이고, 그 이전 것들은 이미 대체된 것으로 본다.
UPDATE shares
SET status = 'REVOKED',
    revoked_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE share_id IN (
    SELECT share_id FROM (
        SELECT share_id,
               row_number() OVER (PARTITION BY user_id, target_hash
                                  ORDER BY created_at DESC, share_id DESC) AS recency
        FROM shares
        WHERE status = 'ACTIVE'
    ) ranked
    WHERE ranked.recency > 1
);

CREATE UNIQUE INDEX uq_shares_active_target
    ON shares(user_id, target_hash)
    WHERE status = 'ACTIVE';
