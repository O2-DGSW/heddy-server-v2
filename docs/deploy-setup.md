# 배포 세팅 런북 (EC2 + GHCR + docker compose)

`deploy.yml` 는 `main` 푸시와 수동 실행(`workflow_dispatch`)으로 동작한다.
워크플로가 서버에 접속하려면 아래 시크릿이 먼저 등록돼 있어야 한다.
시크릿 등록은 저장소 관리 권한으로 수행한다. v1 폐기는 별도 수동 작업이다.

## 1. 저장소 시크릿 등록 (수동)

레포 시크릿 6종. 로컬에서 한 번만 등록하면 이후 배포에 재사용된다.

```bash
gh secret set EC2_HOST   --body "<EC2 퍼블릭 IP 또는 도메인>"
gh secret set EC2_PORT   --body "22"
gh secret set EC2_USER   --body "<SSH 접속 사용자>"
gh secret set EC2_SSH_KEY --body "$(cat <배포용 .pem 경로>)"   # 줄바꿈 보존을 위해 파일 전체를 넣는다
gh secret set PROD_ENV   --body "$(cat ./prod.env)"             # 아래 2번 템플릿으로 만든 파일
```

- `EC2_SSH_KEY` 는 EC2 인스턴스에 SSH 로 접속 가능한 키의 **전체 내용**(헤더 포함)이어야 한다.
- `OPENAI_API_KEY`는 별도 Actions Secret으로 등록한다. 키를 커밋하거나 명령줄 인자로 넘기지 않는다.
  `scripts/create-deployment-bundle.py`가 `PROD_ENV`의 이전 키를 제거하고 별도 시크릿을 넣으므로,
  매 배포에서 서버 `.env`가 교체되어도 유지된다. 키 교체는 이 시크릿만 갱신하면 된다.
  시크릿 누락·잘못된 형식은 기존 서버를 변경하기 전 번들 생성 단계에서 차단한다.
  CI/Deploy 모두 반복 배포·키 교체·누락 시 보존 테스트를 실행한다.
  배포 전후 컨테이너에서 키 존재 여부를 확인하고, 값은 로그에 출력하지 않는다.
- 워크플로가 쓰는 배포 디렉터리는 `/home/<EC2_USER>/heddy-server-v2` 다. v1 의 디렉터리와 섞이지 않게 별도 경로를 쓴다.

## 2. `PROD_ENV` 필수 값 체크리스트 (10종 + S3 자격증명 페어)

아래 10개가 하나라도 빠지면 `docker compose up` 이 즉시 실패한다
(`docker-compose.prod.yml` 의 `${VAR:?}` 가드). 빈 값으로 남겨야 하는 변수도
**키 자체는 반드시 있어야** 한다.

`AWS_S3_ACCESS_KEY` · `AWS_S3_SECRET_KEY` 는 가드 대상이 아니다 — **둘 다 비워
두면** AWS 기본 자격증명 체인(EC2 인스턴스 역할)을 사용한다. IAM 키를 직접 쓰려면
둘 다 채운다. 한쪽만 채우면 기동 시점에 막힌다.

```dotenv
JWT_SECRET=<64자 이상 랜덤 문자열>
DB_URL=jdbc:postgresql://postgres:5432/heddy_v2
DB_USERNAME=<운영 DB 계정>
DB_PASSWORD=<운영 DB 비밀번호>
REDIS_HOST=redis
REDIS_PORT=6379
AWS_S3_BUCKET=<운영 S3 버킷명>
AWS_S3_REGION=ap-northeast-2
AWS_S3_ACCESS_KEY=            # 인스턴스 역할 쓰면 비움, 아니면 IAM 액세스 키
AWS_S3_SECRET_KEY=            # 위와 항상 함께 (둘 다 비움 or 둘 다 채움)
AWS_S3_ENDPOINT=
CORS_ALLOWED_ORIGINS=https://<실제 클라이언트 도메인>
SHARE_PUBLIC_BASE_URL=https://heddy.site/s  # 선택: 미지정 시에도 이 값이 기본 적용됨
```

