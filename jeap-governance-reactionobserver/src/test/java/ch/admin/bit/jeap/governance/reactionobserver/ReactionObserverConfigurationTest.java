package ch.admin.bit.jeap.governance.reactionobserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Reaction Observer's URL and client registration are only required while the Reaction Observer is enabled; the
 * check itself is covered by {@link ReactionObserverPropertiesTest}.
 */
class ReactionObserverConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ReactionObserverConfiguration.class));

    @Test
    void disabled_startsWithoutUrlAndClientRegistration() {
        contextRunner
                .withPropertyValues("jeap.governance.reactionobserver.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(ReactionObserverProperties.class);
                });
    }
}
