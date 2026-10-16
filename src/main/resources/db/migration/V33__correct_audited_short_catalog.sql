-- Audited SYSTEM catalogue thumbnails, 2026-10-16. Preserve assets/history; correct only known seeded names.
UPDATE hairstyle_recommendation_profiles p
SET compatible_hair_lengths = p.compatible_hair_lengths - 'SHORT',
    minimum_hair_length = 'BELOW_CHIN', metadata_version = 'length-audit-20261016', updated_at = now()
FROM hairstyle_assets a JOIN files f ON f.file_id = a.thumbnail_file_id AND f.owner_type = 'SYSTEM'
WHERE p.hairstyle_id = a.hairstyle_id
  AND a.style_name IN ('클래식 보브', '컬리 스타일 01', '컬리 스타일 02', '컬리 스타일 03');

UPDATE hairstyle_recommendation_profiles p
SET compatible_hair_lengths = '["BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,
    minimum_hair_length = 'BELOW_SHOULDER', metadata_version = 'length-audit-20261016', updated_at = now()
FROM hairstyle_assets a JOIN files f ON f.file_id = a.thumbnail_file_id AND f.owner_type = 'SYSTEM'
WHERE p.hairstyle_id = a.hairstyle_id
  AND a.style_name IN ('컬리 스타일 08', '컬리 스타일 09', '컬리 스타일 10');

-- Group photos, tied hair and indistinct archival portraits cannot serve as a current haircut reference.
UPDATE hairstyle_assets a SET active = false, updated_at = now()
FROM files f WHERE f.file_id = a.thumbnail_file_id AND f.owner_type = 'SYSTEM'
  AND a.style_name IN ('숏 스타일 05', '숏 스타일 06', '숏 스타일 07', '숏 스타일 08',
                       '컬리 스타일 04', '컬리 스타일 06', '컬리 스타일 07');
