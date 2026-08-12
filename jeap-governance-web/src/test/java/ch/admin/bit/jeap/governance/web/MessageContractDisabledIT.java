package ch.admin.bit.jeap.governance.web;

import ch.admin.bit.jeap.governance.messagecontract.MessageContractProperties;
import ch.admin.bit.jeap.governance.messagecontract.dataimport.MessageContractVersionImporter;
import ch.admin.bit.jeap.governance.messagecontract.rule.ComponentUsesLatestMessageVersionsRule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "jeap.governance.environment=DEV",
        "jeap.governance.archrepo.url=http://localhost:8081"
})
class MessageContractDisabledIT extends PostgresTestContainerBase {

    @Autowired
    private ApplicationContext context;

    @Test
    void messageContractBeansDoNotExistWhenDisabled() {
        assertThat(context.getBeansOfType(MessageContractVersionImporter.class)).isEmpty();
        assertThat(context.getBeansOfType(ComponentUsesLatestMessageVersionsRule.class)).isEmpty();
        assertThat(context.getBeansOfType(MessageContractProperties.class)).isEmpty();
    }
}
