package ch.admin.bit.jeap.governance.web;

import ch.admin.bit.jeap.governance.archrepo.connector.model.ArchRepoModelDto;
import ch.admin.bit.jeap.governance.archrepo.connector.model.ArchRepoSystemComponentDto;
import ch.admin.bit.jeap.governance.archrepo.connector.model.ArchRepoSystemComponentType;
import ch.admin.bit.jeap.governance.archrepo.connector.model.ArchRepoSystemDto;
import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@UtilityClass
public class ImportModelHelper {

    static final String SYSTEM_A_NAME = "sysa";
    static final String SYSTEM_B_NAME = "sysb";
    static final String SYSTEM_C_NAME = "sysc";

    static final String COMPONENT_A1_NAME = "sysa-comp1-svc";
    static final String COMPONENT_A2_NAME = "sysa-comp2-scs";
    static final String COMPONENT_A3_NAME = "sysa-comp3-svc";

    static final String COMPONENT_B1_NAME = "sysb-comp1-svc";
    static final String COMPONENT_B2_NAME = "sysb-comp2-svc";
    static final String COMPONENT_B3_NAME = "sysb-comp3-svc";

    static final String COMPONENT_C1_NAME = "sysc-comp1-svc";
    static final String COMPONENT_C2_NAME = "sysc-comp2-scs";
    static final String COMPONENT_C3_NAME = "sysc-comp3-svc";

    static final int DEFAULT_SYSTEM_COUNT = 3;
    static final int DEFAULT_COMPONENT_COUNT_PER_SYSTEM = 3;

    static final int SYSTEM_COUNT_LESS = DEFAULT_SYSTEM_COUNT - 1;
    static final int COMPONENT_COUNT_PER_SYSTEM_LESS = DEFAULT_SYSTEM_COUNT - 1;

    static final Set<String> DEFAULT_MODEL_COMPONENT_NAMES = Set.of(
            COMPONENT_A1_NAME, COMPONENT_A2_NAME, COMPONENT_A3_NAME,
            COMPONENT_B1_NAME, COMPONENT_B2_NAME, COMPONENT_B3_NAME,
            COMPONENT_C1_NAME, COMPONENT_C2_NAME, COMPONENT_C3_NAME);

    static final Set<String> LESS_MODEL_COMPONENT_NAMES = Set.of(
            COMPONENT_A1_NAME, COMPONENT_A2_NAME,
            COMPONENT_B1_NAME, COMPONENT_B2_NAME);

    static ArchRepoModelDto createDefaultArchRepoModelDto() {
        var archRepoSystemA = createSystemA(createComponentA1(), createComponentA2(), createComponentA3());
        var archRepoSystemB = createSystemB(createComponentB1(), createComponentB2(), createComponentB3());
        var archRepoSystemC = createDefaultSystemC();

        return ArchRepoModelDto.builder()
                .systems(List.of(archRepoSystemA, archRepoSystemB, archRepoSystemC))
                .build();
    }

    static ArchRepoModelDto createArchRepoModelDtoOneSystemLessOneSystemComponentEachLess() {
        var archRepoSystemA = createSystemA(createComponentA1(), createComponentA2());
        var archRepoSystemB = createSystemB(createComponentB1(), createComponentB2());

        return ArchRepoModelDto.builder()
                .systems(List.of(archRepoSystemA, archRepoSystemB))
                .build();
    }

    private static ArchRepoSystemDto createSystemA(ArchRepoSystemComponentDto... components) {
        return ArchRepoSystemDto.builder()
                .name(SYSTEM_A_NAME)
                .aliases(Set.of("sys-a", "alpha"))
                .systemComponents(Arrays.asList(components))
                .build();
    }

    private static ArchRepoSystemDto createSystemB(ArchRepoSystemComponentDto... components) {
        return ArchRepoSystemDto.builder()
                .name(SYSTEM_B_NAME)
                .aliases(Set.of("sys-b", "bravo"))
                .systemComponents(Arrays.asList(components))
                .build();
    }


    private static ArchRepoSystemDto createDefaultSystemC() {
        return ArchRepoSystemDto.builder()
                .name(SYSTEM_C_NAME)
                .aliases(Set.of("sys-c", "charlie"))
                .systemComponents(Arrays.asList(
                        ArchRepoSystemComponentDto.builder()
                                .name(COMPONENT_C1_NAME)
                                .type(ArchRepoSystemComponentType.BACKEND_SERVICE)
                                .build(),
                        ArchRepoSystemComponentDto.builder()
                                .name(COMPONENT_C2_NAME)
                                .type(ArchRepoSystemComponentType.SELF_CONTAINED_SYSTEM)
                                .build(),
                        ArchRepoSystemComponentDto.builder()
                                .name(COMPONENT_C3_NAME)
                                .type(ArchRepoSystemComponentType.BACKEND_SERVICE)
                                .build()
                ))
                .build();
    }

    private static ArchRepoSystemComponentDto createComponentA1() {
        return ArchRepoSystemComponentDto.builder()
                .name(COMPONENT_A1_NAME)
                .type(ArchRepoSystemComponentType.BACKEND_SERVICE)
                .build();
    }

    private static ArchRepoSystemComponentDto createComponentA2() {
        return ArchRepoSystemComponentDto.builder()
                .name(COMPONENT_A2_NAME)
                .type(ArchRepoSystemComponentType.SELF_CONTAINED_SYSTEM)
                .build();
    }

    private static ArchRepoSystemComponentDto createComponentA3() {
        return ArchRepoSystemComponentDto.builder()
                .name(COMPONENT_A3_NAME)
                .type(ArchRepoSystemComponentType.BACKEND_SERVICE)
                .build();
    }

    private static ArchRepoSystemComponentDto createComponentB1() {
        return ArchRepoSystemComponentDto.builder()
                .name(COMPONENT_B1_NAME)
                .type(ArchRepoSystemComponentType.BACKEND_SERVICE)
                .build();
    }

    private static ArchRepoSystemComponentDto createComponentB2() {
        return ArchRepoSystemComponentDto.builder()
                .name(COMPONENT_B2_NAME)
                .type(ArchRepoSystemComponentType.BACKEND_SERVICE)
                .build();
    }

    private static ArchRepoSystemComponentDto createComponentB3() {
        return ArchRepoSystemComponentDto.builder()
                .name(COMPONENT_B3_NAME)
                .type(ArchRepoSystemComponentType.BACKEND_SERVICE)
                .build();
    }
}
