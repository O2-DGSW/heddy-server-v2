# 레이어 컨벤션

이 서버는 헥사고날 아키텍처(포트-어댑터)를 쓴다. 이 문서는 패키지를 어디에 만들지, 무엇을 무엇에 의존해도 되는지를 정한다.

이 문서에는 두 종류의 문장이 섞여 있다.

- **강제** — `src/test/java/com/heddy/architecture/LayerDependencyTest.java` 의 ArchUnit 규칙이 어기면 빌드를 깬다. 3절에 규칙 전체를 적었다.
- **합의** — 테스트가 잡지 못하므로 리뷰에서 본다.

어느 쪽인지 표시 없이 적힌 문장은 없다. 문서와 테스트가 어긋나면 테스트가 정답이다.

---

## 1. 패키지 구조

```
com.heddy
├── domain/{account,treatment,analysis,style,sharing,reservation,notification}/
│   ├── model/        도메인 모델과 불변식
│   ├── port/in/      유스케이스 인터페이스 + Command·Result
│   ├── port/out/     영속성·외부 연동 추상화
│   └── service/      모델 하나로 표현하기 어려운 도메인 규칙
├── application/{도메인}/service/    유스케이스 구현, 트랜잭션 경계
├── adapter/
│   ├── in/web/{도메인}/{controller, dto}
│   └── out/
│       ├── persistence/{도메인}/    JPA 엔티티·리포지토리·포트 구현
│       ├── storage/   오브젝트 스토리지(사진 업로드·presigned URL)
│       ├── ai/        모발 분석 서버 연동
│       └── push/      푸시 알림 발송
├── infrastructure/security/{jwt, auth, config}
└── global/{error, response, filter, config}
```

`adapter/out/persistence/{도메인}/` 아래는 평탄하게 둔다. 도메인당 파일이 엔티티·리포지토리·어댑터 3~4개뿐이라 `entity/`·`repository/`·`adapter/` 로 한 번 더 나누면 디렉터리가 파일보다 많아진다. `adapter/in/web/{도메인}/` 은 컨트롤러와 DTO 개수 차이가 커서 `controller/`·`dto/` 로 나눈다.

패키지마다 역할을 한 줄로 적은 `package-info.java` 를 둔다. 새 패키지를 만들면 함께 만든다. (합의)

`adapter` 아래 도메인별 하위 패키지는 미리 만들지 않는다. 해당 도메인의 첫 컨트롤러·엔티티가 생기는 PR 에서 같이 만든다. 비어 있는 패키지를 미리 깔아두면 무엇이 실제로 구현됐는지 구조만 보고는 알 수 없다. 반면 `domain` 과 `application` 은 도메인 목록 자체가 설계 산출물이라 7개를 모두 미리 둔다. (합의)

### `.gitignore` 주의

빌드 산출물 패턴은 반드시 `/out/` 처럼 앵커(`/`)를 붙인다. 앵커가 없으면 경로 어디에 있든 같은 이름의 디렉터리가 무시되고, 이 구조는 `port/out` 과 `adapter/out` 을 쓰므로 소스가 통째로 커밋에서 빠진다. CI 에 이를 막는 스텝이 있다(`.github/workflows/ci.yml`).

---

## 2. 레이어별 책임

| 레이어 | 무엇을 두는가 |
|---|---|
| `domain.model` | 값 객체, 도메인 엔티티(JPA 아님), 도메인 규칙, 도메인 예외 |
| `domain.port.in` | 유스케이스 인터페이스와 그 입출력 타입(Command·Result) |
| `domain.port.out` | 저장·조회·외부 호출 인터페이스 |
| `domain.service` | 여러 모델에 걸친 도메인 규칙. 상태 없음 |
| `application.{도메인}.service` | 인바운드 포트 구현. 아웃바운드 포트 조합, `@Transactional` |
| `adapter.in.web` | HTTP 요청·응답 변환, 검증, 인증 컨텍스트 해석 |
| `adapter.out.*` | 아웃바운드 포트 구현. JPA·S3·HTTP 클라이언트 |
| `infrastructure` | 프레임워크 구성. 시큐리티 필터 체인, JWT |
| `global` | 도메인과 무관한 횡단 공통(응답 래퍼, 에러 코드, 필터) |

한 줄로 줄이면 **모든 의존은 안쪽(도메인)을 향한다.** 바깥이 안쪽을 호출하고, 안쪽이 바깥을 쓸 때는 자신이 정의한 포트 인터페이스를 통한다.

