-- Production top-three image audit: this seeded image shows braids, not the advertised medium CUT.
-- Its length, service and preference metadata cannot be justified from the thumbnail. Keep history/files.
UPDATE hairstyle_assets a
SET active = false, updated_at = now()
FROM files f
WHERE f.file_id = a.thumbnail_file_id AND f.owner_type = 'SYSTEM'
  AND a.style_name = '미디엄 스타일 32' AND a.category = 'MEDIUM';
