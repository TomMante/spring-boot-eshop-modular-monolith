package io.github.tommante.eshopmodularmonolith;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CompositeArchRule;

import java.util.List;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "io.github.tommante.eshopmodularmonolith", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String ROOT = "io.github.tommante.eshopmodularmonolith.";
    private static final List<String> MODULES = List.of("basket", "catalog", "ordering");
    private static final String[] OUTBOX_PACKAGES = {"..outboxcore..", "..outboxjpa..", "..outboxmongo.."};

    @ArchTest
    void givenDomainCode_whenScanned_thenItDependsOnNoFrameworkAndNoOuterLayer(JavaClasses classes) {
        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta..", "..application..", "..infrastructure..", "..contract..")
                .orShould().dependOnClassesThat().resideInAnyPackage(OUTBOX_PACKAGES)
                .check(classes);
    }

    @ArchTest
    void givenContractCode_whenScanned_thenItDependsOnNoFrameworkAndNoOtherLayer(JavaClasses classes) {
        noClasses().that().resideInAPackage("..contract..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta..", "..domain..", "..application..", "..infrastructure..")
                .orShould().dependOnClassesThat().resideInAnyPackage(OUTBOX_PACKAGES)
                .check(classes);
    }

    @ArchTest
    void givenEachModule_whenScanned_thenItsApplicationLayerDoesNotReachItsOwnInfrastructure(JavaClasses classes) {
        var perModuleRules = MODULES.stream()
                .map(ArchitectureTest::applicationDoesNotReachOwnInfrastructure)
                .toList();
        CompositeArchRule.of(perModuleRules).check(classes);
    }

    @ArchTest
    void givenEachModule_whenScanned_thenItReachesOtherModulesOnlyThroughTheirContract(JavaClasses classes) {
        var perModulePairRules = MODULES.stream()
                .flatMap(module -> MODULES.stream()
                        .filter(otherModule -> !otherModule.equals(module))
                        .map(otherModule -> reachesOnlyContractOf(module, otherModule)))
                .toList();
        CompositeArchRule.of(perModulePairRules).check(classes);
    }

    private static ArchRule applicationDoesNotReachOwnInfrastructure(String module) {
        return noClasses().that().resideInAPackage(ROOT + module + "..application..")
                .should().dependOnClassesThat().resideInAPackage(ROOT + module + "..infrastructure..");
    }

    private static ArchRule reachesOnlyContractOf(String module, String otherModule) {
        return noClasses().that().resideInAPackage(ROOT + module + "..")
                .should().dependOnClassesThat(
                        resideInAPackage(ROOT + otherModule + "..").and(not(resideInAPackage("..contract.."))));
    }
}