---

## 3. ArchUnit 이 강제하는 규칙

아래 11개가 전부다. 여기 없는 것은 강제되지 않는다.

| # | 규칙 |
|---|---|
| 1 | `domain..` 은 `adapter..` · `application..` · `infrastructure..` · `global..` · `org.springframework..` · `jakarta.persistence..` · `jakarta.validation..` · `jakarta.servlet..` · `org.hibernate..` · `com.fasterxml.jackson..` 에 의존하지 않는다 |
| 2 | `application..` 은 `adapter..` 에 의존하지 않는다 |
| 3 | `adapter.in..` 은 `adapter.out..` 에 의존하지 않는다 |
| 4 | `adapter.out..` 은 `adapter.in..` 에 의존하지 않는다 |
| 5 | `adapter.in..` 은 `domain..port.out..` 에 의존하지 않는다 |
| 6 | `infrastructure..` 는 `adapter..` · `application..` 에 의존하지 않는다 |
| 7 | `global..` 은 `domain..` · `application..` · `adapter..` 에 의존하지 않는다 |
| 8 | `jakarta.persistence..` 는 `adapter.out.persistence..` 안에서만 쓴다 |
| 9 | `domain..port.out..` 의 모든 타입은 인터페이스다 |
| 10 | `domain..port.in..` 에서 이름이 `UseCase` 로 끝나는 타입은 인터페이스다 |
| 11 | 도메인 사이에 순환 의존이 없다 |

**규칙 5 가 이 목록의 중심이다.** 컨트롤러가 아웃바운드 포트를 직접 주입받으면 유스케이스도, `@Transactional` 경계도 건너뛴다. 포트를 전 도메인에 두기로 한 이유 자체가 무효가 된다.

**규칙 9·10 이 나뉘어 있는 이유.** `port/in` 에는 유스케이스 인터페이스와 함께 입출력 타입인 Command·Result 를 `record` 로 둔다. 이들을 별도 하위 패키지로 빼면 파일 서너 개짜리 패키지가 도메인마다 생기므로, 대신 규칙 10 을 이름(`*UseCase`)으로 좁혔다. `port/out` 에는 데이터 타입을 두지 않는다(반환 타입은 `domain.model`)므로 규칙 9 는 전체를 검사한다.

### 검사 대상이 0개인 규칙

지금은 `domain` 과 `application` 에 클래스가 없어 규칙 1·2·9·10·11 은 검사할 대상이 없다. 이 규칙들에만 `allowEmptyShould(true)` 를 붙였고, 언제 지울지를 테스트 파일 주석에 적었다(`#11` 머지 후).

나머지 규칙에는 붙이지 않는다. 무조건 붙이면 패키지명을 잘못 적은 규칙이 검사 대상 0개로 영원히 조용히 통과한다. 규칙을 추가하거나 패키지를 리네임할 때는 반드시 위반 클래스를 임시로 심어 테스트가 FAIL 하는 것을 확인한다.

### 규칙 1 이 `global` 까지 막는 이유

`global.error.ErrorCode` 는 `HttpStatus` 를 필드로 들고 있다. 도메인이 공통 예외를 쓰는 순간 스프링 웹이 전이 의존으로 도메인에 딸려 들어오고, ArchUnit 은 직접 의존만 보므로 이를 잡지 못한다. 도메인을 스프링 없이 테스트한다는 명분이 거기서 무너진다.

그래서 **도메인 예외는 `domain` 안에 자체 타입으로 두고, `ErrorCode` 로의 번역은 `application` 또는 `adapter` 가 한다.** `ErrorCode` 에서 `HttpStatus` 를 걷어내는 작업은 `global/error` 를 전면 재작성하는 #9 에서 한다.

---

## 4. 포트를 전 도메인에 두기로 한 결정

일부 도메인만 포트를 두고 단순 CRUD 는 서비스가 리포지토리를 직접 부르는 절충안도 있었지만, 쓰지 않는다.

**그렇게 정한 이유**

- 기준이 "단순한가"이면 사람마다 답이 달라진다. 리뷰마다 그 논쟁을 다시 한다.
- 처음에 단순했던 도메인이 나중에 안 단순해진다. 그때 포트를 끼워 넣으려면 이미 그 서비스를 부르는 코드까지 전부 바뀐다.
- 구조가 도메인마다 다르면 새로 합류한 사람이 파일을 열기 전까지 어느 형태인지 알 수 없다.

**대가 — 알고 받아들인다**

