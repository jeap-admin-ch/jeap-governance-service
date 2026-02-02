package ch.admin.bit.jeap.governance.web;

import ch.admin.bit.jeap.governance.archrepo.dataimport.ApiDocVersionImporter;
import ch.admin.bit.jeap.governance.archrepo.dataimport.DatabaseSchemaVersionImporter;
import ch.admin.bit.jeap.governance.archrepo.dataimport.ReactionGraphImporter;
import ch.admin.bit.jeap.governance.archrepo.dataimport.RestApiRelationWithoutPactImporter;
import ch.admin.bit.jeap.governance.archrepo.deletion.ApiDocVersionComponentDeletionListener;
import ch.admin.bit.jeap.governance.archrepo.deletion.DatabaseSchemaVersionComponentDeletionListener;
import ch.admin.bit.jeap.governance.archrepo.deletion.ReactionGraphComponentDeletionListener;
import ch.admin.bit.jeap.governance.archrepo.deletion.RestApiRelationWithoutPactComponentDeletionListener;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "jeap.governance.archrepo.url=http://localhost:8080",
        "jeap.governance.archrepo.import.apidocversion.enabled=false"
})
class ApiDocVersionDisabledIT {

    @Autowired
    private ApplicationContext context;

    @Test
    void apiDocVersionBeans_shouldNotExist_whenDisabled() {
        assertThat(context.getBeansOfType(ApiDocVersionImporter.class)).isEmpty();
        assertThat(context.getBeansOfType(ApiDocVersionComponentDeletionListener.class)).isEmpty();

        assertThat(context.getBeansOfType(DatabaseSchemaVersionImporter.class)).hasSize(1);
        assertThat(context.getBeansOfType(DatabaseSchemaVersionComponentDeletionListener.class)).hasSize(1);
        assertThat(context.getBeansOfType(ReactionGraphImporter.class)).hasSize(1);
        assertThat(context.getBeansOfType(ReactionGraphComponentDeletionListener.class)).hasSize(1);
        assertThat(context.getBeansOfType(RestApiRelationWithoutPactImporter.class)).hasSize(1);
        assertThat(context.getBeansOfType(RestApiRelationWithoutPactComponentDeletionListener.class)).hasSize(1);
    }
}