| 변수 | 규칙 |
|---|---|
| `DB_URL` | 호스트는 반드시 compose 서비스명 `postgres`. `localhost` 는 컨테이너 자기 자신이다 |
| `DB_USERNAME` · `DB_PASSWORD` | postgres 서비스의 `POSTGRES_USER` · `POSTGRES_PASSWORD` 와 같은 변수를 공유하므로 compose 가 일치를 보장한다 |
| `REDIS_HOST` | compose 서비스명 `redis`. `REDIS_PORT` 는 컨테이너 내부 포트 `6379` |
| `AWS_S3_ACCESS_KEY` · `AWS_S3_SECRET_KEY` | 둘 다 비우면 인스턴스 역할로 인증한다. 배포 전에 인스턴스에 역할이 붙어 있는지 확인한다 |
| `AWS_S3_ENDPOINT` | **빈 값 유지** — 실제 AWS S3 를 쓴다 (MinIO 엔드포인트를 넣지 않는다) |
| `JWT_SECRET` | 기본값이 없는 유일한 변수. 없으면 부팅 자체가 실패한다 |
| `SHARE_PUBLIC_BASE_URL` | QR을 찍었을 때 열 프론트 공유 페이지의 URL 앞부분. 기본값은 `https://heddy.site/s`이며 다른 환경에서만 재정의한다 |

기능별 선택 변수(소셜 로그인 `KAKAO_APP_ID` · `GOOGLE_CLIENT_ID` · `APPLE_CLIENT_ID`,
SMS `SOLAPI_*` 등)는 해당 기능을 켤 때 같은 파일에 추가하면 `.env` 째로 컨테이너에 전달된다.

### 추천 설명 AI

Spring에서 OpenAI Responses API를 직접 호출한다. 추천 점수·순위는 기존 규칙으로 결정하고,
선택된 최대 3개 후보의 근거만 한 번에 보내 한국어 설명을 생성한다.
이름·연락처·사용자/시술 UUID·사진·메모는 보내지 않는다. `store=false`를 사용한다.

| 설정 | 기본값 | 용도 |
|---|---|---|
| `OPENAI_API_KEY` | 없음 | 별도 GitHub Actions Secret, 운영 필수 |
| `RECOMMENDATION_AI_ENABLED` | `true` | `PROD_ENV`에서 `false`로 끌 수 있음 |
| `OPENAI_RECOMMENDATION_MODEL` | `gpt-4.1-nano-2025-04-14` | 추론 단계 없이 짧은 설명을 생성하는 저비용 모델 |
| `OPENAI_RECOMMENDATION_TIMEOUT_MS` | `8000` | 전체 호출 제한 시간, 최대 15000ms |

- 출력 상한은 요청당 384토큰이다. 호출을 자동 재시도하지 않는다.
- 키 누락(로컬), 시간 초과, 429, API 오류, 거절·불완전·잘못된 JSON 응답은 기본 문구로 대체한다.
- 설명은 `recommendation_items.reasons_json`에 저장한다. 조회/동일 입력 재사용에는 추가 호출이 없다.
  강제 새로고침에는 다시 호출하며, 모델·프롬프트 버전 변경도 재생성을 유발한다.
- 화면은 `items[].explanation`을 표시하면 된다. 기존 클라이언트를 위해
  `items[].reasons[0].message`에도 같은 설명을 반환한다. 클라이언트가 `code`만 보고 자체 문구를
  만드는 경우 프론트 수정이 필요하다.
- 생성 실패 시 기본 문구를 5분 동안 재사용한다. 이후 최신 추천 조회/생성 요청에서 다시 시도하므로
  키 교체·크레딧 충전·장애 복구 후 실패 문구가 영구적으로 남지 않는다. 강제 새로고침은 즉시 재시도한다.
  성공한 설명과 AI 비활성 상태의 결과는 기존대로 재사용한다.