- 필드 하나 고치는 변경에도 포트 인터페이스·구현·엔티티·매핑까지 파일 서너 개를 건드린다.
- 단순 조회 하나에 클래스 수가 3계층 구조보다 2~3배 늘어난다.
- 인터페이스 구현체가 하나뿐인 경우가 대부분이다. 다형성 때문이 아니라 의존 방향을 뒤집으려고 두는 것이다.

이 비용은 도메인 로직을 프레임워크·DB 교체와 분리하고, 유스케이스를 스프링 컨텍스트 없이 테스트할 수 있게 하는 값으로 지불한다.

---

## 5. 새 도메인을 추가할 때 만드는 파일

도메인 이름을 `sample`, 유스케이스를 `RegisterSample` 이라 하면:

| 순서 | 파일 | 내용 |
|---|---|---|
| 1 | `domain/sample/model/Sample.java` | 도메인 모델. JPA 애노테이션 없음 |
| 2 | `domain/sample/port/in/RegisterSampleUseCase.java` | 유스케이스 인터페이스 |
| 3 | `domain/sample/port/in/RegisterSampleCommand.java` | 입력 타입. `record`. 생성 시점에 형식 검증 |
| 4 | `domain/sample/port/out/SampleRepositoryPort.java` | 저장·조회 추상화. 반환 타입은 도메인 모델 |
| 5 | `application/sample/service/RegisterSampleService.java` | **2의 구현**. `@Service` + `@Transactional` |
| 6 | `adapter/out/persistence/sample/SampleEntity.java` | JPA 엔티티. `BaseEntity` 상속 |
| 7 | `adapter/out/persistence/sample/SampleJpaRepository.java` | 스프링 데이터 리포지토리 |
| 8 | `adapter/out/persistence/sample/SamplePersistenceAdapter.java` | **4의 구현**. 엔티티↔모델 변환 |
| 9 | `adapter/in/web/sample/controller/SampleController.java` | 2를 주입받아 호출. 4를 직접 주입받으면 규칙 5 에 걸린다 |
| 10 | `adapter/in/web/sample/dto/SampleRequest.java`, `SampleResponse.java` | HTTP 표현. 도메인 모델을 그대로 노출하지 않는다 |
| 11 | 새로 만든 패키지마다 `package-info.java` | 역할 한 줄 |
| 12 | `resources/db/migration/V{n}__*.sql` | 새 버전 파일 추가. 기존 파일 수정 금지 |

테스트는 최소 두 종류를 쓴다. (합의)

- 유스케이스 단위 테스트 — 아웃바운드 포트를 가짜 구현으로 바꾼다. 스프링 컨텍스트를 띄우지 않는다.
- 컨트롤러 통합 테스트 — 상태 코드, 응답 래퍼, 에러 코드, 소유권 검증(403)을 확인한다.

### 엔티티와 도메인 모델을 나누는 이유

`SampleEntity` 와 `Sample` 은 필드가 거의 같아 보인다. 그래도 합치지 않는다. 합치면 도메인 모델에 `jakarta.persistence` 가 들어오고 규칙 1·8 이 깨진다. 그 순간 도메인은 JPA 의 로딩 전략·프록시·식별자 생성 규칙에 묶인다. 변환 코드는 `SamplePersistenceAdapter` 한 곳에만 둔다.

---

## 6. 위치가 헷갈리는 것들

- **`BaseEntity`** — `adapter/out/persistence/BaseEntity.java`. `@MappedSuperclass` 와 JPA Auditing 이 붙은 영속성 기술 조각이다. `global` 은 "도메인과 무관한 공통"이지 "기술 공통"이 아니고, 규칙 8 과도 이 위치라야 맞는다.
- **`SecurityConfig`** — `infrastructure/security/config/`. `SecurityFilterChain` 은 유스케이스가 아니라 프레임워크 구성이다. JWT 발급·검증도 `infrastructure/security/jwt` 로 간다.
- **`JpaConfig`·`OpenApiConfig`** — `global/config/`. 특정 기술 어댑터에 묶이지 않는 애플리케이션 전역 스위치다.
- **공통 응답 래퍼·에러 코드** — `global/response`, `global/error`. 특정 도메인을 몰라야 한다(규칙 7).
- **헬스 체크** — `adapter/in/web/health`. 유스케이스가 없어 포트를 거치지 않는 운영용 엔드포인트다. 포트 없는 컨트롤러는 여기까지만 허용한다. (합의)
