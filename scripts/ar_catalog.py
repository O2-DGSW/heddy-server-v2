#!/usr/bin/env python3
"""Validate/apply the AR catalogue on the existing Heddy EC2 host. Never print credentials."""
import argparse
import collections
import hashlib
import json
import os
from pathlib import Path
import re
import struct
import subprocess
import uuid

RELEASE = 'ar-notion-20261020'
EXCLUDED = {'short-swept', 'side-sweep', 'long-wave'}


def validate(bundle):
    bundle = Path(bundle).resolve()
    data = json.loads((bundle / 'manifest.json').read_text())
    if data['release'] != RELEASE or len(data['entries']) != 53:
        raise ValueError('Expected the 53 converted Notion styles')
    entries = data['entries']
    if collections.Counter((e['gender'], e['type']) for e in entries) != {
            ('남자', '컷'): 18, ('남자', '펌'): 10, ('여자', '컷'): 11, ('여자', '펌'): 14}:
        raise ValueError('Unexpected catalogue composition')
    for field in ['name', 'hairstyle_id', 'file_id', 'upload_id', 'object_key']:
        if len({e[field] for e in entries}) != 53:
            raise ValueError('Duplicate identity: ' + field)
    for e in entries:
        name = e['name']
        if not re.fullmatch('[a-z0-9][a-z0-9-]{0,79}', name) or name in EXCLUDED:
            raise ValueError('Invalid AR groom identity')
        for field, kind in [('hairstyle_id', 'hairstyle'), ('file_id', 'thumbnail'), ('upload_id', 'upload')]:
            expected = str(uuid.uuid5(uuid.NAMESPACE_URL, f'https://heddy.site/ar-catalog/{name}/{kind}'))
            if e[field] != expected:
                raise ValueError('Unexpected stable identity')
        if e['object_key'] != f'HAIRSTYLE_THUMBNAIL/{RELEASE}/{name}.png':
            raise ValueError('Unexpected S3 key')
        if e['image_file'] != f'images/{name}.png':
            raise ValueError('Invalid image path')
        content = (bundle / e['image_file']).read_bytes()
        if content[:8] != b'\x89PNG\r\n\x1a\n' or hashlib.sha256(content).hexdigest() != e['sha256']:
            raise ValueError('Thumbnail integrity mismatch: ' + name)
        if len(content) != e['file_size'] or struct.unpack('>II', content[16:24]) != (e['width'], e['height']):
            raise ValueError('Thumbnail metadata mismatch')
        levels = ['SHORT', 'BELOW_CHIN', 'BELOW_SHOULDER', 'BELOW_CHEST']
        if e['compatible_hair_lengths'] != levels[levels.index(e['minimum_hair_length']):]:
            raise ValueError('Invalid minimum length')
    return data


def quote(value):
    return "'" + str(value).replace("'", "''") + "'"


def js(value):
    return quote(json.dumps(value, ensure_ascii=False)) + '::jsonb'


