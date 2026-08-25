# 배포 세팅 런북 (EC2 + GHCR + docker compose)

`deploy.yml` 는 `main` 푸시와 수동 실행(`workflow_dispatch`)으로 동작한다.
워크플로가 서버에 접속하려면 아래 시크릿이 먼저 등록돼 있어야 한다.
**시크릿 등록과 v1 폐기는 사람이 직접 실행한다.**

## 1. 저장소 시크릿 등록 (수동)

레포 시크릿 5종. 로컬에서 한 번만 실행하면 된다.

```bash
gh secret set EC2_HOST   --body "<EC2 퍼블릭 IP 또는 도메인>"
gh secret set EC2_PORT   --body "22"
gh secret set EC2_USER   --body "<SSH 접속 사용자>"
gh secret set EC2_SSH_KEY --body "$(cat <배포용 .pem 경로>)"   # 줄바꿈 보존을 위해 파일 전체를 넣는다
gh secret set PROD_ENV   --body "$(cat ./prod.env)"             # 아래 2번 템플릿으로 만든 파일
```

- `EC2_SSH_KEY` 는 EC2 인스턴스에 SSH 로 접속 가능한 키의 **전체 내용**(헤더 포함)이어야 한다.
- 워크플로가 쓰는 배포 디렉터리는 `/home/<EC2_USER>/heddy-server-v2` 다. v1 의 디렉터리와 섞이지 않게 별도 경로를 쓴다.

## 2. `PROD_ENV` 필수 값 체크리스트 (12종)

아래 12개가 하나라도 빠지면 `docker compose up` 이 즉시 실패한다
(`docker-compose.prod.yml` 의 `${VAR:?}` 가드). 빈 값으로 남겨야 하는 변수도
**키 자체는 반드시 있어야** 한다.

```dotenv
JWT_SECRET=<64자 이상 랜덤 문자열>
DB_URL=jdbc:postgresql://postgres:5432/heddy_v2
DB_USERNAME=<운영 DB 계정>
DB_PASSWORD=<운영 DB 비밀번호>
REDIS_HOST=redis
REDIS_PORT=6379
AWS_S3_BUCKET=<운영 S3 버킷명>
AWS_S3_REGION=ap-northeast-2
AWS_S3_ACCESS_KEY=<IAM 액세스 키>
AWS_S3_SECRET_KEY=<IAM 시크릿 키>
AWS_S3_ENDPOINT=
CORS_ALLOWED_ORIGINS=https://<실제 클라이언트 도메인>
```

| 변수 | 규칙 |
|---|---|
| `DB_URL` | 호스트는 반드시 compose 서비스명 `postgres`. `localhost` 는 컨테이너 자기 자신이다 |
| `DB_USERNAME` · `DB_PASSWORD` | postgres 서비스의 `POSTGRES_USER` · `POSTGRES_PASSWORD` 와 같은 변수를 공유하므로 compose 가 일치를 보장한다 |
| `REDIS_HOST` | compose 서비스명 `redis`. `REDIS_PORT` 는 컨테이너 내부 포트 `6379` |
| `AWS_S3_ENDPOINT` | **빈 값 유지** — 실제 AWS S3 를 쓴다 (MinIO 엔드포인트를 넣지 않는다) |
| `JWT_SECRET` | 기본값이 없는 유일한 변수. 없으면 부팅 자체가 실패한다 |

기능별 선택 변수(소셜 로그인 `KAKAO_APP_ID` · `GOOGLE_CLIENT_ID` · `APPLE_CLIENT_ID`,
SMS `SOLAPI_*` 등)는 해당 기능을 켤 때 같은 파일에 추가하면 `.env` 째로 컨테이너에 전달된다.

## 3. 첫 배포

1. 위 1·2번이 끝났으면 `main` 에 푸시하거나 Actions 에서 `Deploy` 를 수동 실행한다.
2. 워크플로: test → bootJar → GHCR 푸시(SHA + latest 태그) → SSH 로 `docker-compose.yml` · `.env` 업로드 → `APP_IMAGE_TAG=<SHA>` 로 pull & up.
3. 서버에서 확인:

```bash
cd ~/heddy-server-v2
docker compose ps                 # app 이 healthy 가 될 때까지 대기 (start_period 60s)
docker compose logs -f app        # Flyway 마이그레이션과 기동 로그 확인
curl -s http://localhost:8080/actuator/health    # {"status":"UP"} 이어야 한다
```

4. **설정이 로컬 기본값으로 뜨지 않았는지 확인** — 미지정 변수가 조용히 로컬 값을 쓰는 사고를 막는 최종 점검이다:

```bash
docker exec heddy-v2-app printenv REDIS_HOST     # localhost 가 아니라 redis 여야 한다
docker exec heddy-v2-app printenv DB_URL         # postgres:5432 여야 한다
```

## 4. v1 정리 (데이터 백업 없음)

같은 인스턴스에서 v1 컨테이너와 볼륨을 폐기하고 8080 포트를 회수한다.
**v1 의 PostgreSQL 데이터는 보존하지 않는다 — 백업 절차가 없으며, 되돌릴 수 없다.**
v2 는 별도 스키마(`heddy_v2`)로 새로 시작한다. v2 컨테이너명·볼륨명은 모두
`heddy-v2-` 접두사라 v1 잔여 리소스와 겹치지 않는다.

서버에서:

```bash
# v1 이 어떤 이름으로 떠 있는지 먼저 확인 (예상: heddy-app, heddy-postgres, heddy-redis 계열)
docker ps
docker volume ls

# v1 중단·삭제
docker rm -f <v1-app> <v1-postgres> <v1-redis>

# v1 볼륨 삭제 — 이 시점부터 v1 데이터는 복구 불가
docker volume rm <v1-db-volume> <v1-cache-volume>

# 8080 포트 회수 확인 (v1 이 붙잡고 있던 8080 이 비었는지)
ss -ltnp | grep 8080 || echo "8080 free"

# 잔여 이미지 정리 (선택)
docker image prune -af
```

v1 을 systemd 나 옛 cron 스크립트로 띄웠다면 해당 유닛·스크립트도 함께 비활성화해서
재부팅 후 되살아나지 않게 한다.

## 5. 롤백

이미지 태그가 커밋 SHA 고정이라 특정 시점으로 되돌리는 게 단순하다.
서버에서 이전 SHA 태그로 재기동한다:

```bash
cd ~/heddy-server-v2
APP_IMAGE_TAG=<되돌아갈 커밋 SHA> docker compose up -d app
docker compose ps          # healthy 확인
```

주의:

- 롤백해도 **DB 스키마는 되돌아가지 않는다**. Flyway 마이그레이션이 진행된 커밋으로
  되돌릴 때는 스키마 호환성을 먼저 확인한다.
- GHCR 에 해당 SHA 태그가 없으면(예: 워크플로 실패 커밋) 롤백할 수 없다.
  배포 성공 커밋의 SHA 를 기록해 둔다 (`docker compose ps` 의 IMAGE 열로 확인 가능).

## 6. 참고

- `deploy.yml` 는 `main` 푸시에만 반응한다. `develop` 머지로는 배포되지 않는다.
- 이미지: `ghcr.io/o2-dgsw/heddy-server-v2:<commit-sha>` (+ `latest`). `latest` 로 배포하지
  않는 이유는 "어떤 코드가 떠 있는지 사후 추적 불가 + 롤백 지점 상실" 때문이다.
- 헬스체크: 액추에이터 `/actuator/health` (compose healthcheck 가 15초 간격으로 검사).
