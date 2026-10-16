#!/usr/bin/env python3
"""Validate or apply the reviewed, immutable catalogue bundle. No application secrets are logged."""
import argparse
import collections
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys


def validate(bundle):
    bundle = Path(bundle)
    data = json.loads((bundle / 'manifest.json').read_text())
    entries = data['entries']
    assert data['release'] == 'curated-20261016'
    expected = {2,5,6,7,8,9,23,27,28,30,31,32,35,36,37,38,39,56,59,61,62,64,67,68,69,70,72,73,76,77,78,80,81,82,84,85,86,87,88,89,91,92,93,94,95,96,97,98,99,100}
    assert set(data['replacement_numbers']) == expected
    assert {e['replaces_number'] for e in entries} == expected
    assert set(data['duplicate_numbers']) == set(range(40,49)) - {47}
    assert len(entries) == 50
    assert collections.Counter(e['style_presentation'] for e in entries) == {'MASCULINE':25,'FEMININE':25}
    for key in ['hairstyle_id','file_id','upload_id','object_key','source_photo_id','sha256','style_name']:
        assert len({e[key] for e in entries}) == 50, ('duplicate', key)
    levels = ['SHORT','BELOW_CHIN','BELOW_SHOULDER','BELOW_CHEST']
    for e in entries:
        path = (bundle / e['image_file']).resolve()
        assert path.is_relative_to(bundle.resolve() / 'images')
        content = path.read_bytes()
        assert content[:3] == b'\xff\xd8\xff', e['image_file']
        assert hashlib.sha256(content).hexdigest() == e['sha256'], e['image_file']
        assert len(content) == e['file_size']
        assert min(e['width'],e['height']) >= 500
        assert e['source_url'].startswith('https://www.pexels.com/photo/')
        assert e['photographer'] and e['license'] == 'Pexels License' and e['page_license_label'] == 'Free'
        assert e['minimum_hair_length'] in levels
        assert e['compatible_hair_lengths'] == levels[levels.index(e['minimum_hair_length']):]
        assert e['category'] == 'SHORT' or e['minimum_hair_length'] != 'SHORT'
        assert e['management_difficulty'] in {'LOW','MEDIUM','HIGH'}
        assert set(e['service_types']) <= {'CUT','STYLING','PERM','COLOR','TREATMENT'}
        assert len(e['style_name']) <= 30
        assert e['object_key'].startswith('HAIRSTYLE_THUMBNAIL/'+data['release']+'/')
    sql = (bundle/'apply.sql').read_bytes()
    assert hashlib.sha256(sql).hexdigest() == data['sql_sha256']
    assert sql.decode().startswith('BEGIN;') and sql.decode().rstrip().endswith('COMMIT;')
    print(json.dumps({'validated':len(entries),'retired':58,'representative':47,'categories':dict(collections.Counter(e['category'] for e in entries))},ensure_ascii=False))
    return data


def fetch_images(bundle):
    """Fetch original licensed assets without republishing image binaries in Git."""
    from concurrent.futures import ThreadPoolExecutor
    from urllib.parse import urlparse
    bundle = Path(bundle).resolve()
    data = json.loads((bundle/'manifest.json').read_text())
    def fetch(e):
        target = (bundle/e['image_file']).resolve()
        assert target.is_relative_to(bundle/'images')
        url = urlparse(e['download_url'])
        assert url.scheme == 'https' and url.hostname == 'images.pexels.com'
        assert url.path.startswith('/photos/'+e['source_photo_id']+'/') and url.path.endswith('.jpeg'), e['source_photo_id']
        if target.exists():
            content = target.read_bytes()
        else:
            result = subprocess.run(['curl','--fail','--silent','--show-error','--location','--proto','=https',
                '--retry','3','--max-time','60',e['download_url']],capture_output=True,check=True)
            content = result.stdout
        assert hashlib.sha256(content).hexdigest() == e['sha256'], 'Source image changed: '+e['source_photo_id']
        assert len(content) == e['file_size']
        if not target.exists():
            target.parent.mkdir(parents=True,exist_ok=True)
            target.write_bytes(content)
    with ThreadPoolExecutor(max_workers=4) as pool:
        list(pool.map(fetch,data['entries']))


def psql(sql):
    result = subprocess.run(['docker','exec','-i','heddy-v2-postgres','sh','-c',
        'exec psql -X -U "$POSTGRES_USER" -d heddy_v2 -qAt -v ON_ERROR_STOP=1'],
        input=sql, text=True, capture_output=True)
    if result.returncode:
        raise RuntimeError('Catalogue SQL failed: '+result.stderr[-1500:])
    return result.stdout


