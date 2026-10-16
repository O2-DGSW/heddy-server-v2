BEGIN;

SET LOCAL lock_timeout = '10s';

SET LOCAL statement_timeout = '60s';

SELECT pg_advisory_xact_lock(hashtext('heddy-catalog-refresh'));

DO $$ BEGIN IF (SELECT count(*) FROM hairstyle_assets a JOIN files f ON f.file_id=a.thumbnail_file_id WHERE a.hairstyle_id IN ('71000000-0000-4000-8000-000000000045'::uuid,'71000000-0000-4000-8000-000000000068'::uuid,'71000000-0000-4000-8000-000000000069'::uuid,'71000000-0000-4000-8000-000000000074'::uuid,'71000000-0000-4000-8000-000000000075'::uuid,'71000000-0000-4000-8000-000000000076'::uuid,'71000000-0000-4000-8000-000000000077'::uuid,'71000000-0000-4000-8000-000000000078'::uuid,'71000000-0000-4000-8000-000000000001'::uuid,'71000000-0000-4000-8000-000000000002'::uuid,'71000000-0000-4000-8000-000000000003'::uuid,'71000000-0000-4000-8000-000000000007'::uuid,'71000000-0000-4000-8000-000000000008'::uuid,'71000000-0000-4000-8000-000000000009'::uuid,'71000000-0000-4000-8000-000000000010'::uuid,'71000000-0000-4000-8000-000000000011'::uuid,'71000000-0000-4000-8000-000000000014'::uuid,'71000000-0000-4000-8000-000000000015'::uuid,'71000000-0000-4000-8000-000000000016'::uuid,'71000000-0000-4000-8000-000000000017'::uuid,'71000000-0000-4000-8000-000000000021'::uuid,'71000000-0000-4000-8000-000000000022'::uuid,'71000000-0000-4000-8000-000000000023'::uuid,'71000000-0000-4000-8000-000000000024'::uuid,'71000000-0000-4000-8000-000000000025'::uuid,'71000000-0000-4000-8000-000000000026'::uuid,'71000000-0000-4000-8000-000000000027'::uuid,'71000000-0000-4000-8000-000000000028'::uuid,'71000000-0000-4000-8000-000000000029'::uuid,'71000000-0000-4000-8000-000000000030'::uuid,'71000000-0000-4000-8000-000000000050'::uuid,'71000000-0000-4000-8000-000000000051'::uuid,'71000000-0000-4000-8000-000000000052'::uuid,'71000000-0000-4000-8000-000000000053'::uuid,'71000000-0000-4000-8000-000000000054'::uuid,'71000000-0000-4000-8000-000000000055'::uuid,'71000000-0000-4000-8000-000000000056'::uuid,'71000000-0000-4000-8000-000000000057'::uuid,'71000000-0000-4000-8000-000000000058'::uuid,'71000000-0000-4000-8000-000000000059'::uuid,'71000000-0000-4000-8000-000000000060'::uuid,'71000000-0000-4000-8000-000000000061'::uuid,'71000000-0000-4000-8000-000000000062'::uuid,'71000000-0000-4000-8000-000000000063'::uuid,'71000000-0000-4000-8000-000000000064'::uuid,'71000000-0000-4000-8000-000000000065'::uuid,'71000000-0000-4000-8000-000000000066'::uuid,'71000000-0000-4000-8000-000000000067'::uuid,'71000000-0000-4000-8000-000000000012'::uuid,'71000000-0000-4000-8000-000000000013'::uuid,'71000000-0000-4000-8000-000000000018'::uuid,'71000000-0000-4000-8000-000000000019'::uuid,'71000000-0000-4000-8000-000000000020'::uuid,'71000000-0000-4000-8000-000000000031'::uuid,'71000000-0000-4000-8000-000000000032'::uuid,'71000000-0000-4000-8000-000000000033'::uuid,'71000000-0000-4000-8000-000000000037'::uuid,'71000000-0000-4000-8000-000000000038'::uuid,'71000000-0000-4000-8000-000000000039'::uuid,'71000000-0000-4000-8000-000000000040'::uuid,'71000000-0000-4000-8000-000000000042'::uuid,'71000000-0000-4000-8000-000000000043'::uuid,'71000000-0000-4000-8000-000000000044'::uuid,'71000000-0000-4000-8000-000000000046'::uuid,'71000000-0000-4000-8000-000000000048'::uuid,'71000000-0000-4000-8000-000000000071'::uuid,'71000000-0000-4000-8000-000000000073'::uuid,'71000000-0000-4000-8000-000000000080'::uuid,'71000000-0000-4000-8000-000000000081'::uuid,'71000000-0000-4000-8000-000000000082'::uuid,'71000000-0000-4000-8000-000000000083'::uuid,'71000000-0000-4000-8000-000000000084'::uuid,'71000000-0000-4000-8000-000000000085'::uuid,'71000000-0000-4000-8000-000000000086'::uuid,'71000000-0000-4000-8000-000000000087'::uuid,'71000000-0000-4000-8000-000000000088'::uuid,'71000000-0000-4000-8000-000000000089'::uuid,'71000000-0000-4000-8000-000000000090'::uuid,'71000000-0000-4000-8000-000000000092'::uuid,'71000000-0000-4000-8000-000000000093'::uuid,'71000000-0000-4000-8000-000000000094'::uuid,'71000000-0000-4000-8000-000000000095'::uuid,'71000000-0000-4000-8000-000000000096'::uuid,'71000000-0000-4000-8000-000000000097'::uuid,'71000000-0000-4000-8000-000000000100'::uuid,'71000000-0000-4000-8000-000000000004'::uuid,'71000000-0000-4000-8000-000000000006'::uuid,'71000000-0000-4000-8000-000000000035'::uuid,'71000000-0000-4000-8000-000000000036'::uuid,'71000000-0000-4000-8000-000000000047'::uuid,'71000000-0000-4000-8000-000000000049'::uuid,'71000000-0000-4000-8000-000000000070'::uuid,'71000000-0000-4000-8000-000000000072'::uuid,'71000000-0000-4000-8000-000000000079'::uuid,'71000000-0000-4000-8000-000000000091'::uuid,'71000000-0000-4000-8000-000000000098'::uuid,'71000000-0000-4000-8000-000000000099'::uuid,'71000000-0000-4000-8000-000000000005'::uuid,'71000000-0000-4000-8000-000000000034'::uuid,'71000000-0000-4000-8000-000000000041'::uuid) AND f.owner_type='SYSTEM' AND f.purpose='HAIRSTYLE_THUMBNAIL') <> 100 THEN RAISE EXCEPTION 'Original SYSTEM catalogue does not match the reviewed 100 records'; END IF; END $$;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('daa307bc-d79f-56f6-9ec4-76d55ab60d84','eafbe55d-7dab-5cc3-b523-1f616851a0c2',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/daa307bc-d79f-56f6-9ec4-76d55ab60d84.jpg','image/jpeg',72941,'7cd22bb04afe2a027620e55ba246e9bcff19e5e39e930f1c3f8fd5c844a84f5b',1200,800,TIMESTAMPTZ '2100-01-01 00:00:00+00','하드파트 포마드컷.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='daa307bc-d79f-56f6-9ec4-76d55ab60d84' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/daa307bc-d79f-56f6-9ec4-76d55ab60d84.jpg' AND sha256='7cd22bb04afe2a027620e55ba246e9bcff19e5e39e930f1c3f8fd5c844a84f5b' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('0d198408-5bf7-514c-b3af-52d56d990361','하드파트 포마드컷','SHORT','daa307bc-d79f-56f6-9ec4-76d55ab60d84',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('0d198408-5bf7-514c-b3af-52d56d990361','["CUT", "STYLING"]'::jsonb,'["SHORT", "BELOW_CHIN", "BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["STRAIGHT", "WAVY"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'SHORT',10,'MEDIUM','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO style_tags (style_tag_id,tag_name,category) VALUES ('31683b6d-c9ee-5478-8e0d-52d9a61e58ad','포마드컷','SHORT') ON CONFLICT (style_tag_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('0d198408-5bf7-514c-b3af-52d56d990361','31683b6d-c9ee-5478-8e0d-52d9a61e58ad') ON CONFLICT DO NOTHING;

INSERT INTO style_tags (style_tag_id,tag_name,category) VALUES ('44105ceb-49bd-507b-b1b0-2b83c7889e5e','사이드파트','SHORT') ON CONFLICT (style_tag_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('0d198408-5bf7-514c-b3af-52d56d990361','44105ceb-49bd-507b-b1b0-2b83c7889e5e') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('0d9c8d3c-6b58-57fa-9764-f8967cb516d0','1dc5de0a-e214-58a9-85e4-e1c96cf4e934',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/0d9c8d3c-6b58-57fa-9764-f8967cb516d0.jpg','image/jpeg',204609,'718b928a8a0238fe6af5352ff303cffd0bec05c807f6ec0aefe02502fd9532db',1200,1800,TIMESTAMPTZ '2100-01-01 00:00:00+00','라인 언더컷 퀴프.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='0d9c8d3c-6b58-57fa-9764-f8967cb516d0' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/0d9c8d3c-6b58-57fa-9764-f8967cb516d0.jpg' AND sha256='718b928a8a0238fe6af5352ff303cffd0bec05c807f6ec0aefe02502fd9532db' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('81871f24-b393-593a-b0b8-f31d7296118a','라인 언더컷 퀴프','SHORT','0d9c8d3c-6b58-57fa-9764-f8967cb516d0',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('81871f24-b393-593a-b0b8-f31d7296118a','["CUT", "STYLING"]'::jsonb,'["SHORT", "BELOW_CHIN", "BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["STRAIGHT", "WAVY"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'SHORT',10,'MEDIUM','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO style_tags (style_tag_id,tag_name,category) VALUES ('73108c3a-e190-5c3f-afc8-6df41dc448f4','언더컷','SHORT') ON CONFLICT (style_tag_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('81871f24-b393-593a-b0b8-f31d7296118a','73108c3a-e190-5c3f-afc8-6df41dc448f4') ON CONFLICT DO NOTHING;

INSERT INTO style_tags (style_tag_id,tag_name,category) VALUES ('f5f3cdba-c2f7-535c-a6ed-3261d3b46b38','리젠트컷','SHORT') ON CONFLICT (style_tag_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('81871f24-b393-593a-b0b8-f31d7296118a','f5f3cdba-c2f7-535c-a6ed-3261d3b46b38') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('b38860ec-2947-5bab-b2f5-f603b60f4296','d33891da-54c0-5dbc-a91b-5fef07b7a52f',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/b38860ec-2947-5bab-b2f5-f603b60f4296.jpg','image/jpeg',291875,'95aaf9d6b7b885d8187db8cf199f172ef41ad3871db29640a9311b5e56a4aa9a',1200,2133,TIMESTAMPTZ '2100-01-01 00:00:00+00','텍스처 크롭 페이드.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='b38860ec-2947-5bab-b2f5-f603b60f4296' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/b38860ec-2947-5bab-b2f5-f603b60f4296.jpg' AND sha256='95aaf9d6b7b885d8187db8cf199f172ef41ad3871db29640a9311b5e56a4aa9a' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('7ffbc247-3416-5316-905e-6fe769a4153f','텍스처 크롭 페이드','SHORT','b38860ec-2947-5bab-b2f5-f603b60f4296',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('7ffbc247-3416-5316-905e-6fe769a4153f','["CUT", "STYLING"]'::jsonb,'["SHORT", "BELOW_CHIN", "BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["WAVY", "CURLY"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'SHORT',5,'LOW','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO style_tags (style_tag_id,tag_name,category) VALUES ('2d264d93-04a6-51bd-b741-dfec5339ce53','크롭컷','SHORT') ON CONFLICT (style_tag_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('7ffbc247-3416-5316-905e-6fe769a4153f','2d264d93-04a6-51bd-b741-dfec5339ce53') ON CONFLICT DO NOTHING;

INSERT INTO style_tags (style_tag_id,tag_name,category) VALUES ('4a481ee2-aee6-5fb7-bbfb-8ef480608caf','페이드컷','SHORT') ON CONFLICT (style_tag_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('7ffbc247-3416-5316-905e-6fe769a4153f','4a481ee2-aee6-5fb7-bbfb-8ef480608caf') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('b08e5ad3-825b-59b0-80ea-c16864f87666','35fea6d2-b11b-5755-8c7a-9f2f9d2e1236',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/b08e5ad3-825b-59b0-80ea-c16864f87666.jpg','image/jpeg',66203,'cac53507b351588dab19b86bb1337649ac781e17e49f459be060d17c5796fc3c',1200,800,TIMESTAMPTZ '2100-01-01 00:00:00+00','클래식 폼파두르.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='b08e5ad3-825b-59b0-80ea-c16864f87666' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/b08e5ad3-825b-59b0-80ea-c16864f87666.jpg' AND sha256='cac53507b351588dab19b86bb1337649ac781e17e49f459be060d17c5796fc3c' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('12d8e1fd-b70d-50e0-a0d5-a4d769759969','클래식 폼파두르','SHORT','b08e5ad3-825b-59b0-80ea-c16864f87666',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('12d8e1fd-b70d-50e0-a0d5-a4d769759969','["CUT", "STYLING"]'::jsonb,'["SHORT", "BELOW_CHIN", "BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["STRAIGHT", "WAVY"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'SHORT',15,'HIGH','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO style_tags (style_tag_id,tag_name,category) VALUES ('31683b6d-c9ee-5478-8e0d-52d9a61e58ad','포마드컷','SHORT') ON CONFLICT (style_tag_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('12d8e1fd-b70d-50e0-a0d5-a4d769759969','31683b6d-c9ee-5478-8e0d-52d9a61e58ad') ON CONFLICT DO NOTHING;

INSERT INTO style_tags (style_tag_id,tag_name,category) VALUES ('f5f3cdba-c2f7-535c-a6ed-3261d3b46b38','리젠트컷','SHORT') ON CONFLICT (style_tag_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('12d8e1fd-b70d-50e0-a0d5-a4d769759969','f5f3cdba-c2f7-535c-a6ed-3261d3b46b38') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('3a4285bc-72f7-5c24-a42b-5f6e8de43de0','60cd55cf-6f1a-57c1-a317-f15253bf74d8',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/3a4285bc-72f7-5c24-a42b-5f6e8de43de0.jpg','image/jpeg',255717,'4ae41d430b48f4c3ba0cd012c55662e65b7e1a586d78dd10487f828135595314',1200,1832,TIMESTAMPTZ '2100-01-01 00:00:00+00','하이 퀴프 페이드.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='3a4285bc-72f7-5c24-a42b-5f6e8de43de0' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/3a4285bc-72f7-5c24-a42b-5f6e8de43de0.jpg' AND sha256='4ae41d430b48f4c3ba0cd012c55662e65b7e1a586d78dd10487f828135595314' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('becfa223-4c43-5ca1-80b0-1e3cd17bf008','하이 퀴프 페이드','SHORT','3a4285bc-72f7-5c24-a42b-5f6e8de43de0',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('becfa223-4c43-5ca1-80b0-1e3cd17bf008','["CUT", "STYLING"]'::jsonb,'["SHORT", "BELOW_CHIN", "BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["STRAIGHT", "WAVY"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'SHORT',10,'MEDIUM','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO style_tags (style_tag_id,tag_name,category) VALUES ('f5f3cdba-c2f7-535c-a6ed-3261d3b46b38','리젠트컷','SHORT') ON CONFLICT (style_tag_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('becfa223-4c43-5ca1-80b0-1e3cd17bf008','f5f3cdba-c2f7-535c-a6ed-3261d3b46b38') ON CONFLICT DO NOTHING;

INSERT INTO style_tags (style_tag_id,tag_name,category) VALUES ('4a481ee2-aee6-5fb7-bbfb-8ef480608caf','페이드컷','SHORT') ON CONFLICT (style_tag_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('becfa223-4c43-5ca1-80b0-1e3cd17bf008','4a481ee2-aee6-5fb7-bbfb-8ef480608caf') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('9e42ce49-eaed-5b2d-8cea-9e25541ccfcc','b34630f0-9ebe-5a7b-adc3-b9621bee9bec',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/9e42ce49-eaed-5b2d-8cea-9e25541ccfcc.jpg','image/jpeg',158675,'ad553a0d085329b90b157a80f135bd31074d44b5610474831c3a03d95a6c94d1',1200,1600,TIMESTAMPTZ '2100-01-01 00:00:00+00','센터파트 볼륨 가르마.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='9e42ce49-eaed-5b2d-8cea-9e25541ccfcc' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/9e42ce49-eaed-5b2d-8cea-9e25541ccfcc.jpg' AND sha256='ad553a0d085329b90b157a80f135bd31074d44b5610474831c3a03d95a6c94d1' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('2f367c11-435d-5c1f-b005-45af3c693de2','센터파트 볼륨 가르마','SHORT','9e42ce49-eaed-5b2d-8cea-9e25541ccfcc',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('2f367c11-435d-5c1f-b005-45af3c693de2','["CUT", "STYLING"]'::jsonb,'["SHORT", "BELOW_CHIN", "BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["STRAIGHT", "WAVY"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'SHORT',10,'MEDIUM','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO style_tags (style_tag_id,tag_name,category) VALUES ('122bdbdf-5ebf-5770-8c8a-c0c09e267343','가르마컷','SHORT') ON CONFLICT (style_tag_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('2f367c11-435d-5c1f-b005-45af3c693de2','122bdbdf-5ebf-5770-8c8a-c0c09e267343') ON CONFLICT DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('2f367c11-435d-5c1f-b005-45af3c693de2','10000000-0000-4000-8000-000000000003') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('a4de0c0c-d7db-5176-8b22-5c8a5069f52c','7adc82e6-acb0-5d0d-9d88-3132604861c0',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/a4de0c0c-d7db-5176-8b22-5c8a5069f52c.jpg','image/jpeg',155578,'c4a8614f3ef12d4ef1c3f9fad4cb136456263aed7f01a742345e6839a3d48b5a',1200,1600,TIMESTAMPTZ '2100-01-01 00:00:00+00','숄더 렝스 웨트 웨이브.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='a4de0c0c-d7db-5176-8b22-5c8a5069f52c' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/a4de0c0c-d7db-5176-8b22-5c8a5069f52c.jpg' AND sha256='c4a8614f3ef12d4ef1c3f9fad4cb136456263aed7f01a742345e6839a3d48b5a' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('a04671ae-c01b-5568-8897-cea144960c1d','숄더 렝스 웨트 웨이브','MEDIUM','a4de0c0c-d7db-5176-8b22-5c8a5069f52c',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('a04671ae-c01b-5568-8897-cea144960c1d','["CUT", "STYLING"]'::jsonb,'["BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["WAVY", "CURLY"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'BELOW_SHOULDER',15,'MEDIUM','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('a04671ae-c01b-5568-8897-cea144960c1d','40000000-0000-4000-8000-000000000001') ON CONFLICT DO NOTHING;

INSERT INTO style_tags (style_tag_id,tag_name,category) VALUES ('2c517e2b-986e-5bde-8744-9ade9b5f9a7d','웨이브','MEDIUM') ON CONFLICT (style_tag_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('a04671ae-c01b-5568-8897-cea144960c1d','2c517e2b-986e-5bde-8744-9ade9b5f9a7d') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('14365489-043d-5b3d-9480-85cfdf1ab9e1','9ada00ac-de14-515e-99f0-5c90c9c82bca',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/14365489-043d-5b3d-9480-85cfdf1ab9e1.jpg','image/jpeg',274261,'14a1d4fa561c0ad657a004b0836a2d16b875e0eb44630d06d9fda2099dc91038',1200,2138,TIMESTAMPTZ '2100-01-01 00:00:00+00','사이드 스윕 숏컷.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='14365489-043d-5b3d-9480-85cfdf1ab9e1' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/14365489-043d-5b3d-9480-85cfdf1ab9e1.jpg' AND sha256='14a1d4fa561c0ad657a004b0836a2d16b875e0eb44630d06d9fda2099dc91038' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('3ae493ce-1c5c-566c-95a2-ca69ace18f5f','사이드 스윕 숏컷','SHORT','14365489-043d-5b3d-9480-85cfdf1ab9e1',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('3ae493ce-1c5c-566c-95a2-ca69ace18f5f','["CUT", "STYLING"]'::jsonb,'["SHORT", "BELOW_CHIN", "BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["STRAIGHT", "WAVY"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'SHORT',5,'LOW','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO style_tags (style_tag_id,tag_name,category) VALUES ('44105ceb-49bd-507b-b1b0-2b83c7889e5e','사이드파트','SHORT') ON CONFLICT (style_tag_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('3ae493ce-1c5c-566c-95a2-ca69ace18f5f','44105ceb-49bd-507b-b1b0-2b83c7889e5e') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('e0e0a3a7-31ba-5cd9-8a6b-5ba3a334df4f','785ef37b-cb07-5ddc-93f8-4ea39f6037ec',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/e0e0a3a7-31ba-5cd9-8a6b-5ba3a334df4f.jpg','image/jpeg',173960,'639db8c8913cfe961c1890b1cc6844a6441238ab3989a71689696b58e4a428fd',1200,1707,TIMESTAMPTZ '2100-01-01 00:00:00+00','컬리 하이톱.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='e0e0a3a7-31ba-5cd9-8a6b-5ba3a334df4f' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/e0e0a3a7-31ba-5cd9-8a6b-5ba3a334df4f.jpg' AND sha256='639db8c8913cfe961c1890b1cc6844a6441238ab3989a71689696b58e4a428fd' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('0c29ffac-9ee8-56b8-a7bf-13d46a0830a5','컬리 하이톱','SHORT','e0e0a3a7-31ba-5cd9-8a6b-5ba3a334df4f',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('0c29ffac-9ee8-56b8-a7bf-13d46a0830a5','["CUT", "STYLING"]'::jsonb,'["SHORT", "BELOW_CHIN", "BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["CURLY"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'SHORT',10,'MEDIUM','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO style_tags (style_tag_id,tag_name,category) VALUES ('0bece143-5b8e-50f8-b836-c698a5e9378e','컬리컷','SHORT') ON CONFLICT (style_tag_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('0c29ffac-9ee8-56b8-a7bf-13d46a0830a5','0bece143-5b8e-50f8-b836-c698a5e9378e') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('f35d850d-e1e9-5889-8536-170c1b4dd860','c9c7c93e-5a05-5c2b-a8ce-f108c0fe20b4',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/f35d850d-e1e9-5889-8536-170c1b4dd860.jpg','image/jpeg',119849,'762bfc399c1f086e30036a40fc1c89b1b273485b328c0e0a8b81ce38ab3075e8',1200,1500,TIMESTAMPTZ '2100-01-01 00:00:00+00','내추럴 아프로.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='f35d850d-e1e9-5889-8536-170c1b4dd860' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/f35d850d-e1e9-5889-8536-170c1b4dd860.jpg' AND sha256='762bfc399c1f086e30036a40fc1c89b1b273485b328c0e0a8b81ce38ab3075e8' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('3719ea70-8240-5652-8a16-02f8f5685b5f','내추럴 아프로','SHORT','f35d850d-e1e9-5889-8536-170c1b4dd860',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('3719ea70-8240-5652-8a16-02f8f5685b5f','["CUT", "STYLING"]'::jsonb,'["SHORT", "BELOW_CHIN", "BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["CURLY"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'SHORT',10,'MEDIUM','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO style_tags (style_tag_id,tag_name,category) VALUES ('6d25f1f6-498a-51b9-b88a-b9b937a6c1bd','아프로','SHORT') ON CONFLICT (style_tag_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('3719ea70-8240-5652-8a16-02f8f5685b5f','6d25f1f6-498a-51b9-b88a-b9b937a6c1bd') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('251f0cfd-e52c-5d25-b760-afcb97ef0211','9f567933-7385-59e0-b97f-caf3ba3deacc',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/251f0cfd-e52c-5d25-b760-afcb97ef0211.jpg','image/jpeg',137001,'9b43790775b604fc4f67c6a2e27533b373c09420cb82879e69abf9b83f0f300d',1200,1800,TIMESTAMPTZ '2100-01-01 00:00:00+00','컬리 프린지 테이퍼.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='251f0cfd-e52c-5d25-b760-afcb97ef0211' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/251f0cfd-e52c-5d25-b760-afcb97ef0211.jpg' AND sha256='9b43790775b604fc4f67c6a2e27533b373c09420cb82879e69abf9b83f0f300d' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('55a650e5-5a99-5ed7-bd78-2ede7a818ed6','컬리 프린지 테이퍼','SHORT','251f0cfd-e52c-5d25-b760-afcb97ef0211',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('55a650e5-5a99-5ed7-bd78-2ede7a818ed6','["CUT", "STYLING"]'::jsonb,'["SHORT", "BELOW_CHIN", "BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["WAVY", "CURLY"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'SHORT',10,'MEDIUM','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO style_tags (style_tag_id,tag_name,category) VALUES ('0bece143-5b8e-50f8-b836-c698a5e9378e','컬리컷','SHORT') ON CONFLICT (style_tag_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('55a650e5-5a99-5ed7-bd78-2ede7a818ed6','0bece143-5b8e-50f8-b836-c698a5e9378e') ON CONFLICT DO NOTHING;

INSERT INTO style_tags (style_tag_id,tag_name,category) VALUES ('143f8cd1-e08f-5cc5-8c23-8838463620b5','테이퍼컷','SHORT') ON CONFLICT (style_tag_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('55a650e5-5a99-5ed7-bd78-2ede7a818ed6','143f8cd1-e08f-5cc5-8c23-8838463620b5') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('9bd9a927-911f-5afb-8777-6c32d6ee9aa5','54c0e743-8f32-5c08-82f2-1cda777a6f2a',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/9bd9a927-911f-5afb-8777-6c32d6ee9aa5.jpg','image/jpeg',259470,'21b6e777221880b6df28df3ee160fd88ffc74ef85a9b2408b6685123ccd54832',1200,1509,TIMESTAMPTZ '2100-01-01 00:00:00+00','웨이브 퀴프 언더컷.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='9bd9a927-911f-5afb-8777-6c32d6ee9aa5' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/9bd9a927-911f-5afb-8777-6c32d6ee9aa5.jpg' AND sha256='21b6e777221880b6df28df3ee160fd88ffc74ef85a9b2408b6685123ccd54832' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('f44a00c9-3c3f-5927-98f7-32966c2b137e','웨이브 퀴프 언더컷','SHORT','9bd9a927-911f-5afb-8777-6c32d6ee9aa5',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('f44a00c9-3c3f-5927-98f7-32966c2b137e','["CUT", "STYLING"]'::jsonb,'["SHORT", "BELOW_CHIN", "BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["WAVY", "CURLY"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'SHORT',10,'MEDIUM','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO style_tags (style_tag_id,tag_name,category) VALUES ('73108c3a-e190-5c3f-afc8-6df41dc448f4','언더컷','SHORT') ON CONFLICT (style_tag_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('f44a00c9-3c3f-5927-98f7-32966c2b137e','73108c3a-e190-5c3f-afc8-6df41dc448f4') ON CONFLICT DO NOTHING;

INSERT INTO style_tags (style_tag_id,tag_name,category) VALUES ('2c517e2b-986e-5bde-8744-9ade9b5f9a7d','웨이브','SHORT') ON CONFLICT (style_tag_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('f44a00c9-3c3f-5927-98f7-32966c2b137e','2c517e2b-986e-5bde-8744-9ade9b5f9a7d') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('8bf0e3b5-0402-5137-a403-03548286b0b6','f0904534-2e9e-556d-a986-5bf6ff5b1fe7',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/8bf0e3b5-0402-5137-a403-03548286b0b6.jpg','image/jpeg',280909,'2650054181fa1ed677aaceecccaaf941a73ef2cd8e6da215fc35a26074416472',1200,1783,TIMESTAMPTZ '2100-01-01 00:00:00+00','로우 페이드 사이드파트.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='8bf0e3b5-0402-5137-a403-03548286b0b6' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/8bf0e3b5-0402-5137-a403-03548286b0b6.jpg' AND sha256='2650054181fa1ed677aaceecccaaf941a73ef2cd8e6da215fc35a26074416472' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('090392ec-2fcb-56e3-ba4c-0063af2471d3','로우 페이드 사이드파트','SHORT','8bf0e3b5-0402-5137-a403-03548286b0b6',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('090392ec-2fcb-56e3-ba4c-0063af2471d3','["CUT", "STYLING"]'::jsonb,'["SHORT", "BELOW_CHIN", "BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["STRAIGHT", "WAVY"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'SHORT',5,'LOW','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO style_tags (style_tag_id,tag_name,category) VALUES ('4a481ee2-aee6-5fb7-bbfb-8ef480608caf','페이드컷','SHORT') ON CONFLICT (style_tag_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('090392ec-2fcb-56e3-ba4c-0063af2471d3','4a481ee2-aee6-5fb7-bbfb-8ef480608caf') ON CONFLICT DO NOTHING;

INSERT INTO style_tags (style_tag_id,tag_name,category) VALUES ('44105ceb-49bd-507b-b1b0-2b83c7889e5e','사이드파트','SHORT') ON CONFLICT (style_tag_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('090392ec-2fcb-56e3-ba4c-0063af2471d3','44105ceb-49bd-507b-b1b0-2b83c7889e5e') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('790da07c-fb25-54ae-ad32-463ce2dbde3d','7ec1675f-2cdf-5fdb-8dd0-1ed72c348b14',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/790da07c-fb25-54ae-ad32-463ce2dbde3d.jpg','image/jpeg',191746,'7c8b840b273b0cddc88eef441feec04e51b7db1d5ef8a4ee74aa2f18fea09122',1200,1766,TIMESTAMPTZ '2100-01-01 00:00:00+00','텍스처 멀릿컷.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='790da07c-fb25-54ae-ad32-463ce2dbde3d' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/790da07c-fb25-54ae-ad32-463ce2dbde3d.jpg' AND sha256='7c8b840b273b0cddc88eef441feec04e51b7db1d5ef8a4ee74aa2f18fea09122' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('30482fbb-adf7-5843-a988-f340b85fa429','텍스처 멀릿컷','MEDIUM','790da07c-fb25-54ae-ad32-463ce2dbde3d',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('30482fbb-adf7-5843-a988-f340b85fa429','["CUT", "STYLING"]'::jsonb,'["BELOW_CHIN", "BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["STRAIGHT", "WAVY"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'BELOW_CHIN',10,'MEDIUM','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO style_tags (style_tag_id,tag_name,category) VALUES ('3e2e412d-ee7a-5e5e-b9ae-ad080ca70269','멀릿컷','MEDIUM') ON CONFLICT (style_tag_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('30482fbb-adf7-5843-a988-f340b85fa429','3e2e412d-ee7a-5e5e-b9ae-ad080ca70269') ON CONFLICT DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('30482fbb-adf7-5843-a988-f340b85fa429','10000000-0000-4000-8000-000000000004') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('039a728d-7d09-53b4-840f-96e1411c62cf','505d88ac-a231-56fc-9346-41c2929675b8',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/039a728d-7d09-53b4-840f-96e1411c62cf.jpg','image/jpeg',185320,'b167b17eda6f2a2e329c0df9b23301feba7cfba80d474038f3c1079c977b8e94',1200,1799,TIMESTAMPTZ '2100-01-01 00:00:00+00','브러시백 멀릿컷.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='039a728d-7d09-53b4-840f-96e1411c62cf' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/039a728d-7d09-53b4-840f-96e1411c62cf.jpg' AND sha256='b167b17eda6f2a2e329c0df9b23301feba7cfba80d474038f3c1079c977b8e94' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('cb9713ac-a1b0-5e95-a303-e24bc4a425ac','브러시백 멀릿컷','MEDIUM','039a728d-7d09-53b4-840f-96e1411c62cf',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('cb9713ac-a1b0-5e95-a303-e24bc4a425ac','["CUT", "STYLING"]'::jsonb,'["BELOW_CHIN", "BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["STRAIGHT", "WAVY"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'BELOW_CHIN',10,'MEDIUM','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO style_tags (style_tag_id,tag_name,category) VALUES ('3e2e412d-ee7a-5e5e-b9ae-ad080ca70269','멀릿컷','MEDIUM') ON CONFLICT (style_tag_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('cb9713ac-a1b0-5e95-a303-e24bc4a425ac','3e2e412d-ee7a-5e5e-b9ae-ad080ca70269') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('c269cd83-125a-500e-bbe4-d44dfca09429','ce7b3303-dd4e-51a8-b77c-55c68dc52db1',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/c269cd83-125a-500e-bbe4-d44dfca09429.jpg','image/jpeg',140293,'f8733ea98f929bf3385e9ea205d3793e640808340e68f33374ed1fca6d1dabfd',1200,1798,TIMESTAMPTZ '2100-01-01 00:00:00+00','이어턱 미디엄컷.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='c269cd83-125a-500e-bbe4-d44dfca09429' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/c269cd83-125a-500e-bbe4-d44dfca09429.jpg' AND sha256='f8733ea98f929bf3385e9ea205d3793e640808340e68f33374ed1fca6d1dabfd' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('12a552fe-4ad8-5ee3-8a85-0795f3b27fb4','이어턱 미디엄컷','MEDIUM','c269cd83-125a-500e-bbe4-d44dfca09429',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('12a552fe-4ad8-5ee3-8a85-0795f3b27fb4','["CUT", "STYLING"]'::jsonb,'["BELOW_CHIN", "BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["STRAIGHT", "WAVY"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'BELOW_CHIN',5,'LOW','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('12a552fe-4ad8-5ee3-8a85-0795f3b27fb4','20000000-0000-4000-8000-000000000002') ON CONFLICT DO NOTHING;

INSERT INTO style_tags (style_tag_id,tag_name,category) VALUES ('3d5ea65d-7b9a-5826-9084-e6d3f6b45f79','센터파트','MEDIUM') ON CONFLICT (style_tag_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('12a552fe-4ad8-5ee3-8a85-0795f3b27fb4','3d5ea65d-7b9a-5826-9084-e6d3f6b45f79') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('e0c83e60-8516-5dc7-b268-8180176c8196','42f61913-29d9-5d7f-95ad-cc7bceab6d0c',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/e0c83e60-8516-5dc7-b268-8180176c8196.jpg','image/jpeg',201305,'f8e07a2f52fda943a20f947540fa6069f49713bf46af627d0a4c0a29a881dbad',1200,2133,TIMESTAMPTZ '2100-01-01 00:00:00+00','버즈컷.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='e0c83e60-8516-5dc7-b268-8180176c8196' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/e0c83e60-8516-5dc7-b268-8180176c8196.jpg' AND sha256='f8e07a2f52fda943a20f947540fa6069f49713bf46af627d0a4c0a29a881dbad' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('b98199c9-df22-5914-8d6c-ff1c9818aa2e','버즈컷','SHORT','e0c83e60-8516-5dc7-b268-8180176c8196',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('b98199c9-df22-5914-8d6c-ff1c9818aa2e','["CUT", "STYLING"]'::jsonb,'["SHORT", "BELOW_CHIN", "BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["STRAIGHT", "WAVY", "CURLY"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'SHORT',0,'LOW','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO style_tags (style_tag_id,tag_name,category) VALUES ('0f3a9f4e-5dd8-59e7-99b1-a5657d3b35d0','버즈컷','SHORT') ON CONFLICT (style_tag_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('b98199c9-df22-5914-8d6c-ff1c9818aa2e','0f3a9f4e-5dd8-59e7-99b1-a5657d3b35d0') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('cbc58c96-9253-5e84-a062-8c824986b638','79b782ca-78e9-5f5b-9743-01ecf7c8842e',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/cbc58c96-9253-5e84-a062-8c824986b638.jpg','image/jpeg',170500,'b79506f4a531bbcf9d144b4324b1149c354c34a3652c42abd4184f95e205eb94',1200,1800,TIMESTAMPTZ '2100-01-01 00:00:00+00','크루컷.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='cbc58c96-9253-5e84-a062-8c824986b638' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/cbc58c96-9253-5e84-a062-8c824986b638.jpg' AND sha256='b79506f4a531bbcf9d144b4324b1149c354c34a3652c42abd4184f95e205eb94' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('7f17a8fe-2b82-558e-969f-79a3563ce8fb','크루컷','SHORT','cbc58c96-9253-5e84-a062-8c824986b638',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('7f17a8fe-2b82-558e-969f-79a3563ce8fb','["CUT", "STYLING"]'::jsonb,'["SHORT", "BELOW_CHIN", "BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["STRAIGHT", "WAVY"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'SHORT',5,'LOW','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO style_tags (style_tag_id,tag_name,category) VALUES ('17dacfcb-659b-5910-bb6d-11b242ac6579','크루컷','SHORT') ON CONFLICT (style_tag_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('7f17a8fe-2b82-558e-969f-79a3563ce8fb','17dacfcb-659b-5910-bb6d-11b242ac6579') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('805dcd76-e71c-5aa6-bbed-6dd100f64ec2','3139fd32-2fac-5c2e-9d11-a8e5d2c05c7e',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/805dcd76-e71c-5aa6-bbed-6dd100f64ec2.jpg','image/jpeg',151545,'a61758d67be3a0238c10dd6e62eab0e4ee0cc80f6c2c959eff8a41998d490f20',1200,1800,TIMESTAMPTZ '2100-01-01 00:00:00+00','스킨 페이드 버즈컷.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='805dcd76-e71c-5aa6-bbed-6dd100f64ec2' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/805dcd76-e71c-5aa6-bbed-6dd100f64ec2.jpg' AND sha256='a61758d67be3a0238c10dd6e62eab0e4ee0cc80f6c2c959eff8a41998d490f20' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('87408dfc-91cf-53df-bfab-369e62e93e38','스킨 페이드 버즈컷','SHORT','805dcd76-e71c-5aa6-bbed-6dd100f64ec2',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('87408dfc-91cf-53df-bfab-369e62e93e38','["CUT", "STYLING"]'::jsonb,'["SHORT", "BELOW_CHIN", "BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["STRAIGHT", "WAVY", "CURLY"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'SHORT',0,'LOW','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO style_tags (style_tag_id,tag_name,category) VALUES ('0f3a9f4e-5dd8-59e7-99b1-a5657d3b35d0','버즈컷','SHORT') ON CONFLICT (style_tag_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('87408dfc-91cf-53df-bfab-369e62e93e38','0f3a9f4e-5dd8-59e7-99b1-a5657d3b35d0') ON CONFLICT DO NOTHING;

INSERT INTO style_tags (style_tag_id,tag_name,category) VALUES ('4a481ee2-aee6-5fb7-bbfb-8ef480608caf','페이드컷','SHORT') ON CONFLICT (style_tag_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('87408dfc-91cf-53df-bfab-369e62e93e38','4a481ee2-aee6-5fb7-bbfb-8ef480608caf') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('ccdd16a6-c616-548c-8c54-2e0fe6c6f167','1421bae9-6b73-5762-90da-f6d7e9062268',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/ccdd16a6-c616-548c-8c54-2e0fe6c6f167.jpg','image/jpeg',102651,'c47f24bcbb5d89291a4af08c67fd5c154cce7440a8096caa361f06514ebce693',1200,794,TIMESTAMPTZ '2100-01-01 00:00:00+00','투블록 보울컷.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='ccdd16a6-c616-548c-8c54-2e0fe6c6f167' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/ccdd16a6-c616-548c-8c54-2e0fe6c6f167.jpg' AND sha256='c47f24bcbb5d89291a4af08c67fd5c154cce7440a8096caa361f06514ebce693' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('b4b3e4fc-cd3e-5df2-ae97-4947e1076293','투블록 보울컷','SHORT','ccdd16a6-c616-548c-8c54-2e0fe6c6f167',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('b4b3e4fc-cd3e-5df2-ae97-4947e1076293','["CUT", "STYLING"]'::jsonb,'["SHORT", "BELOW_CHIN", "BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["STRAIGHT"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'SHORT',5,'LOW','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO style_tags (style_tag_id,tag_name,category) VALUES ('9a1ab3a1-dad0-505a-87fb-ccfea4f421e4','투블록','SHORT') ON CONFLICT (style_tag_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('b4b3e4fc-cd3e-5df2-ae97-4947e1076293','9a1ab3a1-dad0-505a-87fb-ccfea4f421e4') ON CONFLICT DO NOTHING;

INSERT INTO style_tags (style_tag_id,tag_name,category) VALUES ('24f5486e-0ce5-591a-a061-6f45e5ebb9e6','보울컷','SHORT') ON CONFLICT (style_tag_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('b4b3e4fc-cd3e-5df2-ae97-4947e1076293','24f5486e-0ce5-591a-a061-6f45e5ebb9e6') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('ad79a8cf-d74f-504e-ae76-b31e24155234','1e7208fe-9db6-50f4-86ff-6fc46edac019',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/ad79a8cf-d74f-504e-ae76-b31e24155234.jpg','image/jpeg',98689,'1ea8a2d8ef6657569f9320cd395c1213f779bbfa60dda2e59727c5a1733b89e0',1200,1800,TIMESTAMPTZ '2100-01-01 00:00:00+00','클래식 사이드파트.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='ad79a8cf-d74f-504e-ae76-b31e24155234' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/ad79a8cf-d74f-504e-ae76-b31e24155234.jpg' AND sha256='1ea8a2d8ef6657569f9320cd395c1213f779bbfa60dda2e59727c5a1733b89e0' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('b252732a-2d91-5c01-afd5-6d36f1bd3a92','클래식 사이드파트','SHORT','ad79a8cf-d74f-504e-ae76-b31e24155234',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('b252732a-2d91-5c01-afd5-6d36f1bd3a92','["CUT", "STYLING"]'::jsonb,'["SHORT", "BELOW_CHIN", "BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["STRAIGHT", "WAVY"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'SHORT',10,'MEDIUM','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO style_tags (style_tag_id,tag_name,category) VALUES ('44105ceb-49bd-507b-b1b0-2b83c7889e5e','사이드파트','SHORT') ON CONFLICT (style_tag_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('b252732a-2d91-5c01-afd5-6d36f1bd3a92','44105ceb-49bd-507b-b1b0-2b83c7889e5e') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('fe0565fa-297c-513b-80a1-111dde14aa81','8402b338-e9c2-544b-a054-1aa85685c10e',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/fe0565fa-297c-513b-80a1-111dde14aa81.jpg','image/jpeg',150811,'bd62355f481bbd647443739acf1e32ac84774039d088e4f449c1ab288c635f09',1200,1680,TIMESTAMPTZ '2100-01-01 00:00:00+00','스파이키 언더컷.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='fe0565fa-297c-513b-80a1-111dde14aa81' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/fe0565fa-297c-513b-80a1-111dde14aa81.jpg' AND sha256='bd62355f481bbd647443739acf1e32ac84774039d088e4f449c1ab288c635f09' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('86c65822-8ea7-58c4-b115-d9924435ea68','스파이키 언더컷','SHORT','fe0565fa-297c-513b-80a1-111dde14aa81',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('86c65822-8ea7-58c4-b115-d9924435ea68','["CUT", "STYLING"]'::jsonb,'["SHORT", "BELOW_CHIN", "BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["STRAIGHT", "WAVY"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'SHORT',10,'MEDIUM','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO style_tags (style_tag_id,tag_name,category) VALUES ('73108c3a-e190-5c3f-afc8-6df41dc448f4','언더컷','SHORT') ON CONFLICT (style_tag_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('86c65822-8ea7-58c4-b115-d9924435ea68','73108c3a-e190-5c3f-afc8-6df41dc448f4') ON CONFLICT DO NOTHING;

INSERT INTO style_tags (style_tag_id,tag_name,category) VALUES ('d6310cdc-9828-5180-9eb3-aa8f35f91280','스파이키컷','SHORT') ON CONFLICT (style_tag_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('86c65822-8ea7-58c4-b115-d9924435ea68','d6310cdc-9828-5180-9eb3-aa8f35f91280') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('4807dce0-83fe-53f6-87f2-3679c38cfd1b','b84fe71b-42dd-540f-98d4-e2cd61b7b630',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/4807dce0-83fe-53f6-87f2-3679c38cfd1b.jpg','image/jpeg',137009,'c3188ef87dc380f15d6ae680e51ab251f313b53a64400c84b657321cd65b984c',1200,1800,TIMESTAMPTZ '2100-01-01 00:00:00+00','슬릭백 스타일.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='4807dce0-83fe-53f6-87f2-3679c38cfd1b' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/4807dce0-83fe-53f6-87f2-3679c38cfd1b.jpg' AND sha256='c3188ef87dc380f15d6ae680e51ab251f313b53a64400c84b657321cd65b984c' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('8a9d43c6-cf38-5cc3-b038-94fe0f689de7','슬릭백 스타일','SHORT','4807dce0-83fe-53f6-87f2-3679c38cfd1b',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('8a9d43c6-cf38-5cc3-b038-94fe0f689de7','["CUT", "STYLING"]'::jsonb,'["SHORT", "BELOW_CHIN", "BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["STRAIGHT", "WAVY"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'SHORT',10,'MEDIUM','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO style_tags (style_tag_id,tag_name,category) VALUES ('01599c69-c392-5270-8080-8397be66c3c2','슬릭백','SHORT') ON CONFLICT (style_tag_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('8a9d43c6-cf38-5cc3-b038-94fe0f689de7','01599c69-c392-5270-8080-8397be66c3c2') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('8bfcc7ca-ef75-5eb4-b7cc-b417968c7ac4','b7784df8-a82e-5c32-8eda-8d6de7315471',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/8bfcc7ca-ef75-5eb4-b7cc-b417968c7ac4.jpg','image/jpeg',232465,'c9860ced9e54ed4d8e0da5d4e98b315fa9e8a1cb8a0319da98b5f3f32e4af79e',1200,1800,TIMESTAMPTZ '2100-01-01 00:00:00+00','센터파트 내추럴 플로우.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='8bfcc7ca-ef75-5eb4-b7cc-b417968c7ac4' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/8bfcc7ca-ef75-5eb4-b7cc-b417968c7ac4.jpg' AND sha256='c9860ced9e54ed4d8e0da5d4e98b315fa9e8a1cb8a0319da98b5f3f32e4af79e' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('c17d60af-6144-5d44-9658-eda2e5ad638d','센터파트 내추럴 플로우','MEDIUM','8bfcc7ca-ef75-5eb4-b7cc-b417968c7ac4',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('c17d60af-6144-5d44-9658-eda2e5ad638d','["CUT", "STYLING"]'::jsonb,'["BELOW_CHIN", "BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["STRAIGHT", "WAVY"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'BELOW_CHIN',5,'LOW','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO style_tags (style_tag_id,tag_name,category) VALUES ('122bdbdf-5ebf-5770-8c8a-c0c09e267343','가르마컷','MEDIUM') ON CONFLICT (style_tag_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('c17d60af-6144-5d44-9658-eda2e5ad638d','122bdbdf-5ebf-5770-8c8a-c0c09e267343') ON CONFLICT DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('c17d60af-6144-5d44-9658-eda2e5ad638d','20000000-0000-4000-8000-000000000002') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('6b6cd640-a67d-5c8b-b638-bd7ed7d2d15c','246e7322-0fa8-5d64-a199-7d95950ad88c',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/6b6cd640-a67d-5c8b-b638-bd7ed7d2d15c.jpg','image/jpeg',45784,'ad8d88b85e804ce6885f142592c4d910bcaf4877760e8670356297e82556f28f',1200,800,TIMESTAMPTZ '2100-01-01 00:00:00+00','커튼뱅 리프컷.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='6b6cd640-a67d-5c8b-b638-bd7ed7d2d15c' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/6b6cd640-a67d-5c8b-b638-bd7ed7d2d15c.jpg' AND sha256='ad8d88b85e804ce6885f142592c4d910bcaf4877760e8670356297e82556f28f' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('06cc4930-e540-5d6f-b284-1e08b1818a1c','커튼뱅 리프컷','MEDIUM','6b6cd640-a67d-5c8b-b638-bd7ed7d2d15c',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('06cc4930-e540-5d6f-b284-1e08b1818a1c','["CUT", "STYLING"]'::jsonb,'["BELOW_CHIN", "BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["STRAIGHT", "WAVY"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'BELOW_CHIN',10,'MEDIUM','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('06cc4930-e540-5d6f-b284-1e08b1818a1c','20000000-0000-4000-8000-000000000002') ON CONFLICT DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('06cc4930-e540-5d6f-b284-1e08b1818a1c','10000000-0000-4000-8000-000000000003') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('8da7f475-29f4-5218-9572-f1bc81b89985','d39c2916-e45c-5ac8-97a2-847a0731519b',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/8da7f475-29f4-5218-9572-f1bc81b89985.jpg','image/jpeg',204630,'a60c0ebd24d1ef0726b65b5e50ef212b4b3fbe1b4f03beae2cb355b41de1bf30',1200,1800,TIMESTAMPTZ '2100-01-01 00:00:00+00','옴브레 롱 글램 웨이브.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='8da7f475-29f4-5218-9572-f1bc81b89985' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/8da7f475-29f4-5218-9572-f1bc81b89985.jpg' AND sha256='a60c0ebd24d1ef0726b65b5e50ef212b4b3fbe1b4f03beae2cb355b41de1bf30' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('76145924-61ee-52ca-8fb6-faf5392da1d5','옴브레 롱 글램 웨이브','LONG','8da7f475-29f4-5218-9572-f1bc81b89985',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('76145924-61ee-52ca-8fb6-faf5392da1d5','["CUT", "STYLING"]'::jsonb,'["BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["STRAIGHT", "WAVY"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'BELOW_SHOULDER',15,'MEDIUM','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('76145924-61ee-52ca-8fb6-faf5392da1d5','50000000-0000-4000-8000-000000000001') ON CONFLICT DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('76145924-61ee-52ca-8fb6-faf5392da1d5','50000000-0000-4000-8000-000000000004') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('830fef5a-219b-5a3a-894a-9c501d7f2ee4','43444ef5-7aad-5467-9f51-5e8b5a2203ee',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/830fef5a-219b-5a3a-894a-9c501d7f2ee4.jpg','image/jpeg',159658,'78b483d2523615d352db726eb7d70ac35142265d934256695c2f48076be56fcf',1200,1800,TIMESTAMPTZ '2100-01-01 00:00:00+00','사이드뱅 롱 레이어드.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='830fef5a-219b-5a3a-894a-9c501d7f2ee4' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/830fef5a-219b-5a3a-894a-9c501d7f2ee4.jpg' AND sha256='78b483d2523615d352db726eb7d70ac35142265d934256695c2f48076be56fcf' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('447ef348-7a08-5565-b345-de7ba9aa77fd','사이드뱅 롱 레이어드','LONG','830fef5a-219b-5a3a-894a-9c501d7f2ee4',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('447ef348-7a08-5565-b345-de7ba9aa77fd','["CUT", "STYLING"]'::jsonb,'["BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["STRAIGHT", "WAVY"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'BELOW_SHOULDER',15,'MEDIUM','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('447ef348-7a08-5565-b345-de7ba9aa77fd','50000000-0000-4000-8000-000000000001') ON CONFLICT DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('447ef348-7a08-5565-b345-de7ba9aa77fd','10000000-0000-4000-8000-000000000003') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('ed1707b3-54c0-59a0-b921-f80d7d84beca','19711382-8e2e-585e-94cb-4a692c94b97d',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/ed1707b3-54c0-59a0-b921-f80d7d84beca.jpg','image/jpeg',251228,'8660c7d1dbadccb968483b72fe6e1b37ba016c39621d00ef3994c37d9d6fcf62',1200,1800,TIMESTAMPTZ '2100-01-01 00:00:00+00','발레아쥬 중단발 웨이브.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='ed1707b3-54c0-59a0-b921-f80d7d84beca' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/ed1707b3-54c0-59a0-b921-f80d7d84beca.jpg' AND sha256='8660c7d1dbadccb968483b72fe6e1b37ba016c39621d00ef3994c37d9d6fcf62' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('cbf47ccd-6a33-5cc1-963d-6dcd61fd0189','발레아쥬 중단발 웨이브','MEDIUM','ed1707b3-54c0-59a0-b921-f80d7d84beca',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('cbf47ccd-6a33-5cc1-963d-6dcd61fd0189','["CUT", "STYLING"]'::jsonb,'["BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["STRAIGHT", "WAVY"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'BELOW_SHOULDER',15,'MEDIUM','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('cbf47ccd-6a33-5cc1-963d-6dcd61fd0189','40000000-0000-4000-8000-000000000001') ON CONFLICT DO NOTHING;

INSERT INTO style_tags (style_tag_id,tag_name,category) VALUES ('2c517e2b-986e-5bde-8744-9ade9b5f9a7d','웨이브','MEDIUM') ON CONFLICT (style_tag_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('cbf47ccd-6a33-5cc1-963d-6dcd61fd0189','2c517e2b-986e-5bde-8744-9ade9b5f9a7d') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('0aa49435-f662-5367-9c31-f2d70c4cbf18','d49ffc6f-8f9d-5181-a1c4-983910a11f9d',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/0aa49435-f662-5367-9c31-f2d70c4cbf18.jpg','image/jpeg',338540,'6cb04d4009bccb8ba729f368320ab46a9e34186b7ec577efdbe3a4feb18b5931',1200,1800,TIMESTAMPTZ '2100-01-01 00:00:00+00','페이스프레이밍 레이어드.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='0aa49435-f662-5367-9c31-f2d70c4cbf18' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/0aa49435-f662-5367-9c31-f2d70c4cbf18.jpg' AND sha256='6cb04d4009bccb8ba729f368320ab46a9e34186b7ec577efdbe3a4feb18b5931' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('02310a32-527e-599b-aafd-a64c63398d68','페이스프레이밍 레이어드','LONG','0aa49435-f662-5367-9c31-f2d70c4cbf18',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('02310a32-527e-599b-aafd-a64c63398d68','["CUT", "STYLING"]'::jsonb,'["BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["STRAIGHT", "WAVY"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'BELOW_SHOULDER',15,'MEDIUM','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('02310a32-527e-599b-aafd-a64c63398d68','50000000-0000-4000-8000-000000000001') ON CONFLICT DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('02310a32-527e-599b-aafd-a64c63398d68','10000000-0000-4000-8000-000000000003') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('12eea25f-cad4-59ce-9f21-18a8f268adc2','edb50534-704a-530e-9cdf-190477b5c48b',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/12eea25f-cad4-59ce-9f21-18a8f268adc2.jpg','image/jpeg',195920,'fc5e36eb6318655c3f440677e040d8390258759ddb73031960ad2c7624d4661b',1200,1800,TIMESTAMPTZ '2100-01-01 00:00:00+00','커튼뱅 롱 스트레이트.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='12eea25f-cad4-59ce-9f21-18a8f268adc2' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/12eea25f-cad4-59ce-9f21-18a8f268adc2.jpg' AND sha256='fc5e36eb6318655c3f440677e040d8390258759ddb73031960ad2c7624d4661b' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('4972ab0a-e635-58d1-85e3-9889c6cce4a7','커튼뱅 롱 스트레이트','LONG','12eea25f-cad4-59ce-9f21-18a8f268adc2',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('4972ab0a-e635-58d1-85e3-9889c6cce4a7','["CUT", "STYLING"]'::jsonb,'["BELOW_CHEST"]'::jsonb,'["STRAIGHT"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'BELOW_CHEST',10,'MEDIUM','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('4972ab0a-e635-58d1-85e3-9889c6cce4a7','50000000-0000-4000-8000-000000000002') ON CONFLICT DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('4972ab0a-e635-58d1-85e3-9889c6cce4a7','10000000-0000-4000-8000-000000000003') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('cc7d31a0-a676-5d7c-a8d6-ee960a834587','82fd3751-228a-522e-a492-276ef3dec3f3',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/cc7d31a0-a676-5d7c-a8d6-ee960a834587.jpg','image/jpeg',167927,'bce3e0bbc5aa0acef47f2707a162a40b41074f2c64e8025f57612cb7f78f3c39',1200,1800,TIMESTAMPTZ '2100-01-01 00:00:00+00','블런트 프린지 픽시.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='cc7d31a0-a676-5d7c-a8d6-ee960a834587' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/cc7d31a0-a676-5d7c-a8d6-ee960a834587.jpg' AND sha256='bce3e0bbc5aa0acef47f2707a162a40b41074f2c64e8025f57612cb7f78f3c39' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('42d2fb31-6285-5aa4-864f-8fbe10e75a5c','블런트 프린지 픽시','SHORT','cc7d31a0-a676-5d7c-a8d6-ee960a834587',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('42d2fb31-6285-5aa4-864f-8fbe10e75a5c','["CUT", "STYLING"]'::jsonb,'["SHORT", "BELOW_CHIN", "BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["STRAIGHT"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'SHORT',5,'LOW','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('42d2fb31-6285-5aa4-864f-8fbe10e75a5c','20000000-0000-4000-8000-000000000001') ON CONFLICT DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('42d2fb31-6285-5aa4-864f-8fbe10e75a5c','10000000-0000-4000-8000-000000000004') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('d37fc871-3166-573f-bd37-579e9fd9d993','f4191642-47a1-5369-8e2c-541ed28ad0ff',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/d37fc871-3166-573f-bd37-579e9fd9d993.jpg','image/jpeg',219156,'7beae5de025bf361346eef67d276513ef303d49882432601a5c4e79e974aa8bf',1200,1800,TIMESTAMPTZ '2100-01-01 00:00:00+00','볼륨 텍스처 픽시.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='d37fc871-3166-573f-bd37-579e9fd9d993' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/d37fc871-3166-573f-bd37-579e9fd9d993.jpg' AND sha256='7beae5de025bf361346eef67d276513ef303d49882432601a5c4e79e974aa8bf' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('f055858a-07fe-5a79-a257-b7ccc3dab7f8','볼륨 텍스처 픽시','SHORT','d37fc871-3166-573f-bd37-579e9fd9d993',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('f055858a-07fe-5a79-a257-b7ccc3dab7f8','["CUT", "STYLING"]'::jsonb,'["SHORT", "BELOW_CHIN", "BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["STRAIGHT", "WAVY"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'SHORT',10,'MEDIUM','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('f055858a-07fe-5a79-a257-b7ccc3dab7f8','20000000-0000-4000-8000-000000000001') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('c1d14fa0-b538-5746-84bd-0ccaf03a54f0','0404e286-42d3-5a11-8a27-4ebb523c9daf',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/c1d14fa0-b538-5746-84bd-0ccaf03a54f0.jpg','image/jpeg',126254,'12695486bf01e632a74f2a83aa2ff16c261e6bf766209cde39a2213d43486cf8',1200,1600,TIMESTAMPTZ '2100-01-01 00:00:00+00','사이드 프린지 픽시.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='c1d14fa0-b538-5746-84bd-0ccaf03a54f0' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/c1d14fa0-b538-5746-84bd-0ccaf03a54f0.jpg' AND sha256='12695486bf01e632a74f2a83aa2ff16c261e6bf766209cde39a2213d43486cf8' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('8167d07c-eb7e-5632-878a-0df3e0ac5ee7','사이드 프린지 픽시','SHORT','c1d14fa0-b538-5746-84bd-0ccaf03a54f0',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('8167d07c-eb7e-5632-878a-0df3e0ac5ee7','["CUT", "STYLING"]'::jsonb,'["SHORT", "BELOW_CHIN", "BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["STRAIGHT", "WAVY"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'SHORT',5,'LOW','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('8167d07c-eb7e-5632-878a-0df3e0ac5ee7','20000000-0000-4000-8000-000000000001') ON CONFLICT DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('8167d07c-eb7e-5632-878a-0df3e0ac5ee7','10000000-0000-4000-8000-000000000003') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('1f8d371f-ba06-5d7d-9425-d5afe5a0c36a','098cfb06-257a-5740-b3c9-8d0af011a90d',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/1f8d371f-ba06-5d7d-9425-d5afe5a0c36a.jpg','image/jpeg',180375,'728482538e48a69a5ae57f398314f8fb41c58401c2489ee3c7178cbab0cd0c38',1200,1600,TIMESTAMPTZ '2100-01-01 00:00:00+00','마이크로 프린지 숏컷.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='1f8d371f-ba06-5d7d-9425-d5afe5a0c36a' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/1f8d371f-ba06-5d7d-9425-d5afe5a0c36a.jpg' AND sha256='728482538e48a69a5ae57f398314f8fb41c58401c2489ee3c7178cbab0cd0c38' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('6abf9427-67ac-5754-a2f3-a29c50a8f4b6','마이크로 프린지 숏컷','SHORT','1f8d371f-ba06-5d7d-9425-d5afe5a0c36a',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('6abf9427-67ac-5754-a2f3-a29c50a8f4b6','["CUT", "STYLING"]'::jsonb,'["SHORT", "BELOW_CHIN", "BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["STRAIGHT", "WAVY"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'SHORT',5,'LOW','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('6abf9427-67ac-5754-a2f3-a29c50a8f4b6','20000000-0000-4000-8000-000000000001') ON CONFLICT DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('6abf9427-67ac-5754-a2f3-a29c50a8f4b6','10000000-0000-4000-8000-000000000004') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('6d90b2d5-8a82-5966-b7af-60c76f6a8525','5013a16d-a2d8-5292-975c-17bd8373dfa3',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/6d90b2d5-8a82-5966-b7af-60c76f6a8525.jpg','image/jpeg',224126,'ae0e8b2ba3ce49268254c6758b04428c9a9ce06ca75ff2ad2efeb8ec763985a1',1200,1799,TIMESTAMPTZ '2100-01-01 00:00:00+00','비대칭 언더컷 픽시.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='6d90b2d5-8a82-5966-b7af-60c76f6a8525' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/6d90b2d5-8a82-5966-b7af-60c76f6a8525.jpg' AND sha256='ae0e8b2ba3ce49268254c6758b04428c9a9ce06ca75ff2ad2efeb8ec763985a1' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('b1fdcdba-f935-5f0d-8fba-e9b56f3003fc','비대칭 언더컷 픽시','SHORT','6d90b2d5-8a82-5966-b7af-60c76f6a8525',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('b1fdcdba-f935-5f0d-8fba-e9b56f3003fc','["CUT", "STYLING"]'::jsonb,'["SHORT", "BELOW_CHIN", "BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["STRAIGHT"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'SHORT',10,'MEDIUM','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('b1fdcdba-f935-5f0d-8fba-e9b56f3003fc','20000000-0000-4000-8000-000000000001') ON CONFLICT DO NOTHING;

INSERT INTO style_tags (style_tag_id,tag_name,category) VALUES ('73108c3a-e190-5c3f-afc8-6df41dc448f4','언더컷','SHORT') ON CONFLICT (style_tag_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('b1fdcdba-f935-5f0d-8fba-e9b56f3003fc','73108c3a-e190-5c3f-afc8-6df41dc448f4') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('6c2c27c3-210a-5ca4-b322-09aad446f639','3c4f624b-a12f-5b4b-846e-e059ee90b8f3',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/6c2c27c3-210a-5ca4-b322-09aad446f639.jpg','image/jpeg',209288,'a3fb86a403dcd5115dca583bd3e9858035b68955e445588a91c7fe9c7fe864af',1200,1520,TIMESTAMPTZ '2100-01-01 00:00:00+00','블런트 보브.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='6c2c27c3-210a-5ca4-b322-09aad446f639' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/6c2c27c3-210a-5ca4-b322-09aad446f639.jpg' AND sha256='a3fb86a403dcd5115dca583bd3e9858035b68955e445588a91c7fe9c7fe864af' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('e0ef9b77-ea49-5d67-80da-673fc665e394','블런트 보브','BOB','6c2c27c3-210a-5ca4-b322-09aad446f639',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('e0ef9b77-ea49-5d67-80da-673fc665e394','["CUT", "STYLING"]'::jsonb,'["BELOW_CHIN", "BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["STRAIGHT"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'BELOW_CHIN',5,'LOW','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('e0ef9b77-ea49-5d67-80da-673fc665e394','30000000-0000-4000-8000-000000000001') ON CONFLICT DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('e0ef9b77-ea49-5d67-80da-673fc665e394','30000000-0000-4000-8000-000000000002') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('b9712588-5c6d-5aeb-b826-3db036524a04','94b77999-44b8-58dd-ba91-8c5b81a16b11',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/b9712588-5c6d-5aeb-b826-3db036524a04.jpg','image/jpeg',174509,'4754adc620a9adb8319e6abd26f1a1bd803ecfae9bb13da2f3b329b9c7fea792',1200,1961,TIMESTAMPTZ '2100-01-01 00:00:00+00','시스루뱅 보브.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='b9712588-5c6d-5aeb-b826-3db036524a04' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/b9712588-5c6d-5aeb-b826-3db036524a04.jpg' AND sha256='4754adc620a9adb8319e6abd26f1a1bd803ecfae9bb13da2f3b329b9c7fea792' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('ffe39c84-1b70-58f0-979c-b7474c76888a','시스루뱅 보브','BOB','b9712588-5c6d-5aeb-b826-3db036524a04',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('ffe39c84-1b70-58f0-979c-b7474c76888a','["CUT", "STYLING"]'::jsonb,'["BELOW_CHIN", "BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["STRAIGHT"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'BELOW_CHIN',5,'LOW','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('ffe39c84-1b70-58f0-979c-b7474c76888a','30000000-0000-4000-8000-000000000001') ON CONFLICT DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('ffe39c84-1b70-58f0-979c-b7474c76888a','10000000-0000-4000-8000-000000000001') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('4333aaa0-02ed-531d-b768-300535e05b0d','8c9e83bb-3dc1-5e1e-9d5d-870c7f4eff78',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/4333aaa0-02ed-531d-b768-300535e05b0d.jpg','image/jpeg',121672,'79caeede28822f77a66d1756b7efba5b8e0a9ff26f148c4cce89c3f2cf981c0a',1200,1500,TIMESTAMPTZ '2100-01-01 00:00:00+00','사이드뱅 클래식 보브.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='4333aaa0-02ed-531d-b768-300535e05b0d' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/4333aaa0-02ed-531d-b768-300535e05b0d.jpg' AND sha256='79caeede28822f77a66d1756b7efba5b8e0a9ff26f148c4cce89c3f2cf981c0a' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('16c7597c-2c40-51e1-a754-ef4fe35aa2b3','사이드뱅 클래식 보브','BOB','4333aaa0-02ed-531d-b768-300535e05b0d',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('16c7597c-2c40-51e1-a754-ef4fe35aa2b3','["CUT", "STYLING"]'::jsonb,'["BELOW_CHIN", "BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["STRAIGHT"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'BELOW_CHIN',5,'LOW','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('16c7597c-2c40-51e1-a754-ef4fe35aa2b3','30000000-0000-4000-8000-000000000001') ON CONFLICT DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('16c7597c-2c40-51e1-a754-ef4fe35aa2b3','10000000-0000-4000-8000-000000000003') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('79ab601a-0ef7-5fc1-ae0d-f01f12d69fd9','9cf167c2-79cd-5bd6-8e6f-f9b12596e478',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/79ab601a-0ef7-5fc1-ae0d-f01f12d69fd9.jpg','image/jpeg',249953,'22ee56b41a12d65b621d0ee0a00bdc18a5ab039c1594abffcb643aec1d0179dc',1200,1800,TIMESTAMPTZ '2100-01-01 00:00:00+00','사이드파트 원랭스 보브.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='79ab601a-0ef7-5fc1-ae0d-f01f12d69fd9' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/79ab601a-0ef7-5fc1-ae0d-f01f12d69fd9.jpg' AND sha256='22ee56b41a12d65b621d0ee0a00bdc18a5ab039c1594abffcb643aec1d0179dc' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('c9442c21-485b-5d64-87c3-a97b491e3702','사이드파트 원랭스 보브','BOB','79ab601a-0ef7-5fc1-ae0d-f01f12d69fd9',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('c9442c21-485b-5d64-87c3-a97b491e3702','["CUT", "STYLING"]'::jsonb,'["BELOW_CHIN", "BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["STRAIGHT"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'BELOW_CHIN',5,'LOW','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('c9442c21-485b-5d64-87c3-a97b491e3702','30000000-0000-4000-8000-000000000001') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('61f943fd-f574-5894-8ea6-916149971fae','6c3a844d-1561-523a-8fc5-4594a020b2c8',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/61f943fd-f574-5894-8ea6-916149971fae.jpg','image/jpeg',133130,'c1e880d9eef714180b0313210e75bf39841b2716c1dc658ffefaf1a198d2c080',1200,1800,TIMESTAMPTZ '2100-01-01 00:00:00+00','센터파트 슬릭 보브.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='61f943fd-f574-5894-8ea6-916149971fae' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/61f943fd-f574-5894-8ea6-916149971fae.jpg' AND sha256='c1e880d9eef714180b0313210e75bf39841b2716c1dc658ffefaf1a198d2c080' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('e01d6760-17b8-5cd4-97de-c604322e304d','센터파트 슬릭 보브','BOB','61f943fd-f574-5894-8ea6-916149971fae',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('e01d6760-17b8-5cd4-97de-c604322e304d','["CUT", "STYLING"]'::jsonb,'["BELOW_CHIN", "BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["STRAIGHT"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'BELOW_CHIN',5,'LOW','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('e01d6760-17b8-5cd4-97de-c604322e304d','30000000-0000-4000-8000-000000000001') ON CONFLICT DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('e01d6760-17b8-5cd4-97de-c604322e304d','30000000-0000-4000-8000-000000000002') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('917bcbd1-29cb-514c-95ab-1f6240f36631','3a5550dd-50a0-5f36-a02f-f69c8aa75b3f',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/917bcbd1-29cb-514c-95ab-1f6240f36631.jpg','image/jpeg',196822,'372cac673947612bda42e1ccba01e005437c09532b188abffb1869e0d27067c2',1200,1800,TIMESTAMPTZ '2100-01-01 00:00:00+00','커튼 레이어드 블로아웃.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='917bcbd1-29cb-514c-95ab-1f6240f36631' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/917bcbd1-29cb-514c-95ab-1f6240f36631.jpg' AND sha256='372cac673947612bda42e1ccba01e005437c09532b188abffb1869e0d27067c2' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('abcb8ccb-9044-5c1b-954e-2c222fc5a701','커튼 레이어드 블로아웃','LONG','917bcbd1-29cb-514c-95ab-1f6240f36631',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('abcb8ccb-9044-5c1b-954e-2c222fc5a701','["CUT", "STYLING"]'::jsonb,'["BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["STRAIGHT", "WAVY"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'BELOW_SHOULDER',15,'MEDIUM','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('abcb8ccb-9044-5c1b-954e-2c222fc5a701','50000000-0000-4000-8000-000000000001') ON CONFLICT DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('abcb8ccb-9044-5c1b-954e-2c222fc5a701','10000000-0000-4000-8000-000000000003') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('6c25c799-986b-5296-a5fb-2414409f7353','3e6d548d-9ea9-5c71-9184-6c2db305744c',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/6c25c799-986b-5296-a5fb-2414409f7353.jpg','image/jpeg',277049,'05e8acc2e2a72b61379abcd9304f2eb06a4cfe2260ba2ad801a2a663f6822c49',1200,1800,TIMESTAMPTZ '2100-01-01 00:00:00+00','하이라이트 롱 S컬.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='6c25c799-986b-5296-a5fb-2414409f7353' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/6c25c799-986b-5296-a5fb-2414409f7353.jpg' AND sha256='05e8acc2e2a72b61379abcd9304f2eb06a4cfe2260ba2ad801a2a663f6822c49' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('fb32cea4-c14f-5c7e-a0d0-2225caf4361c','하이라이트 롱 S컬','LONG','6c25c799-986b-5296-a5fb-2414409f7353',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('fb32cea4-c14f-5c7e-a0d0-2225caf4361c','["CUT", "STYLING"]'::jsonb,'["BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["STRAIGHT", "WAVY"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'BELOW_SHOULDER',15,'MEDIUM','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('fb32cea4-c14f-5c7e-a0d0-2225caf4361c','50000000-0000-4000-8000-000000000001') ON CONFLICT DO NOTHING;

INSERT INTO style_tags (style_tag_id,tag_name,category) VALUES ('2c517e2b-986e-5bde-8744-9ade9b5f9a7d','웨이브','LONG') ON CONFLICT (style_tag_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('fb32cea4-c14f-5c7e-a0d0-2225caf4361c','2c517e2b-986e-5bde-8744-9ade9b5f9a7d') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('144afb9a-a898-5f3d-adc1-33b43c08b879','7448211d-037e-5794-9f4b-5b70c2d81f8e',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/144afb9a-a898-5f3d-adc1-33b43c08b879.jpg','image/jpeg',192811,'1811475fed1dffb1f1e8dbf3fdd9938de9e548c9facb36e1f6d39a8aa7b697a9',1200,1800,TIMESTAMPTZ '2100-01-01 00:00:00+00','롱 레이어드 C컬.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='144afb9a-a898-5f3d-adc1-33b43c08b879' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/144afb9a-a898-5f3d-adc1-33b43c08b879.jpg' AND sha256='1811475fed1dffb1f1e8dbf3fdd9938de9e548c9facb36e1f6d39a8aa7b697a9' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('5b2448da-3946-5de8-9e22-a1584575acda','롱 레이어드 C컬','LONG','144afb9a-a898-5f3d-adc1-33b43c08b879',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('5b2448da-3946-5de8-9e22-a1584575acda','["CUT", "STYLING"]'::jsonb,'["BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["STRAIGHT", "WAVY"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'BELOW_SHOULDER',10,'MEDIUM','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('5b2448da-3946-5de8-9e22-a1584575acda','50000000-0000-4000-8000-000000000001') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('621b4131-8d40-57a6-8c47-2a6d665b476d','803d8165-0bb4-5d47-97cf-233b62cbe55a',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/621b4131-8d40-57a6-8c47-2a6d665b476d.jpg','image/jpeg',189980,'ec0c264b5230985b024326a8ecfc1cfc3fe002a01ac022c4d83abc9c7a260e55',1200,1800,TIMESTAMPTZ '2100-01-01 00:00:00+00','롱 페더드 레이어드.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='621b4131-8d40-57a6-8c47-2a6d665b476d' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/621b4131-8d40-57a6-8c47-2a6d665b476d.jpg' AND sha256='ec0c264b5230985b024326a8ecfc1cfc3fe002a01ac022c4d83abc9c7a260e55' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('c0dcc01b-d923-56b0-ba41-3e7f060dac05','롱 페더드 레이어드','LONG','621b4131-8d40-57a6-8c47-2a6d665b476d',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('c0dcc01b-d923-56b0-ba41-3e7f060dac05','["CUT", "STYLING"]'::jsonb,'["BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["STRAIGHT", "WAVY"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'BELOW_SHOULDER',15,'MEDIUM','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('c0dcc01b-d923-56b0-ba41-3e7f060dac05','50000000-0000-4000-8000-000000000001') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('e51b4c2e-ce5e-5817-baff-1280bd64ccf5','06255242-2663-52ee-8248-ed33424e818f',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/e51b4c2e-ce5e-5817-baff-1280bd64ccf5.jpg','image/jpeg',228745,'1ada903281efa4a7723b76708b25db14e60e4e80de4fe9984c2017a55a404d3a',1200,1800,TIMESTAMPTZ '2100-01-01 00:00:00+00','볼륨 레이어드 블로아웃.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='e51b4c2e-ce5e-5817-baff-1280bd64ccf5' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/e51b4c2e-ce5e-5817-baff-1280bd64ccf5.jpg' AND sha256='1ada903281efa4a7723b76708b25db14e60e4e80de4fe9984c2017a55a404d3a' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('cc25d0ff-ec17-5eab-9dc3-95b0fcc14ad0','볼륨 레이어드 블로아웃','MEDIUM','e51b4c2e-ce5e-5817-baff-1280bd64ccf5',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('cc25d0ff-ec17-5eab-9dc3-95b0fcc14ad0','["CUT", "STYLING"]'::jsonb,'["BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["STRAIGHT", "WAVY"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'BELOW_SHOULDER',15,'MEDIUM','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('cc25d0ff-ec17-5eab-9dc3-95b0fcc14ad0','40000000-0000-4000-8000-000000000001') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('670329bd-c450-504d-9598-9fea62599e72','ecc49cc9-b20f-52d5-934f-111ff12e24b2',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/670329bd-c450-504d-9598-9fea62599e72.jpg','image/jpeg',215473,'4f9e457294dafd0582ef81708770f93c3974a6cacdaaf5faa164c8df9e53dead',1200,1800,TIMESTAMPTZ '2100-01-01 00:00:00+00','사이드뱅 미디엄 C컬.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='670329bd-c450-504d-9598-9fea62599e72' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/670329bd-c450-504d-9598-9fea62599e72.jpg' AND sha256='4f9e457294dafd0582ef81708770f93c3974a6cacdaaf5faa164c8df9e53dead' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('31717139-b772-5190-95ad-bdad71d6aaed','사이드뱅 미디엄 C컬','MEDIUM','670329bd-c450-504d-9598-9fea62599e72',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('31717139-b772-5190-95ad-bdad71d6aaed','["CUT", "STYLING"]'::jsonb,'["BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["STRAIGHT", "WAVY"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'BELOW_SHOULDER',10,'MEDIUM','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('31717139-b772-5190-95ad-bdad71d6aaed','40000000-0000-4000-8000-000000000001') ON CONFLICT DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('31717139-b772-5190-95ad-bdad71d6aaed','10000000-0000-4000-8000-000000000003') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('56bba053-3ad3-562c-b81f-c46044e26049','bc7e5dca-dc8a-5992-bad0-24f87ad7ee32',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/56bba053-3ad3-562c-b81f-c46044e26049.jpg','image/jpeg',210998,'0ae6ff3c97af5b38607afdb24e3f59688f7c0bb81edbbf8d982b67656249bfd2',1200,1804,TIMESTAMPTZ '2100-01-01 00:00:00+00','롱 캐스케이드 웨이브.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='56bba053-3ad3-562c-b81f-c46044e26049' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/56bba053-3ad3-562c-b81f-c46044e26049.jpg' AND sha256='0ae6ff3c97af5b38607afdb24e3f59688f7c0bb81edbbf8d982b67656249bfd2' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('9dde44be-0010-5f35-880b-35075b1c9ca4','롱 캐스케이드 웨이브','LONG','56bba053-3ad3-562c-b81f-c46044e26049',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('9dde44be-0010-5f35-880b-35075b1c9ca4','["CUT", "STYLING"]'::jsonb,'["BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["STRAIGHT", "WAVY"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'BELOW_SHOULDER',15,'MEDIUM','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('9dde44be-0010-5f35-880b-35075b1c9ca4','50000000-0000-4000-8000-000000000001') ON CONFLICT DO NOTHING;

INSERT INTO style_tags (style_tag_id,tag_name,category) VALUES ('2c517e2b-986e-5bde-8744-9ade9b5f9a7d','웨이브','LONG') ON CONFLICT (style_tag_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('9dde44be-0010-5f35-880b-35075b1c9ca4','2c517e2b-986e-5bde-8744-9ade9b5f9a7d') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('5657847a-f497-5fd1-8ca5-7bcb0353e152','695307d0-7ace-5d07-a337-6f68f09ed9bd',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/5657847a-f497-5fd1-8ca5-7bcb0353e152.jpg','image/jpeg',295705,'44f24d6b508713f08c7f9c52f34da78d807467dc44422d47cdbbe768c2e25ee7',1200,1800,TIMESTAMPTZ '2100-01-01 00:00:00+00','사이드뱅 페더드컷.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='5657847a-f497-5fd1-8ca5-7bcb0353e152' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/5657847a-f497-5fd1-8ca5-7bcb0353e152.jpg' AND sha256='44f24d6b508713f08c7f9c52f34da78d807467dc44422d47cdbbe768c2e25ee7' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('5aa1cd83-3951-5ae7-999f-ddd765f75108','사이드뱅 페더드컷','LONG','5657847a-f497-5fd1-8ca5-7bcb0353e152',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('5aa1cd83-3951-5ae7-999f-ddd765f75108','["CUT", "STYLING"]'::jsonb,'["BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["STRAIGHT", "WAVY"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'BELOW_SHOULDER',15,'MEDIUM','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('5aa1cd83-3951-5ae7-999f-ddd765f75108','50000000-0000-4000-8000-000000000001') ON CONFLICT DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('5aa1cd83-3951-5ae7-999f-ddd765f75108','10000000-0000-4000-8000-000000000003') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('3763f094-afc0-5b98-9160-4ef220ddae94','474249ec-b38e-5af8-b4d8-6f3d50fe4db1',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/3763f094-afc0-5b98-9160-4ef220ddae94.jpg','image/jpeg',242682,'f1cc1f414b115d6e9ee4d18724a8fc4d5af265f094887b0a207f368ce07a0107',1200,1800,TIMESTAMPTZ '2100-01-01 00:00:00+00','미디엄 페더드 C컬.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='3763f094-afc0-5b98-9160-4ef220ddae94' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/3763f094-afc0-5b98-9160-4ef220ddae94.jpg' AND sha256='f1cc1f414b115d6e9ee4d18724a8fc4d5af265f094887b0a207f368ce07a0107' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('e4eff9f7-6ee8-5023-95c1-fdbad8a771e7','미디엄 페더드 C컬','MEDIUM','3763f094-afc0-5b98-9160-4ef220ddae94',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('e4eff9f7-6ee8-5023-95c1-fdbad8a771e7','["CUT", "STYLING"]'::jsonb,'["BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["STRAIGHT", "WAVY"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'BELOW_SHOULDER',10,'MEDIUM','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('e4eff9f7-6ee8-5023-95c1-fdbad8a771e7','40000000-0000-4000-8000-000000000001') ON CONFLICT DO NOTHING;

INSERT INTO files (file_id,upload_id,user_id,owner_type,purpose,status,object_key,content_type,file_size,sha256,width,height,expires_at,file_name) VALUES ('4f08b7f6-72c1-50be-aa2d-a9f31ef73973','204febd9-3b29-5b90-b6c2-2e3dc2fcff46',NULL,'SYSTEM','HAIRSTYLE_THUMBNAIL','READY','HAIRSTYLE_THUMBNAIL/curated-20261016/4f08b7f6-72c1-50be-aa2d-a9f31ef73973.jpg','image/jpeg',56767,'2bca2225c39b9b46ff61db5584a9cbbd4ba86ca815443eedb65ce01de7d9fc51',1200,675,TIMESTAMPTZ '2100-01-01 00:00:00+00','숏 샤기 멀릿컷.jpg') ON CONFLICT (file_id) DO NOTHING;

DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM files WHERE file_id='4f08b7f6-72c1-50be-aa2d-a9f31ef73973' AND owner_type='SYSTEM' AND object_key='HAIRSTYLE_THUMBNAIL/curated-20261016/4f08b7f6-72c1-50be-aa2d-a9f31ef73973.jpg' AND sha256='2bca2225c39b9b46ff61db5584a9cbbd4ba86ca815443eedb65ce01de7d9fc51' AND status='READY') THEN RAISE EXCEPTION 'File identity conflict'; END IF; END $$;

INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES ('2a7afeef-18a9-5e09-9516-a248e68b4090','숏 샤기 멀릿컷','MEDIUM','4f08b7f6-72c1-50be-aa2d-a9f31ef73973',true,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_recommendation_profiles (hairstyle_id,service_types,compatible_hair_lengths,compatible_hair_types,compatible_hair_thicknesses,compatible_hair_conditions,contraindicated_hair_conditions,minimum_hair_length,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,editorial_priority,metadata_version) VALUES ('2a7afeef-18a9-5e09-9516-a248e68b4090','["CUT", "STYLING"]'::jsonb,'["BELOW_CHIN", "BELOW_SHOULDER", "BELOW_CHEST"]'::jsonb,'["STRAIGHT", "WAVY"]'::jsonb,'["THIN", "NORMAL", "THICK"]'::jsonb,'["HEALTHY", "NORMAL", "DAMAGED", "SEVERELY_DAMAGED"]'::jsonb,'[]'::jsonb,'BELOW_CHIN',10,'MEDIUM','LOW',50,'curated-20261016') ON CONFLICT (hairstyle_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('2a7afeef-18a9-5e09-9516-a248e68b4090','40000000-0000-4000-8000-000000000004') ON CONFLICT DO NOTHING;

INSERT INTO style_tags (style_tag_id,tag_name,category) VALUES ('3e2e412d-ee7a-5e5e-b9ae-ad080ca70269','멀릿컷','MEDIUM') ON CONFLICT (style_tag_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('2a7afeef-18a9-5e09-9516-a248e68b4090','3e2e412d-ee7a-5e5e-b9ae-ad080ca70269') ON CONFLICT DO NOTHING;

DELETE FROM hairstyle_style_tags WHERE hairstyle_id='71000000-0000-4000-8000-000000000066';

INSERT INTO style_tags (style_tag_id,tag_name,category) VALUES ('38808b16-4f6b-563b-981c-90ccc32609a1','콘로우','UPDO') ON CONFLICT (style_tag_id) DO NOTHING;

INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id) VALUES ('71000000-0000-4000-8000-000000000066','38808b16-4f6b-563b-981c-90ccc32609a1'),('71000000-0000-4000-8000-000000000066','60000000-0000-4000-8000-000000000001') ON CONFLICT DO NOTHING;

UPDATE hairstyle_assets SET active=false,updated_at=now() WHERE hairstyle_id IN ('71000000-0000-4000-8000-000000000068'::uuid,'71000000-0000-4000-8000-000000000075'::uuid,'71000000-0000-4000-8000-000000000076'::uuid,'71000000-0000-4000-8000-000000000077'::uuid,'71000000-0000-4000-8000-000000000078'::uuid,'71000000-0000-4000-8000-000000000001'::uuid,'71000000-0000-4000-8000-000000000023'::uuid,'71000000-0000-4000-8000-000000000027'::uuid,'71000000-0000-4000-8000-000000000028'::uuid,'71000000-0000-4000-8000-000000000030'::uuid,'71000000-0000-4000-8000-000000000050'::uuid,'71000000-0000-4000-8000-000000000051'::uuid,'71000000-0000-4000-8000-000000000054'::uuid,'71000000-0000-4000-8000-000000000055'::uuid,'71000000-0000-4000-8000-000000000056'::uuid,'71000000-0000-4000-8000-000000000057'::uuid,'71000000-0000-4000-8000-000000000058'::uuid,'71000000-0000-4000-8000-000000000059'::uuid,'71000000-0000-4000-8000-000000000060'::uuid,'71000000-0000-4000-8000-000000000061'::uuid,'71000000-0000-4000-8000-000000000062'::uuid,'71000000-0000-4000-8000-000000000063'::uuid,'71000000-0000-4000-8000-000000000064'::uuid,'71000000-0000-4000-8000-000000000065'::uuid,'71000000-0000-4000-8000-000000000067'::uuid,'71000000-0000-4000-8000-000000000033'::uuid,'71000000-0000-4000-8000-000000000039'::uuid,'71000000-0000-4000-8000-000000000042'::uuid,'71000000-0000-4000-8000-000000000043'::uuid,'71000000-0000-4000-8000-000000000046'::uuid,'71000000-0000-4000-8000-000000000073'::uuid,'71000000-0000-4000-8000-000000000080'::uuid,'71000000-0000-4000-8000-000000000081'::uuid,'71000000-0000-4000-8000-000000000082'::uuid,'71000000-0000-4000-8000-000000000084'::uuid,'71000000-0000-4000-8000-000000000085'::uuid,'71000000-0000-4000-8000-000000000088'::uuid,'71000000-0000-4000-8000-000000000089'::uuid,'71000000-0000-4000-8000-000000000090'::uuid,'71000000-0000-4000-8000-000000000093'::uuid,'71000000-0000-4000-8000-000000000094'::uuid,'71000000-0000-4000-8000-000000000095'::uuid,'71000000-0000-4000-8000-000000000097'::uuid,'71000000-0000-4000-8000-000000000100'::uuid,'71000000-0000-4000-8000-000000000004'::uuid,'71000000-0000-4000-8000-000000000006'::uuid,'71000000-0000-4000-8000-000000000035'::uuid,'71000000-0000-4000-8000-000000000036'::uuid,'71000000-0000-4000-8000-000000000049'::uuid,'71000000-0000-4000-8000-000000000070'::uuid,'71000000-0000-4000-8000-000000000072'::uuid,'71000000-0000-4000-8000-000000000079'::uuid,'71000000-0000-4000-8000-000000000091'::uuid,'71000000-0000-4000-8000-000000000098'::uuid,'71000000-0000-4000-8000-000000000099'::uuid,'71000000-0000-4000-8000-000000000005'::uuid,'71000000-0000-4000-8000-000000000034'::uuid,'71000000-0000-4000-8000-000000000041'::uuid) AND active;

UPDATE hairstyle_assets SET style_name='콘로우 로우번',category='BRAIDED',asset_version='curated-20261016',updated_at=now() WHERE hairstyle_id='71000000-0000-4000-8000-000000000066' AND asset_version <> 'curated-20261016';

UPDATE hairstyle_recommendation_profiles SET service_types='["STYLING"]'::jsonb,minimum_hair_length='BELOW_SHOULDER',compatible_hair_lengths='["BELOW_SHOULDER","BELOW_CHEST"]'::jsonb,metadata_version='curated-20261016',updated_at=now() WHERE hairstyle_id='71000000-0000-4000-8000-000000000066' AND metadata_version <> 'curated-20261016';

DO $$ BEGIN IF (SELECT count(*) FROM hairstyle_assets a JOIN files f ON f.file_id=a.thumbnail_file_id JOIN hairstyle_recommendation_profiles p USING(hairstyle_id) WHERE a.hairstyle_id IN ('0d198408-5bf7-514c-b3af-52d56d990361'::uuid,'81871f24-b393-593a-b0b8-f31d7296118a'::uuid,'7ffbc247-3416-5316-905e-6fe769a4153f'::uuid,'12d8e1fd-b70d-50e0-a0d5-a4d769759969'::uuid,'becfa223-4c43-5ca1-80b0-1e3cd17bf008'::uuid,'2f367c11-435d-5c1f-b005-45af3c693de2'::uuid,'a04671ae-c01b-5568-8897-cea144960c1d'::uuid,'3ae493ce-1c5c-566c-95a2-ca69ace18f5f'::uuid,'0c29ffac-9ee8-56b8-a7bf-13d46a0830a5'::uuid,'3719ea70-8240-5652-8a16-02f8f5685b5f'::uuid,'55a650e5-5a99-5ed7-bd78-2ede7a818ed6'::uuid,'f44a00c9-3c3f-5927-98f7-32966c2b137e'::uuid,'090392ec-2fcb-56e3-ba4c-0063af2471d3'::uuid,'30482fbb-adf7-5843-a988-f340b85fa429'::uuid,'cb9713ac-a1b0-5e95-a303-e24bc4a425ac'::uuid,'12a552fe-4ad8-5ee3-8a85-0795f3b27fb4'::uuid,'b98199c9-df22-5914-8d6c-ff1c9818aa2e'::uuid,'7f17a8fe-2b82-558e-969f-79a3563ce8fb'::uuid,'87408dfc-91cf-53df-bfab-369e62e93e38'::uuid,'b4b3e4fc-cd3e-5df2-ae97-4947e1076293'::uuid,'b252732a-2d91-5c01-afd5-6d36f1bd3a92'::uuid,'86c65822-8ea7-58c4-b115-d9924435ea68'::uuid,'8a9d43c6-cf38-5cc3-b038-94fe0f689de7'::uuid,'c17d60af-6144-5d44-9658-eda2e5ad638d'::uuid,'06cc4930-e540-5d6f-b284-1e08b1818a1c'::uuid,'76145924-61ee-52ca-8fb6-faf5392da1d5'::uuid,'447ef348-7a08-5565-b345-de7ba9aa77fd'::uuid,'cbf47ccd-6a33-5cc1-963d-6dcd61fd0189'::uuid,'02310a32-527e-599b-aafd-a64c63398d68'::uuid,'4972ab0a-e635-58d1-85e3-9889c6cce4a7'::uuid,'42d2fb31-6285-5aa4-864f-8fbe10e75a5c'::uuid,'f055858a-07fe-5a79-a257-b7ccc3dab7f8'::uuid,'8167d07c-eb7e-5632-878a-0df3e0ac5ee7'::uuid,'6abf9427-67ac-5754-a2f3-a29c50a8f4b6'::uuid,'b1fdcdba-f935-5f0d-8fba-e9b56f3003fc'::uuid,'e0ef9b77-ea49-5d67-80da-673fc665e394'::uuid,'ffe39c84-1b70-58f0-979c-b7474c76888a'::uuid,'16c7597c-2c40-51e1-a754-ef4fe35aa2b3'::uuid,'c9442c21-485b-5d64-87c3-a97b491e3702'::uuid,'e01d6760-17b8-5cd4-97de-c604322e304d'::uuid,'abcb8ccb-9044-5c1b-954e-2c222fc5a701'::uuid,'fb32cea4-c14f-5c7e-a0d0-2225caf4361c'::uuid,'5b2448da-3946-5de8-9e22-a1584575acda'::uuid,'c0dcc01b-d923-56b0-ba41-3e7f060dac05'::uuid,'cc25d0ff-ec17-5eab-9dc3-95b0fcc14ad0'::uuid,'31717139-b772-5190-95ad-bdad71d6aaed'::uuid,'9dde44be-0010-5f35-880b-35075b1c9ca4'::uuid,'5aa1cd83-3951-5ae7-999f-ddd765f75108'::uuid,'e4eff9f7-6ee8-5023-95c1-fdbad8a771e7'::uuid,'2a7afeef-18a9-5e09-9516-a248e68b4090'::uuid) AND a.active AND a.asset_version='curated-20261016' AND f.status='READY' AND p.minimum_hair_length IS NOT NULL) <> 50 THEN RAISE EXCEPTION 'Expected 50 ready replacement styles'; END IF; IF EXISTS (SELECT 1 FROM hairstyle_assets WHERE hairstyle_id IN ('71000000-0000-4000-8000-000000000068'::uuid,'71000000-0000-4000-8000-000000000075'::uuid,'71000000-0000-4000-8000-000000000076'::uuid,'71000000-0000-4000-8000-000000000077'::uuid,'71000000-0000-4000-8000-000000000078'::uuid,'71000000-0000-4000-8000-000000000001'::uuid,'71000000-0000-4000-8000-000000000023'::uuid,'71000000-0000-4000-8000-000000000027'::uuid,'71000000-0000-4000-8000-000000000028'::uuid,'71000000-0000-4000-8000-000000000030'::uuid,'71000000-0000-4000-8000-000000000050'::uuid,'71000000-0000-4000-8000-000000000051'::uuid,'71000000-0000-4000-8000-000000000054'::uuid,'71000000-0000-4000-8000-000000000055'::uuid,'71000000-0000-4000-8000-000000000056'::uuid,'71000000-0000-4000-8000-000000000057'::uuid,'71000000-0000-4000-8000-000000000058'::uuid,'71000000-0000-4000-8000-000000000059'::uuid,'71000000-0000-4000-8000-000000000060'::uuid,'71000000-0000-4000-8000-000000000061'::uuid,'71000000-0000-4000-8000-000000000062'::uuid,'71000000-0000-4000-8000-000000000063'::uuid,'71000000-0000-4000-8000-000000000064'::uuid,'71000000-0000-4000-8000-000000000065'::uuid,'71000000-0000-4000-8000-000000000067'::uuid,'71000000-0000-4000-8000-000000000033'::uuid,'71000000-0000-4000-8000-000000000039'::uuid,'71000000-0000-4000-8000-000000000042'::uuid,'71000000-0000-4000-8000-000000000043'::uuid,'71000000-0000-4000-8000-000000000046'::uuid,'71000000-0000-4000-8000-000000000073'::uuid,'71000000-0000-4000-8000-000000000080'::uuid,'71000000-0000-4000-8000-000000000081'::uuid,'71000000-0000-4000-8000-000000000082'::uuid,'71000000-0000-4000-8000-000000000084'::uuid,'71000000-0000-4000-8000-000000000085'::uuid,'71000000-0000-4000-8000-000000000088'::uuid,'71000000-0000-4000-8000-000000000089'::uuid,'71000000-0000-4000-8000-000000000090'::uuid,'71000000-0000-4000-8000-000000000093'::uuid,'71000000-0000-4000-8000-000000000094'::uuid,'71000000-0000-4000-8000-000000000095'::uuid,'71000000-0000-4000-8000-000000000097'::uuid,'71000000-0000-4000-8000-000000000100'::uuid,'71000000-0000-4000-8000-000000000004'::uuid,'71000000-0000-4000-8000-000000000006'::uuid,'71000000-0000-4000-8000-000000000035'::uuid,'71000000-0000-4000-8000-000000000036'::uuid,'71000000-0000-4000-8000-000000000049'::uuid,'71000000-0000-4000-8000-000000000070'::uuid,'71000000-0000-4000-8000-000000000072'::uuid,'71000000-0000-4000-8000-000000000079'::uuid,'71000000-0000-4000-8000-000000000091'::uuid,'71000000-0000-4000-8000-000000000098'::uuid,'71000000-0000-4000-8000-000000000099'::uuid,'71000000-0000-4000-8000-000000000005'::uuid,'71000000-0000-4000-8000-000000000034'::uuid,'71000000-0000-4000-8000-000000000041'::uuid) AND active) THEN RAISE EXCEPTION 'Retired styles are still active'; END IF; END $$;

COMMIT;