def apply_sql(data, previous):
    statements = ['BEGIN;', 'LOCK TABLE hairstyle_assets IN SHARE ROW EXCLUSIVE MODE;']
    # Fail on concurrent catalogue edits; this script is a reviewed replacement, not an open-ended import.
    expected = js(previous)
    statements.append(f"""DO $$ BEGIN IF (SELECT coalesce(jsonb_agg(jsonb_build_object(
        'hairstyle_id', hairstyle_id, 'active', active) ORDER BY hairstyle_id), '[]'::jsonb)
        FROM hairstyle_assets) <> {expected} THEN
        RAISE EXCEPTION 'Catalogue changed after snapshot'; END IF; END $$;""")
    for e in data['entries']:
        fid, hid = quote(e['file_id']), quote(e['hairstyle_id'])
        statements.append(f"""INSERT INTO files (file_id, upload_id, owner_type, user_id, purpose,
            status, object_key, content_type, file_size, sha256, width, height, expires_at, file_name)
            VALUES ({fid},{quote(e['upload_id'])},'SYSTEM',NULL,'HAIRSTYLE_THUMBNAIL','READY',
            {quote(e['object_key'])},'image/png',{e['file_size']},{quote(e['sha256'])},
            {e['width']},{e['height']},now(),{quote(e['name']+'.png')})
            ON CONFLICT (file_id) DO NOTHING;""")
        anchor = {'groom_name': e['name'], 'user_fit': e['user_fit'], 'gender': e['gender'],
                  'source_tags': e['tags'], 'catalog_release': RELEASE,
                  'metadata_policy': 'tag-derived-estimates-v1'}
        statements.append(f"""INSERT INTO hairstyle_assets (hairstyle_id,style_name,category,
            thumbnail_file_id,ar_mode,ar_groom_name,anchor_config,active,asset_version)
            VALUES ({hid},{quote(e['label'])},{quote(e['category'])},{fid},'THREE_D',
            {quote(e['name'])},{js(anchor)},true,'ar-notion-v1')
            ON CONFLICT (hairstyle_id) DO NOTHING;""")
        # Do not invent hair-type/thickness compatibility. Empty lists mean not assessed.
        contraindications = ['DAMAGED', 'SEVERELY_DAMAGED'] if e['type'] == '펌' else []
        statements.append(f"""INSERT INTO hairstyle_recommendation_profiles
            (hairstyle_id,service_types,compatible_hair_lengths,minimum_hair_length,
             contraindicated_hair_conditions,estimated_daily_care_minutes,management_difficulty,
             chemical_stress_level,editorial_priority,metadata_version)
            VALUES ({hid},{js(e['service_types'])},{js(e['compatible_hair_lengths'])},
            {quote(e['minimum_hair_length'])},{js(contraindications)},
            {e['estimated_daily_care_minutes']},{quote(e['management_difficulty'])},
            {quote(e['chemical_stress_level'])},0,'ar-tags-estimate-v1')
            ON CONFLICT (hairstyle_id) DO NOTHING;""")
        # Keep existing preference IDs: map by exact style name or a small explicit alias table.
        aliases = {'bob-cut': ['클래식 보브','쇼트 보브'], 'hippie-perm': ['히피펌'],
                   'layered-cut': ['미디엄 레이어드','롱 레이어드'], 'glossy-straight': ['긴 생머리'],
                   'hush-cut': ['중단발 허쉬컷'], 'danbal-cut': ['레이어드 단발'],
                   'c-curl-perm': ['C컬 단발'], 'pixie-cut': ['픽시컷']}
        names = [e['label'], *aliases.get(e['name'], [])]
        statements.append(f"""INSERT INTO hairstyle_style_tags (hairstyle_id,style_tag_id)
            SELECT {hid},style_tag_id FROM style_tags WHERE tag_name IN ({','.join(map(quote,names))})
            ON CONFLICT DO NOTHING;""")
        statements.append(f"""INSERT INTO hairstyle_colors (hairstyle_id,color_id,name,hex_code,sort_order)
            SELECT {hid},color_id,name,hex_code,sort_order FROM hair_colors ON CONFLICT DO NOTHING;""")
    ids = ','.join(quote(e['hairstyle_id']) for e in data['entries'])
    statements.append(f"UPDATE hairstyle_assets SET active = (hairstyle_id IN ({ids})), updated_at=now();")
    statements.append("UPDATE recommendation_runs SET status='STALE' WHERE status='ACTIVE';")
    statements.append('COMMIT;')
    return '\n'.join(statements)


def psql(sql):
    result = subprocess.run(['docker','exec','-i','heddy-v2-postgres','sh','-c',
        'exec psql -X -U "$POSTGRES_USER" -d heddy_v2 -qAt -v ON_ERROR_STOP=1'],
        input=sql, text=True, capture_output=True)
    if result.returncode:
        raise RuntimeError('Catalogue SQL failed: ' + result.stderr[-1500:])
    return result.stdout


