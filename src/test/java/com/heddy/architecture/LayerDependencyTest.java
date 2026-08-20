package com.heddy.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * 헥사고날 의존 방향을 강제한다. 규칙을 어긴 코드는 컴파일은 통과하지만 이 테스트에서 막힌다.
 *
 * <p>패키지 표기는 모두 {@link #BASE} 접두를 붙인 절대 경로로 쓴다. {@code ..domain..} 같은 상대 표기는
 * 나중에 {@code adapter.out.persistence.account.domain} 같은 패키지가 생기면 오탐한다.
 *
 * <p>검사 대상이 0개인 규칙은 ArchUnit 이 기본적으로 실패시킨다. 그 안전장치를 끄는
 * {@code allowEmptyShould(true)} 는 오늘 실제로 대상이 없는 규칙에만, 언제 지울지 적어서 붙인다.
 * 무조건 붙이면 패키지명을 잘못 적은 규칙이 영원히 조용히 통과한다.
 */
class LayerDependencyTest {

    private static final String BASE = "com.heddy";

    private static final String DOMAIN = BASE + ".domain..";
    private static final String DOMAIN_PORT_IN = BASE + ".domain..port.in..";
    private static final String DOMAIN_PORT_OUT = BASE + ".domain..port.out..";
    private static final String APPLICATION = BASE + ".application..";
    private static final String ADAPTER = BASE + ".adapter..";
    private static final String ADAPTER_IN = BASE + ".adapter.in..";
    private static final String ADAPTER_OUT = BASE + ".adapter.out..";
    private static final String PERSISTENCE = BASE + ".adapter.out.persistence..";
    private static final String INFRASTRUCTURE = BASE + ".infrastructure..";
    private static final String GLOBAL = BASE + ".global..";

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE);
    }

    // ---------------------------------------------------------------- 의존 방향

    @Test
    @DisplayName("domain 은 바깥 계층·프레임워크·global 에 의존하지 않는다")
    void domainDependsOnNothingOutside() {
        // global 도 금지한다. global.error.ErrorCode 가 HttpStatus 를 들고 있어,
        // 도메인이 공통 예외를 쓰는 순간 스프링 웹이 도메인에 전이 의존으로 딸려 들어온다.
        // 도메인 예외는 domain 안에 자체 타입으로 두고 ErrorCode 번역은 application·adapter 가 한다.
        // 대상 0개 — 도메인 클래스가 들어오는 #11 이 머지되면 아래 allowEmptyShould 를 지운다.
        noClasses()
                .that().resideInAPackage(DOMAIN)
                .should().dependOnClassesThat().resideInAnyPackage(
                        ADAPTER,
                        APPLICATION,
                        INFRASTRUCTURE,
                        GLOBAL,
                        "org.springframework..",
                        "jakarta.persistence..",
                        "jakarta.validation..",
                        "jakarta.servlet..",
                        "org.hibernate..",
                        "com.fasterxml.jackson..")
                .because("도메인은 순수해야 한다. 기술 의존은 포트 뒤에 둔다")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    @DisplayName("application 은 adapter 에 의존하지 않는다")
    void applicationDoesNotDependOnAdapter() {
        // 대상 0개 — application 에 첫 유스케이스 구현이 들어오면 allowEmptyShould 를 지운다.
        noClasses()
                .that().resideInAPackage(APPLICATION)
                .should().dependOnClassesThat().resideInAPackage(ADAPTER)
                .because("유스케이스는 아웃바운드 포트만 알아야 한다")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    @DisplayName("adapter.in 은 adapter.out 을 직접 참조하지 않는다")
    void inboundAdapterDoesNotDependOnOutboundAdapter() {
        check(noClasses()
                .that().resideInAPackage(ADAPTER_IN)
                .should().dependOnClassesThat().resideInAPackage(ADAPTER_OUT)
                .because("어댑터끼리 붙으면 유스케이스를 건너뛴다. 인바운드 포트를 거친다"));
    }

    @Test
    @DisplayName("adapter.out 은 adapter.in 을 참조하지 않는다")
    void outboundAdapterDoesNotDependOnInboundAdapter() {
        check(noClasses()
                .that().resideInAPackage(ADAPTER_OUT)
                .should().dependOnClassesThat().resideInAPackage(ADAPTER_IN)
                .because("영속성 어댑터가 웹 DTO 를 그대로 반환하는 지름길을 막는다"));
    }

    @Test
    @DisplayName("adapter.in 은 아웃바운드 포트를 직접 주입받지 않는다")
    void inboundAdapterDoesNotDependOnOutboundPort() {
        check(noClasses()
                .that().resideInAPackage(ADAPTER_IN)
                .should().dependOnClassesThat().resideInAPackage(DOMAIN_PORT_OUT)
                .because("컨트롤러가 아웃바운드 포트를 직접 쓰면 유스케이스와 트랜잭션 경계를 건너뛴다"));
    }

    @Test
    @DisplayName("infrastructure 는 adapter·application 에 의존하지 않는다")
    void infrastructureDoesNotDependOnAdapterOrApplication() {
        check(noClasses()
                .that().resideInAPackage(INFRASTRUCTURE)
                .should().dependOnClassesThat().resideInAnyPackage(ADAPTER, APPLICATION)
                .because("인증 필터가 컨트롤러를 참조하면 프레임워크 구성과 유스케이스가 서로를 물게 된다"));
    }

    @Test
    @DisplayName("global 은 도메인·유스케이스·어댑터에 의존하지 않는다")
    void globalDoesNotDependOnDomains() {
        check(noClasses()
                .that().resideInAPackage(GLOBAL)
                .should().dependOnClassesThat().resideInAnyPackage(DOMAIN, APPLICATION, ADAPTER)
                .because("global 은 도메인과 무관한 횡단 공통이다"));
    }

    @Test
    @DisplayName("JPA 애노테이션은 영속성 어댑터 안에서만 쓴다")
    void jpaOnlyInPersistenceAdapter() {
        check(noClasses()
                .that().resideOutsideOfPackage(PERSISTENCE)
                .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..")
                .because("영속성 기술이 새어 나가면 포트가 의미를 잃는다"));
    }

    // ---------------------------------------------------------------- 포트 형태

    @Test
    @DisplayName("아웃바운드 포트는 전부 인터페이스다")
    void outboundPortsAreInterfaces() {
        // 대상 0개 — 포트가 들어오는 #11 이 머지되면 allowEmptyShould 를 지운다.
        classes()
                .that().resideInAPackage(DOMAIN_PORT_OUT)
                .should().beInterfaces()
                .because("구현을 바깥에 두고 의존을 뒤집는 것이 포트의 전부다")
                .allowEmptyShould(true)
                .check(classes);
    }

    @Test
    @DisplayName("인바운드 포트의 UseCase 타입은 인터페이스다")
    void useCasePortsAreInterfaces() {
        // port.in 에는 Command·Result 를 record 로 두므로 UseCase 로 끝나는 타입만 검사한다.
        // 대상 0개 — 포트가 들어오는 #11 이 머지되면 allowEmptyShould 를 지운다.
        classes()
                .that().resideInAPackage(DOMAIN_PORT_IN)
                .and().haveSimpleNameEndingWith("UseCase")
                .should().beInterfaces()
                .because("유스케이스 계약은 구현과 분리돼야 한다")
                .allowEmptyShould(true)
                .check(classes);
    }

    // ---------------------------------------------------------------- 순환

    @Test
    @DisplayName("도메인 사이에 순환 의존이 없다")
    void domainsAreFreeOfCycles() {
        // 대상 0개 — 도메인 클래스가 들어오는 #11 이 머지되면 allowEmptyShould 를 지운다.
        slices()
                .matching(BASE + ".domain.(*)..")
                .should().beFreeOfCycles()
                .because("도메인끼리 서로를 물면 어느 쪽도 따로 이해할 수 없다")
                .allowEmptyShould(true)
                .check(classes);
    }

    /** 오늘 실제 검사 대상이 있는 규칙. 대상이 비면 ArchUnit 이 실패시킨다. */
    private static void check(ArchRule rule) {
        rule.check(classes);
    }
}
