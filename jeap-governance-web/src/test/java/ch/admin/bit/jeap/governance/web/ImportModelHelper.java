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

    static final String SYSTEM_A_NAME = "System A";
    static final String SYSTEM_B_NAME = "System B";
    static final String SYSTEM_C_NAME = "System C";

    static final String COMPONENT_A1_NAME = "Component A1";
    static final String COMPONENT_A2_NAME = "Component A2";
    static final String COMPONENT_A3_NAME = "Component A3";

    static final String COMPONENT_B1_NAME = "Component B1";
    static final String COMPONENT_B2_NAME = "Component B2";
    static final String COMPONENT_B3_NAME = "Component B3";

    static final String COMPONENT_C1_NAME = "Component C1";
    static final String COMPONENT_C2_NAME = "Component C2";
    static final String COMPONENT_C3_NAME = "Component C3";

    static final int DEFAULT_SYSTEM_COUNT = 3;
    static final int DEFAULT_COMPONENT_COUNT_PER_SYSTEM = 3;

    static final int SYSTEM_COUNT_LESS = DEFAULT_SYSTEM_COUNT - 1;
    static final int COMPONENT_COUNT_PER_SYSTEM_LESS = DEFAULT_SYSTEM_COUNT - 1;


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
                .aliases(Set.of("SysA", "System Alpha"))
                .systemComponents(Arrays.asList(components))
                .build();
    }

    private static ArchRepoSystemDto createSystemB(ArchRepoSystemComponentDto... components) {
        return ArchRepoSystemDto.builder()
                .name(SYSTEM_B_NAME)
                .aliases(Set.of("SysB", "System Bravo"))
                .systemComponents(Arrays.asList(components))
                .build();
    }


    private static ArchRepoSystemDto createDefaultSystemC() {
        return ArchRepoSystemDto.builder()
                .name(SYSTEM_C_NAME)
                .aliases(Set.of("SysC", "System Charlie"))
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
