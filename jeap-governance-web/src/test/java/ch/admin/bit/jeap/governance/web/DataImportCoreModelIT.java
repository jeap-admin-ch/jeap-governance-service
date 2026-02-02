package ch.admin.bit.jeap.governance.web;

import ch.admin.bit.jeap.governance.dataimport.DataImportScheduler;
import ch.admin.bit.jeap.governance.domain.System;
import ch.admin.bit.jeap.governance.domain.SystemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DataImportCoreModelIT extends ArchRepoMockIntegrationTestBase {

    @Autowired
    private SystemRepository systemRepository;

    @Autowired
    private DataImportScheduler dataImportScheduler;

    @Test
    void synchronizeArchRepoModel_shouldImportSystemsAndComponents_initial() throws Exception {
        setUpImportDefaultModel();

        dataImportScheduler.update();

        List<System> all = systemRepository.findAll();
        assertEquals(ImportModelHelper.DEFAULT_SYSTEM_COUNT, all.size());
        for (System system : all) {
            assertEquals(ImportModelHelper.DEFAULT_COMPONENT_COUNT_PER_SYSTEM, system.getSystemComponents().size());
        }
    }

    @Test
    void synchronizeArchRepoModel_shouldImportSystemsAndComponents_deletedSystemsAndComponents() throws Exception {
        setUpImportDefaultModel();
        dataImportScheduler.update();

        assertEquals(ImportModelHelper.DEFAULT_SYSTEM_COUNT, systemRepository.findAll().size());

        setUpImportModelLess();

        dataImportScheduler.update();

        List<System> all = systemRepository.findAll();
        assertEquals(ImportModelHelper.SYSTEM_COUNT_LESS, all.size());
        for (System system : all) {
            assertEquals(ImportModelHelper.COMPONENT_COUNT_PER_SYSTEM_LESS, system.getSystemComponents().size());
        }
    }

    @Test
    void synchronizeArchRepoModel_shouldImportSystemsAndComponents_addedSystemsAndComponents() throws Exception {
        setUpImportModelLess();
        dataImportScheduler.update();

        assertEquals(ImportModelHelper.SYSTEM_COUNT_LESS, systemRepository.findAll().size());

        setUpImportDefaultModel();

        dataImportScheduler.update();

        List<System> all = systemRepository.findAll();
        assertEquals(ImportModelHelper.DEFAULT_SYSTEM_COUNT, all.size());
        for (System system : all) {
            assertEquals(ImportModelHelper.DEFAULT_COMPONENT_COUNT_PER_SYSTEM, system.getSystemComponents().size());
        }
    }
}
