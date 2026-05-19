package com.dove.api.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 아키텍처 가드레일.
 *
 * <p>인증/사용자 도메인 분리 결정의 핵심 불변식을 ArchUnit으로 컴파일 타임에 강제한다.
 * 개별 PR에서 누군가 의도치 않게 boundary를 깰 때 빌드가 실패하도록 방어한다.
 */
@AnalyzeClasses(packages = "com.dove")
public class ArchitectureTest {

    /**
     * Credential은 비밀번호 해시를 보유하므로 domain/auth 외부에서 직접 import 금지.
     * 다른 도메인이 비밀번호 검증을 해야 한다면 CredentialQueryService 같은 명시적 인터페이스를 통해서만.
     */
    @ArchTest
    static final ArchRule credential_must_not_be_imported_outside_auth =
            noClasses().that()
                    .resideOutsideOfPackage("com.dove.auth..")
                    .and().resideOutsideOfPackage("com.dove.api..")
                    .should().dependOnClassesThat().haveFullyQualifiedName("com.dove.auth.domain.entity.Credential")
                    .because("Credential 엔티티는 domain/auth, application/api 외부에서 직접 import 금지 (비밀번호 해시 격리)");

    /**
     * 양방향 의존 차단. domain/auth → domain/user 단방향만 허용 (InviteCode가 MemberRole 사용).
     */
    @ArchTest
    static final ArchRule user_must_not_depend_on_auth =
            noClasses().that().resideInAPackage("com.dove.user..")
                    .should().dependOnClassesThat().resideInAPackage("com.dove.auth..")
                    .because("domain/user는 domain/auth를 import하면 안 된다 (단방향 의존: auth → user)");

    /**
     * 인프라 레이어는 도메인을 모른다 (onion 원칙).
     */
    @ArchTest
    static final ArchRule security_infrastructure_must_not_depend_on_domain =
            noClasses().that().resideInAPackage("com.dove.security..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "com.dove.auth..",
                            "com.dove.user..",
                            "com.dove.stock..",
                            "com.dove.market..",
                            "com.dove.indicator..",
                            "com.dove.screening..",
                            "com.dove.eventretry..",
                            "com.dove.krx..")
                    .because("infrastructure/security는 도메인을 모르는 인프라 레이어");

    /**
     * 주식 도메인은 MemberProfile을 직접 참조하지 않고 Long memberId만 사용.
     * cross-schema 결합 차단.
     */
    @ArchTest
    static final ArchRule stock_domains_must_not_import_member_profile =
            noClasses().that().resideInAnyPackage(
                            "com.dove.stock..",
                            "com.dove.indicator..",
                            "com.dove.screening..",
                            "com.dove.market..",
                            "com.dove.eventretry..",
                            "com.dove.krx..")
                    .should().dependOnClassesThat().haveFullyQualifiedName("com.dove.user.domain.entity.MemberProfile")
                    .because("주식 도메인은 MemberProfile을 직접 참조하지 않고 Long memberId만 사용 (cross-schema 결합 차단)");

    /**
     * CQRS 컨벤션: domain/auth, domain/user의 application service는 모두
     * CommandService 또는 QueryService suffix를 가져야 한다.
     */
    @ArchTest
    static final ArchRule cqrs_services_must_have_suffix =
            classes().that().resideInAnyPackage(
                            "com.dove.auth.application.service..",
                            "com.dove.user.application.service..")
                    .should().haveSimpleNameEndingWith("CommandService")
                    .orShould().haveSimpleNameEndingWith("QueryService")
                    .because("사내 컨벤션: application service는 CQRS 분리 (CommandService / QueryService)");
}
