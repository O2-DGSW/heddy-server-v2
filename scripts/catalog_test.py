#!/usr/bin/env python3
"""Integration test against a disposable PostgreSQL database (never production)."""
import json
import os
from pathlib import Path
import subprocess
import sys
import uuid

root = Path(__file__).resolve().parents[1]
bundle = Path(sys.argv[1])
data = json.loads((bundle / 'manifest.json').read_text())
dsn = os.environ['CATALOG_TEST_DATABASE_URL']

def sql(statement, succeeds=True):
    result = subprocess.run(['psql', dsn, '-XqAt', '-v', 'ON_ERROR_STOP=1'], input=statement, text=True, capture_output=True)
    assert (result.returncode == 0) == succeeds, result.stderr
    return result.stdout.strip()

def q(value):
    return "'" + str(value).replace("'", "''") + "'"

assert sql('SELECT count(*) FROM information_schema.tables WHERE table_schema=\'public\'') == '0', 'Test requires an empty disposable database'
for migration in sorted((root / 'src/main/resources/db/migration').glob('V*.sql'), key=lambda p:int(p.name.split('__')[0][1:])):
    sql(migration.read_text())
for original in data['original']:
    sql("INSERT INTO files(file_id,upload_id,owner_type,purpose,status,object_key,content_type,file_size,expires_at,file_name) VALUES (" + ','.join([q(original['thumbnail_file_id']),q(uuid.uuid4()),"'SYSTEM'","'HAIRSTYLE_THUMBNAIL'","'READY'",q(original['object_key']),"'image/jpeg'",'100',"now()+interval '1 year'","'fixture.jpg'"]) + ');' +
        'INSERT INTO hairstyle_assets(hairstyle_id,style_name,category,thumbnail_file_id,active,asset_version) VALUES (' + ','.join([q(original['hairstyle_id']),q(original['style_name']),q(original['category']),q(original['thumbnail_file_id']),str(original['active']).lower(),"'fixture'"]) + ');' +
        'INSERT INTO hairstyle_recommendation_profiles(hairstyle_id,estimated_daily_care_minutes,management_difficulty,chemical_stress_level,metadata_version) VALUES (' + q(original['hairstyle_id']) + ",5,'LOW','LOW','fixture');")
apply_sql = (bundle/'apply.sql').read_text()
# A file identity conflict must roll back every insert/update in the transaction.
first = data['entries'][0]
sql("INSERT INTO files(file_id,upload_id,owner_type,purpose,status,object_key,content_type,file_size,expires_at,file_name) VALUES (" + ','.join([q(first['file_id']),q(uuid.uuid4()),"'SYSTEM'","'HAIRSTYLE_THUMBNAIL'","'READY'","'conflicting/key'","'image/jpeg'",'1',"now()+interval '1 year'","'conflict.jpg'"]) + ');')
sql(apply_sql, succeeds=False)
assert sql('SELECT count(*) FROM hairstyle_assets') == '100'
assert sql('SELECT count(*) FROM hairstyle_assets WHERE active') == '92'
sql('DELETE FROM files WHERE file_id='+q(first['file_id']))

def snapshot():
    return sql("SELECT json_build_object('assets',(SELECT json_agg(a ORDER BY hairstyle_id) FROM hairstyle_assets a),'profiles',(SELECT json_agg(p ORDER BY hairstyle_id) FROM hairstyle_recommendation_profiles p),'files',(SELECT json_agg(f ORDER BY file_id) FROM files f),'tags',(SELECT json_agg(json_build_array(hairstyle_id,style_tag_id) ORDER BY hairstyle_id,style_tag_id) FROM hairstyle_style_tags));")

sql(apply_sql)
assert sql('SELECT count(*) FROM hairstyle_assets') == '150'
assert sql('SELECT count(*) FROM hairstyle_assets WHERE active') == '92'
retired = set(data['replacement_numbers'] + data['duplicate_numbers'])
for o in data['original']:
    active = sql('SELECT active FROM hairstyle_assets WHERE hairstyle_id='+q(o['hairstyle_id']))
    assert active == ('f' if o['number'] in retired else str(o['active'])[0].lower()), o['number']
for e in data['entries']:
    actual = json.loads(sql('SELECT row_to_json(t) FROM (SELECT a.style_name,a.category,a.thumbnail_file_id,a.active,p.* FROM hairstyle_assets a JOIN hairstyle_recommendation_profiles p USING(hairstyle_id) WHERE a.hairstyle_id='+q(e['hairstyle_id'])+') t'))
    for field in ['style_name','category','service_types','minimum_hair_length','compatible_hair_lengths','compatible_hair_types','estimated_daily_care_minutes','management_difficulty']:
        assert actual[field] == e[field], (e['style_name'],field)
    assert actual['active'] and actual['thumbnail_file_id'] == e['file_id']
    assert int(sql('SELECT count(*) FROM hairstyle_style_tags WHERE hairstyle_id='+q(e['hairstyle_id']))) == len(e['tags'])
first_state = snapshot()
sql(apply_sql)
assert snapshot() == first_state, 'Retry changed catalogue contents'
print('PASS: schema, 58 retirements, 50 replacements, 92 active, metadata/tags, atomic rollback and idempotent retry')
