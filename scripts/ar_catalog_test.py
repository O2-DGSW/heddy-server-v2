"""Run against a disposable PostgreSQL database, including every production migration."""
import json
from pathlib import Path
import subprocess
import sys
import time
import uuid
from ar_catalog import apply_sql, validate


def main():
    data = validate(sys.argv[1])
    name = 'heddy-ar-catalog-test-' + uuid.uuid4().hex[:8]
    subprocess.run(['docker','run','--rm','-d','--name',name,
        '-e','POSTGRES_PASSWORD=local-catalog-test','postgres:16-alpine'], check=True,capture_output=True)
    def sql(statement):
        return subprocess.run(['docker','exec','-i',name,'psql','-X','-qAt','-U','postgres',
            '-v','ON_ERROR_STOP=1'],input=statement,text=True,capture_output=True,check=True).stdout
    try:
        for _ in range(40):
            if subprocess.run(['docker','exec',name,'pg_isready','-U','postgres'],capture_output=True).returncode == 0:
                break
            time.sleep(.25)
        root = Path(__file__).resolve().parent.parent
        paths = sorted((root/'src/main/resources/db/migration').glob('V*.sql'),key=lambda p:int(p.name.split('__')[0][1:]))
        for path in paths:
            sql(path.read_text())
        # Include a legacy active style to exercise retirement without deleting saved references.
        old_id = '33333333-3333-4333-8333-333333333333'
        sql(f"INSERT INTO hairstyle_assets(hairstyle_id,style_name,category,active,asset_version) VALUES ('{old_id}','기존 스타일','SHORT',true,'v1');")
        previous = [{'hairstyle_id':old_id,'active':True}]
        sql(apply_sql(data, previous))
        assert sql('SELECT count(*) FROM hairstyle_assets WHERE active;').strip() == '53'
        assert sql('SELECT count(*) FROM hairstyle_assets;').strip() == '54'
        assert sql("SELECT count(*) FROM hairstyle_assets a JOIN files f ON f.file_id=a.thumbnail_file_id JOIN hairstyle_recommendation_profiles p USING(hairstyle_id) WHERE a.active AND f.status='READY' AND f.owner_type='SYSTEM' AND a.ar_groom_name IS NOT NULL;").strip() == '53'
        assert sql("SELECT count(*) FROM hairstyle_assets WHERE ar_groom_name IN ('short-swept','side-sweep','long-wave');").strip() == '0'
        assert sql('SELECT count(*) FROM hairstyle_colors;').strip() != '0'
        # A stale reviewed snapshot must reject a second application atomically.
        try:
            sql(apply_sql(data, previous))
        except subprocess.CalledProcessError:
            pass
        else:
            raise AssertionError('Concurrent change guard did not reject stale snapshot')
        assert sql('SELECT count(*) FROM hairstyle_assets WHERE active;').strip() == '53'
        print('PASS: migrations, 53 ready S3 references, groom mapping, colors, legacy retention, concurrent-change rollback')
    finally:
        subprocess.run(['docker','rm','-f',name],capture_output=True,check=True)


if __name__ == '__main__':
    main()