def apply(bundle, data):
    import boto3
    from botocore.exceptions import ClientError
    # Check that the new API is deployed before changing candidate selection.
    psql('SELECT ar_groom_name FROM hairstyle_assets LIMIT 0;')
    previous = json.loads(psql("SELECT coalesce(json_agg(row_to_json(t)), '[]') FROM "
        "(SELECT hairstyle_id,active FROM hairstyle_assets ORDER BY hairstyle_id) t;"))
    new_ids = {e['hairstyle_id'] for e in data['entries']}
    if any(r['hairstyle_id'] in new_ids for r in previous):
        raise ValueError('Release already present; inspect it instead of overwriting it')
    backup = Path.home() / 'heddy-catalog-backups' / RELEASE
    backup.mkdir(parents=True, exist_ok=True, mode=0o700)
    # Write once; never replace the original restore point.
    snapshot = backup/'before.json'
    if snapshot.exists():
        if json.loads(snapshot.read_text()) != previous:
            raise ValueError('Catalogue differs from the retained backup')
    else:
        with snapshot.open('x') as f:
            os.chmod(f.name, 0o600)
            json.dump(previous, f)
    restore = ['BEGIN;', "UPDATE hairstyle_assets SET active=false WHERE ar_groom_name IS NOT NULL;"]
    restore += [f"UPDATE hairstyle_assets SET active={'true' if r['active'] else 'false'} WHERE hairstyle_id={quote(r['hairstyle_id'])};" for r in previous]
    restore += ["UPDATE recommendation_runs SET status='STALE' WHERE status='ACTIVE';", 'COMMIT;']
    (backup/'restore.sql').write_text('\n'.join(restore))
    sql = apply_sql(data, previous)
    (backup/'apply.sql').write_text(sql)
    env = dict(v.split('=',1) for v in json.loads(subprocess.check_output(
        ['docker','inspect','heddy-v2-app']))[0]['Config']['Env'] if '=' in v)
    options = {'region_name': env.get('AWS_S3_REGION','ap-northeast-2')}
    if env.get('AWS_S3_ACCESS_KEY'):
        options.update(aws_access_key_id=env['AWS_S3_ACCESS_KEY'],aws_secret_access_key=env['AWS_S3_SECRET_KEY'])
    s3 = boto3.client('s3', **options)
    bucket = env['AWS_S3_BUCKET']
    for e in data['entries']:
        content = (Path(bundle)/e['image_file']).read_bytes()
        try:
            head = s3.head_object(Bucket=bucket, Key=e['object_key'])
        except ClientError as error:
            if error.response['Error']['Code'] not in {'404','NoSuchKey','NotFound'}:
                raise
            head = None
        if head is None:
            s3.put_object(Bucket=bucket, Key=e['object_key'], Body=content, ContentType='image/png',
                Metadata={'sha256':e['sha256'],'ar-groom':e['name'],'catalog-release':RELEASE}, IfNoneMatch='*')
        remote = s3.get_object(Bucket=bucket, Key=e['object_key'])['Body'].read()
        if hashlib.sha256(remote).hexdigest() != e['sha256']:
            raise ValueError('S3 content conflict: '+e['name'])
    psql(sql)
    counts = json.loads(psql("SELECT json_build_object('active',count(*) FILTER (WHERE active),"
        "'ar_mapped',count(*) FILTER (WHERE active AND ar_mode='THREE_D' AND ar_groom_name IS NOT NULL),"
        "'total_preserved',count(*)) FROM hairstyle_assets;"))
    if counts['active'] != 53 or counts['ar_mapped'] != 53:
        raise RuntimeError('Post-apply count mismatch')
    print(json.dumps(counts))


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('bundle',type=Path)
    parser.add_argument('--apply',action='store_true')
    args = parser.parse_args()
    data = validate(args.bundle)
    print('Validated 53 AR styles')
    if args.apply:
        apply(args.bundle, data)
