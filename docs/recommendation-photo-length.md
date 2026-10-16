# 사진과 추천 길이 연결

최신 시술의 AFTER 사진을 서버 내부 ONNX 모델로 분할하고, 얼굴 대비 머리 영역의 끝 위치를 이용해 보수적으로 길이를 추정한다. 사용자 사진을 외부 AI로 보내지 않으며 성별을 추론하지 않는다. 기존 색상 균일도·대칭·볼륨·거칠기 분석과는 별도 기능이다. 그 4개 지표의 분석 엔진을 이 모델로 대체하지 않는다.

- 직접 입력한 머리 길이보다 긴 길이로 자동 변경하지 않는다. 사진 업로드 후 갱신한 프로필은 우선한다.
- 시술 날짜가 같으면 기록 생성 시각으로 최신 기록을 정하며, 그 기록에서 가장 최근 업로드한 AFTER 파일을 사용한다. 기존 사진을 남겨둔 추가 업로드도 새 입력으로 반영한다.
- 최신 시술에 AFTER 사진이 없으면 예전 시술 사진으로 대체하지 않는다. BEFORE/OTHER는 현재 길이 판단에 사용하지 않는다.
- 파일 소유자·READY·TREATMENT_PHOTO를 확인한다. 분석은 파일별 캐시 최대 256개로 제한하며 이미지 바이트는 보관하지 않는다.
- 사진·모델·분석 상태·적용 길이가 추천 입력 해시에 포함된다. latest 재조회는 달라진 입력을 재계산하고 같은 입력이면 재사용한다.
- 길이 입력도 없고 사진 길이도 판정할 수 없으면 `422 RECOMMENDATION_HAIR_LENGTH_REQUIRED`를 반환한다. 임의의 장발 기본 추천은 제공하지 않는다.
- `recommendation_basis.photo_analysis`는 `file_id`, `model_version`, `status`, `observed_length`, `applied`를 반환한다. `current_hair.hair_length`는 실제 점수/필터에 사용한 길이다. 프로필 DB 자체는 바꾸지 않는다.
- 상태: `ASSESSED`, `UNCERTAIN`, `UNREADABLE`, `UNAVAILABLE`, `DISABLED`, `NEWER_PROFILE`, `NO_RECENT_PHOTO`. 판정됐어도 입력 길이보다 길면 applied=false다.

## 제약

길이 전용 학습 분류기가 아니라 머리/얼굴 분할 결과에 기반한 규칙이다. 뒷모습, 작은 얼굴, 여러 사람, 잘린 머리, 측면 등에서 판정을 보류한다. 묶은 머리나 가려진 머리끝은 짧게 보일 수 있으므로 사용자가 입력한 길이와 비교해 확인할 필요가 있다. JPEG/PNG만 읽을 수 있고 HEIC는 UNREADABLE이다. 어깨 아래를 가슴 아래로 세분화하지 않는다. 제공 모델의 픽셀 확률을 길이 분류의 정확도라고 표시하지 않는다.

모델은 Docker 빌드에서 고정 커밋과 SHA256으로 받으며 라이선스를 동봉한다. 운영 compose의 `HAIR_PHOTO_LENGTH_ENABLED` 기본값은 true, 테스트/로컬 기본값은 false다. 기존 `HAIR_ANALYSIS_ENABLED`는 별도 설정이다. 롤백/비활성화 역시 GitHub/Actions 경로로 수행한다.

## 검증

단위 테스트는 길이 경계, 불확실/잘림/복수 얼굴, 계정 소유권, BEFORE 배제, 최신 프로필 우선, 짧은 입력 보호를 검증한다. API/실제 PostgreSQL 테스트는 업로드 변경에 따른 latest 재생성과 분석 근거 저장/재사용 및 길이 누락 오류를 검증한다.

실제 모델 smoke test는 `PHOTO_LENGTH_TEST_MODEL`에 고정 ONNX 경로, `PHOTO_LENGTH_TEST_IMAGES`에 검수한 SYSTEM 카탈로그 사진 0~5.jpg 경로를 설정해 실행한다. 사용자 사진은 테스트 fixture로 복사하지 않는다. 측면 장발/뒷모습 보브/손·안경에 가린 얼굴은 UNCERTAIN, 정면 숏 2개는 SHORT를 기대한다.

배포 후 운영 컨테이너에서 읽기 전용 감사:

```sh
docker exec heddy-v2-app java -Djava.awt.headless=true -Xmx256m \
  -Dloader.main=com.heddy.diagnostics.RecommendationReadOnlyAudit \
  -cp /app/app.jar org.springframework.boot.loader.launch.PropertiesLauncher
```

Spring 서버나 마이그레이션을 새로 실행하지 않는다. DB 읽기 전용 트랜잭션, 기존 S3 내부 읽기, 실제 배포된 resolver/scorer만 사용한다. 최대 100개 활성 계정을 검사하며 계정 해시, 입력·적용 길이, 분석 상태와 추천 이름/점수만 출력한다. 사진·연락처·기록 원문·키를 출력하거나 추천/프로필을 저장하지 않는다.

V33은 실제 SYSTEM 썸네일을 검수해 확인된 길이 오분류를 교정하고 그룹/묶은머리/불명확한 옛 사진 7개를 추천에서 제외한다. 사용자 기록이나 파일은 삭제하지 않는다.

V34는 배포 후 실제 상위 추천 썸네일 대조에서 발견한 `미디엄 스타일 32`를 비활성화한다. 사진은 땋은 머리인데 미디엄 CUT 및 선호 메타데이터로 추천되고 있어 해당 메타데이터를 검증할 수 없다. 기존 파일/기록은 보존한다.
