package ch.admin.bit.jeap.governance.reactionobserver;

import ch.admin.bit.jeap.governance.domain.ComponentType;
import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import lombok.experimental.UtilityClass;

import java.time.ZonedDateTime;
import java.util.ArrayList;

@UtilityClass
public class TestUtility {

    public static System createSystem(String systemName) {
        return System.builder()
                .name(systemName)
                .systemComponents(new ArrayList<>())
                .createdAt(ZonedDateTime.now())
                .build();
    }

    public static SystemComponent createSystemComponent(System system, String componentName) {
        SystemComponent systemComponent = SystemComponent.builder()
                .name(componentName)
                .type(ComponentType.BACKEND_SERVICE)
                .createdAt(ZonedDateTime.now())
                .build();
        system.addSystemComponent(systemComponent);
        return systemComponent;
    }
}
