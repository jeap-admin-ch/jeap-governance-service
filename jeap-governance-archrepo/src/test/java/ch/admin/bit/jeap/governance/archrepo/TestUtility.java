package ch.admin.bit.jeap.governance.archrepo;

import ch.admin.bit.jeap.governance.archrepo.synchronize.ComponentTechnicalIdentifier;
import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import lombok.experimental.UtilityClass;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

@UtilityClass
public class TestUtility {

    private static final AtomicLong SYSTEM_ID_GENERATOR = new AtomicLong(1);
    private static final AtomicLong COMPONENT_ID_GENERATOR = new AtomicLong(1);

    public static final String SYSTEM_NAME_A = "SystemA";
    public static final String COMPONENT_NAME_A1 = "ComponentA1";
    public static final String COMPONENT_NAME_A2 = "ComponentA2";
    public static final String COMPONENT_NAME_A_NON_EXISTING = "ComponentANonExisting";
    public static final String SYSTEM_NAME_B = "SystemB";
    public static final String COMPONENT_NAME_B1 = "ComponentB1";
    public static final String COMPONENT_NAME_B2 = "ComponentB2";

    public static final ComponentTechnicalIdentifier PROVIDER_KEY_COMPONENT_A1 = new ComponentTechnicalIdentifier(SYSTEM_NAME_A, COMPONENT_NAME_A1);

    public static final System SYSTEM_A = createSystem(SYSTEM_NAME_A);
    public static final System SYSTEM_B = createSystem(SYSTEM_NAME_B);
    public static final SystemComponent SYSTEM_COMPONENT_A1 = createSystemComponent(SYSTEM_A, COMPONENT_NAME_A1);
    public static final SystemComponent SYSTEM_COMPONENT_A2 = createSystemComponent(SYSTEM_A, COMPONENT_NAME_A2);
    public static final SystemComponent SYSTEM_COMPONENT_B1 = createSystemComponent(SYSTEM_B, COMPONENT_NAME_B1);
    public static final SystemComponent SYSTEM_COMPONENT_B2 = createSystemComponent(SYSTEM_B, COMPONENT_NAME_B2);

    public static System createSystem(String systemName) {
        System system = System.builder()
                .name(systemName)
                .systemComponents(new ArrayList<>())
                .createdAt(ZonedDateTime.now())
                .build();
        ReflectionTestUtils.setField(system, "id", SYSTEM_ID_GENERATOR.getAndIncrement());
        return system;
    }

    public static SystemComponent createSystemComponent(System system, String componentName) {
        SystemComponent systemComponent = SystemComponent.builder()
                .name(componentName)
                .type(ComponentType.BACKEND_SERVICE)
                .createdAt(ZonedDateTime.now())
                .build();
        ReflectionTestUtils.setField(systemComponent, "id", COMPONENT_ID_GENERATOR.getAndIncrement());
        system.addSystemComponent(systemComponent);
        return systemComponent;
    }
}
