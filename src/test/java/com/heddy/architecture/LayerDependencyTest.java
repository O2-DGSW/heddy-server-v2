package com.heddy.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 헥사고날 의존 방향을 강제한다. 규칙을 어긴 코드는 컴파일은 통과하지만 이 테스트에서 막힌다.
 *
 * <p>골격 단계에서는 아직 클래스가 없는 패키지가 있어 {@code allowEmptyShould(true)} 를 쓴다.
 * 규칙이 비어 있어도 통과하도록 두되, 클래스가 생기는 순간부터 실제로 검사된다.
 */
class LayerDependencyTest {

    private static final String BASE = "com.heddy";

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE);
    }

    private static void check(ArchRule rule) {
        rule.allowEmptyShould(true).check(classes);
    }

    @Test
    @DisplayName("domain 은 adapter·application·스프링·JPA 에 의존하지 않는다")
    void domainDependsOnNothingTechnical() {
        check(noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        BASE + ".adapter..",
                        BASE + ".application..",
                        BASE + ".infrastructure..",
                        "org.springframework..",
                        "jakarta.persistence..")
                .because("도메인은 순수해야 한다. 기술 의존은 포트 뒤에 둔다"));
    }

    @Test
    @DisplayName("application 은 adapter 에 의존하지 않는다")
    void applicationDoesNotDependOnAdapter() {
        check(noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAPackage(BASE + ".adapter..")
                .because("유스케이스는 아웃바운드 포트만 알아야 한다"));
    }

    @Test
    @DisplayName("adapter.in 은 adapter.out 을 직접 참조하지 않는다")
    void inboundAdapterDoesNotDependOnOutboundAdapter() {
        check(noClasses()
                .that().resideInAPackage(BASE + ".adapter.in..")
                .should().dependOnClassesThat().resideInAPackage(BASE + ".adapter.out..")
                .because("어댑터끼리 붙으면 유스케이스를 건너뛴다. 인바운드 포트를 거친다"));
    }

    @Test
    @DisplayName("global 은 도메인 패키지에 의존하지 않는다")
    void globalDoesNotDependOnDomains() {
        check(noClasses()
                .that().resideInAPackage(BASE + ".global..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        BASE + ".domain..",
                        BASE + ".application..",
                        BASE + ".adapter..")
                .because("global 은 도메인과 무관한 횡단 공통이다"));
    }

    @Test
    @DisplayName("JPA 애노테이션은 영속성 어댑터 안에서만 쓴다")
    void jpaOnlyInPersistenceAdapter() {
        check(noClasses()
                .that().resideOutsideOfPackage(BASE + ".adapter.out.persistence..")
                .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..")
                .because("영속성 기술이 새어 나가면 포트가 의미를 잃는다"));
    }
}
