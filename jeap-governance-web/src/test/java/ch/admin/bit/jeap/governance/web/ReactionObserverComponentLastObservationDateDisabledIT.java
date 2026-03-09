package ch.admin.bit.jeap.governance.web;

import ch.admin.bit.jeap.governance.reactionobserver.ReactionObserverProperties;
import ch.admin.bit.jeap.governance.reactionobserver.dataimport.ReactionObserverComponentLastObservationDateImporter;
import ch.admin.bit.jeap.governance.reactionobserver.deletion.ReactionObserverComponentLastObservationDateDeletionListener;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "jeap.governance.environment=DEV",
        "jeap.governance.reactionobserver.enabled=false",
        "jeap.governance.archrepo.url=http://localhost:8081"
})
class ReactionObserverComponentLastObservationDateDisabledIT extends PostgresTestContainerBase {

    @Autowired
    private ApplicationContext context;

    @Test
    void reactionObserverBeans_shouldNotExist_whenDisabled() {
        assertThat(context.getBeansOfType(ReactionObserverComponentLastObservationDateImporter.class)).isEmpty();
        assertThat(context.getBeansOfType(ReactionObserverComponentLastObservationDateDeletionListener.class)).isEmpty();
        assertThat(context.getBeansOfType(ReactionObserverProperties.class)).isEmpty();
    }
}
