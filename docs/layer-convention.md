# 레이어 컨벤션

이 서버는 헥사고날 아키텍처(포트-어댑터)를 쓴다. 이 문서는 패키지를 어디에 만들지, 무엇을 무엇에 의존해도 되는지를 정한다.
여기 적힌 의존 방향은 `src/test/java/com/heddy/architecture/LayerDependencyTest.java` 에서 ArchUnit 으로 강제된다. 문서와 테스트가 어긋나면 테스트가 정답이다.

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
│       ├── persistence/{도메인}/{entity, repository, adapter}
│       ├── storage/   오브젝트 스토리지(사진 업로드·presigned URL)
│       ├── ai/        모발 분석 서버 연동
│       └── push/      푸시 알림 발송
├── infrastructure/security/{jwt, auth, config}
└── global/{error, response, filter, config}
```

각 패키지에는 역할을 한 줄로 적은 `package-info.java` 가 있다. 새 패키지를 만들면 함께 만든다.

`adapter` 아래 도메인별 하위 패키지(`in/web/account`, `out/persistence/account` 등)는 미리 만들지 않는다.
해당 도메인의 첫 컨트롤러·엔티티가 생기는 PR 에서 같이 만든다. 비어 있는 패키지를 미리 깔아두면 무엇이 실제로 구현됐는지 구조만 보고는 알 수 없다.
반면 `domain` 과 `application` 은 도메인 목록 자체가 설계 산출물이라 7개를 모두 미리 둔다.

---

## 2. 레이어별 책임

| 레이어 | 무엇을 두는가 | 무엇을 알아도 되는가 |
|---|---|---|
| `domain.model` | 값 객체, 엔티티(JPA 아님), 도메인 규칙 | 같은 도메인 안의 것 + JDK |
| `domain.port.in` | 유스케이스 인터페이스. 입력은 Command, 출력은 Result | `domain.model` |
| `domain.port.out` | 저장·조회·외부 호출 인터페이스 | `domain.model` |
| `domain.service` | 여러 모델에 걸친 도메인 규칙. 상태 없음 | `domain.model` |
| `application.{도메인}.service` | 인바운드 포트 구현. 아웃바운드 포트 조합, `@Transactional` | `domain..`, 스프링 |
| `adapter.in.web` | HTTP 요청·응답 변환, 검증, 인증 컨텍스트 해석 | `domain.port.in`, `global` |
| `adapter.out.*` | 아웃바운드 포트 구현. JPA·S3·HTTP 클라이언트 | `domain.port.out`, `domain.model` |
| `infrastructure` | 프레임워크 구성. 시큐리티 필터 체인, JWT | 스프링, `global` |
| `global` | 도메인과 무관한 횡단 공통 | JDK, 스프링 |

### 의존 방향 (ArchUnit 강제 규칙)

1. `domain..` 은 `adapter..` · `application..` · `infrastructure..` · `org.springframework..` · `jakarta.persistence..` 를 import 하지 않는다.
2. `application..` 은 `adapter..` 를 import 하지 않는다. 아웃바운드 포트만 안다.
3. `adapter.in..` 은 `adapter.out..` 을 직접 참조하지 않는다. 어댑터끼리 붙으면 유스케이스를 건너뛴다.
4. `global..` 은 `domain..` · `application..` · `adapter..` 를 import 하지 않는다.
5. `jakarta.persistence..` 는 `adapter.out.persistence..` 안에서만 쓴다.

한 줄로 줄이면 **모든 의존은 안쪽(도메인)을 향한다.** 바깥이 안쪽을 호출하고, 안쪽이 바깥을 쓸 때는 자신이 정의한 포트 인터페이스를 통한다.

골격 단계에는 클래스가 없는 패키지가 있어서 테스트는 `allowEmptyShould(true)` 로 둔다. 클래스가 생기는 순간부터 실제로 검사된다.

---

## 3. 포트를 전 도메인에 두기로 한 결정

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

## 4. 새 도메인을 추가할 때 만드는 파일

도메인 이름을 `sample`, 유스케이스를 `RegisterSample` 이라 하면:

| 순서 | 파일 | 내용 |
|---|---|---|
| 1 | `domain/sample/model/Sample.java` | 도메인 모델. JPA 애노테이션 없음 |
| 2 | `domain/sample/port/in/RegisterSampleUseCase.java` | 유스케이스 인터페이스 |
| 3 | `domain/sample/port/in/RegisterSampleCommand.java` | 입력 타입. 생성 시점에 형식 검증 |
| 4 | `domain/sample/port/out/SampleRepositoryPort.java` | 저장·조회 추상화. 반환 타입은 도메인 모델 |
| 5 | `application/sample/service/RegisterSampleService.java` | 1의 구현. `@Service` + `@Transactional` |
| 6 | `adapter/out/persistence/sample/SampleEntity.java` | JPA 엔티티. `BaseEntity` 상속 |
| 7 | `adapter/out/persistence/sample/SampleJpaRepository.java` | 스프링 데이터 리포지토리 |
| 8 | `adapter/out/persistence/sample/SamplePersistenceAdapter.java` | 4의 구현. 엔티티↔모델 변환 |
| 9 | `adapter/in/web/sample/controller/SampleController.java` | 2를 주입받아 호출 |
| 10 | `adapter/in/web/sample/dto/SampleRequest.java`, `SampleResponse.java` | HTTP 표현. 도메인 모델을 그대로 노출하지 않는다 |
| 11 | 새로 만든 패키지마다 `package-info.java` | 역할 한 줄 |
| 12 | `resources/db/migration/V{n}__*.sql` | 새 버전 파일 추가. 기존 파일 수정 금지 |

테스트는 최소 두 종류를 쓴다.

- 유스케이스 단위 테스트 — 아웃바운드 포트를 가짜 구현으로 바꾼다. 스프링 컨텍스트를 띄우지 않는다.
- 컨트롤러 통합 테스트 — 상태 코드, 응답 래퍼, 에러 코드, 소유권 검증(403)을 확인한다.

### 엔티티와 도메인 모델을 나누는 이유

`SampleEntity` 와 `Sample` 은 필드가 거의 같아 보인다. 그래도 합치지 않는다.
합치면 도메인 모델에 `jakarta.persistence` 가 들어오고 규칙 1·5 가 깨진다. 그 순간 도메인은 JPA 의 로딩 전략·프록시·식별자 생성 규칙에 묶인다.
변환 코드는 `SamplePersistenceAdapter` 한 곳에만 둔다.

---

## 5. 위치가 헷갈리는 것들

- **`BaseEntity`** — `adapter/out/persistence/BaseEntity.java`. `@MappedSuperclass` 와 JPA Auditing 이 붙은 영속성 기술 조각이라 `global` 이 아니라 영속성 어댑터에 둔다. `global` 은 도메인과 무관한 공통이지 기술 공통이 아니고, 규칙 5(JPA 는 영속성 어댑터 안에서만)와도 이 위치라야 맞는다.
- **공통 응답 래퍼·에러 코드** — `global/response`, `global/error`. 특정 도메인을 모르므로 규칙 4 를 지킨다.
- **JWT·인증 필터** — `infrastructure/security`. 유스케이스가 아니라 프레임워크 구성이다.
- **헬스 체크** — `adapter/in/web/health`. 유스케이스가 없어 포트를 거치지 않는 운영용 엔드포인트다. 이 예외는 여기까지만 허용한다.
