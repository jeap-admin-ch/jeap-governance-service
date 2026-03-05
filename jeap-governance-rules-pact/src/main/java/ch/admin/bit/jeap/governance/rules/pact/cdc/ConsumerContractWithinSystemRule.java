package ch.admin.bit.jeap.governance.rules.pact.cdc;

import ch.admin.bit.jeap.governance.archrepo.domain.RestApiRelationWithoutPact;
import ch.admin.bit.jeap.governance.archrepo.domain.RestApiRelationWithoutPactRepository;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleMetadata;
import ch.admin.bit.jeap.governance.domain.rule.RuleId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean({RestApiRelationWithoutPactRepository.class})
public class ConsumerContractWithinSystemRule extends AbstractConsumerContractRule {

    public ConsumerContractWithinSystemRule(RestApiRelationWithoutPactRepository restApiRelationWithoutPactRepository) {
        super(restApiRelationWithoutPactRepository);
    }

    @Override
    public RuleMetadata metadata() {
        return RuleMetadata.builder()
                .ruleId(RuleId.of("component-cdc-contractwithinsystem"))
                .label("Consumer Contract Within System")
                .build();
    }

    @Override
    protected boolean filterServices(RestApiRelationWithoutPact relation) {
        return relation.getConsumerSystemComponent().getSystem().equals(relation.getProviderSystemComponent().getSystem());
    }
}