def apply(bundle, data):
    import boto3
    from botocore.exceptions import ClientError
    inspect = json.loads(subprocess.check_output(['docker','inspect','heddy-v2-app']))[0]
    env = dict(v.split('=',1) for v in inspect['Config']['Env'] if '=' in v)
    options = {'region_name': env.get('AWS_S3_REGION','ap-northeast-2')}
    if env.get('AWS_S3_ACCESS_KEY') and env.get('AWS_S3_SECRET_KEY'):
        options.update(aws_access_key_id=env['AWS_S3_ACCESS_KEY'],aws_secret_access_key=env['AWS_S3_SECRET_KEY'])
    client = boto3.client('s3',**options)
    bucket = env['AWS_S3_BUCKET']
    # Preflight DB identity before uploads; only the 100 reviewed SYSTEM assets are in scope.
    original = {o['hairstyle_id']:o for o in data['original']}
    ids = ','.join("'"+i+"'::uuid" for i in original)
    snapshot = psql("BEGIN READ ONLY; SELECT coalesce(json_agg(row_to_json(t)),'[]') FROM (SELECT row_to_json(a) asset,row_to_json(p) profile,row_to_json(f) file,coalesce((SELECT json_agg(row_to_json(h)) FROM hairstyle_style_tags h WHERE h.hairstyle_id=a.hairstyle_id),'[]'::json) tags FROM hairstyle_assets a JOIN files f ON f.file_id=a.thumbnail_file_id LEFT JOIN hairstyle_recommendation_profiles p USING(hairstyle_id) WHERE a.hairstyle_id IN ("+ids+") AND f.owner_type='SYSTEM' AND f.purpose='HAIRSTYLE_THUMBNAIL') t; COMMIT;")
    rows = json.loads(snapshot)
    assert len(rows) == 100, 'Reviewed original catalogue is missing records'
    for r in rows:
        expected = original[r['asset']['hairstyle_id']]
        assert r['file']['file_id'] == expected['thumbnail_file_id'], 'Thumbnail changed since review'
        assert r['file']['object_key'] == expected['object_key'], 'Object changed since review'
    backup = Path.home() / 'heddy-catalog-backups' / data['release']
    backup.mkdir(parents=True, exist_ok=True,mode=0o700)
    backup_file = backup/'before.json'
    # Retain the very first snapshot across retries.
    try:
        with backup_file.open('x') as f:
            os.chmod(backup_file,0o600)
            f.write(snapshot)
    except FileExistsError:
        pass
    for e in data['entries']:
        content = (Path(bundle)/e['image_file']).read_bytes()
        try:
            head = client.head_object(Bucket=bucket,Key=e['object_key'])
        except ClientError as ex:
            if ex.response['Error']['Code'] not in {'404','NoSuchKey','NotFound'}:
                raise
            head = None
        if head is None:
            client.put_object(Bucket=bucket,Key=e['object_key'],Body=content,ContentType='image/jpeg',
                Metadata={'sha256':e['sha256'],'source-photo-id':e['source_photo_id'],'catalog-release':data['release']},IfNoneMatch='*')
        else:
            assert head['Metadata'].get('sha256') == e['sha256'], 'Existing object content conflict'
        # Verify actual S3 bytes before committing READY database records.
        remote = client.get_object(Bucket=bucket,Key=e['object_key'])['Body'].read()
        assert len(remote) == e['file_size'] and hashlib.sha256(remote).hexdigest() == e['sha256']
    print('Verified 50 uploaded catalogue images; applying atomic database change.',flush=True)
    psql((Path(bundle)/'apply.sql').read_text())
    newids = ','.join("'"+e['hairstyle_id']+"'::uuid" for e in data['entries'])
    result = psql("BEGIN READ ONLY; SELECT json_build_object('new_active',(SELECT count(*) FROM hairstyle_assets WHERE hairstyle_id IN ("+newids+") AND active),'all_active',(SELECT count(*) FROM hairstyle_assets WHERE active),'original_preserved',(SELECT count(*) FROM hairstyle_assets WHERE hairstyle_id IN ("+ids+"))); COMMIT;")
    counts=json.loads(result)
    assert counts['new_active']==50 and counts['original_preserved']==100 and counts['all_active']==92
    print(json.dumps(counts))


if __name__ == '__main__':
    parser=argparse.ArgumentParser()
    parser.add_argument('bundle',type=Path)
    parser.add_argument('--apply',action='store_true')
    parser.add_argument('--fetch-images',action='store_true')
    args=parser.parse_args()
    try:
        if args.fetch_images: fetch_images(args.bundle)
        data=validate(args.bundle)
        if args.apply: apply(args.bundle,data)
    except Exception as error:
        print(str(error),file=sys.stderr)
        sys.exit(1)