- AI가 문장화해도 부족한 추천 메타데이터가 보완되지는 않는다. 시술 종류 일치는 세부 스타일 유사성의 근거가 아니다.
- `429 credit_balance_exhausted`/`insufficient_quota`는 일시적인 속도 제한과 다르다.
  [API 결제](https://platform.openai.com/settings/organization/billing)에서 잔액을 확인하고 충전한 후 다시 생성한다.
  ChatGPT 구독과 API 결제는 별개다. 간단한 실험 모델로 `gpt-6-luna`도 선택할 수 있지만,
  모델을 바꾸는 것만으로 크레딧 부족이 해결되지는 않는다.

로컬 키는 Git에서 제외된 `.env.local`의 `OPENAI_API_KEY`에 저장한다. Spring이 자동으로 읽으며
Docker 빌드 컨텍스트에서는 제외한다. 테스트 프로필은 AI를 끈다.

모델/요금 근거: [GPT-4.1 nano](https://developers.openai.com/api/docs/models/gpt-4.1-nano),
[OpenAI 요금](https://developers.openai.com/api/docs/pricing). 2026-10-17 확인 기준 표준 요금은
입력 100만 토큰당 $0.10, 출력 $0.40이다. 추론 토큰이 없어 설명 생성의 토큰 사용량을 제한하기 쉽다.
예를 들어 입력 1,000토큰 + 출력 384토큰이면 요청당 약 $0.000254이며 실제 비용은 입력량에 따라 다르다.

헤어 분석을 켤 때는 아래 값도 추가한다. 모델 파일은 이미지에 넣지 않고 EC2의
`/home/ubuntu/heddy-models`에서 컨테이너로 읽기 전용 마운트한다.

```dotenv
HAIR_ANALYSIS_ENABLED=true
HAIR_ANALYSIS_MODEL_HOST_DIR=/home/ubuntu/heddy-models
HAIR_ANALYSIS_MODEL_PATH=/opt/heddy/models/face-parsing.onnx
HAIR_ANALYSIS_MODEL_VERSION=<학습 데이터와 가중치를 식별하는 버전>
HAIR_ANALYSIS_WORKER_THREADS=1
HAIR_ANALYSIS_INPUT_SIZE=320
```

- ONNX 입력은 RGB `float32 [1,3,S,S]`(기본 S=512, 소형 운영 호스트 S=320), 출력은 19개 이상의 클래스 logits
  `[1,C,H,W]`여야 한다. 사용하는 클래스 번호는 skin=1, nose=2, left-eye=4,
  right-eye=5, hair=13이다.
- 모델 파일이 없거나 열리지 않으면 앱을 분석 활성 상태로 기동하지 않는다. 기능을 끈 환경의
  분석 요청은 더미 점수 대신 `503 ANALYSIS_ENGINE_UNAVAILABLE`을 반환한다.
- 참고용 공개 `jonathandinu/face-parsing`/CelebAMask-HQ 가중치는 비상업 용도 제한이 있으므로
  운영 서비스에 그대로 배포하지 않는다. 실제 운영에는 사용 권리를 확보한 가중치를 같은 ONNX
  계약으로 내보내 배치한다.
- 배포 워크플로는 기존 앱을 재시작하기 전에 후보 이미지의 실제 사용자와 모델 마운트로
  `scripts/check-hair-analysis-model.sh`를 실행한다. 분석이 켜졌는데 파일이 없거나 비어 있거나
  읽을 수 없으면 기존 컨테이너를 유지하고 배포를 실패시킨다. 비활성화 상태는 503 경고를 남긴다.
  이 검사는 파일 접근만 검증하므로 ONNX 호환성과 실제 사진 추론은 별도로 확인해야 한다.
- 작은 호스트에서 분석 실행은 엔진 단위로 직렬화하고 ONNX 내부 스레드는 1개로 제한한다.
  CPU arena와 memory pattern 캐시를 끄므로 추론 버퍼를 계속 보유하지 않는다.
  기본 512px와 기존 지표 계산식은 유지한다. 2GB 운영 서버는 실제 추론 검증을 거친
  `HAIR_ANALYSIS_INPUT_SIZE=320`을 사용한다. 작은 입력에서는 눈 등 세부 영역을 찾지 못해
  재촬영 안내가 늘 수 있으므로 입력 크기를 모델 버전에도 포함한다.
- 작은 운영 호스트의 JVM 힙은 `JAVA_TOOL_OPTIONS=-Xmx256m -Xms64m`로 제한한다.
  모델 추론은 네이티브 메모리도 쓰므로 JVM 힙 외 여유가 필요하다. 호스트 swap 설정은 변경하지 않는다.
- 공개 검증 모델의 정확한 버전·체크섬·사용 제한은 `docs/hair-analysis-model-NOTICE.txt`에 기록한다.
  모델을 배치하는 경우 해당 NOTICE도 모델 파일과 함께 보관한다.
- 현재 운영 분석 설정은 `config/hair-analysis.prod.env`에서 관리하며 배포 시 `PROD_ENV` 뒤에
  적용한다(동일 키는 이 파일의 값이 우선). 기존 시크릿은 변경하거나 다시 읽어 올 필요가 없다.
  분석을 끄거나 입력 크기를 바꿀 때는 이 파일을 수정하고 Actions로 배포한다.
  서버 `.env`만 수정하면 다음 배포에서 다시 덮어써진다.
- 인스턴스 역할에는 기존 업로드 권한과 함께 시술 사진 객체에 대한 `s3:GetObject`가 필요하다.
  서버가 분석 시 S3 원본을 직접 읽으며 Presigned URL을 외부 AI 서비스에 전달하지 않는다.

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
docker exec heddy-v2-app printenv SHARE_PUBLIC_BASE_URL # https://heddy.site/s 여야 한다
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
