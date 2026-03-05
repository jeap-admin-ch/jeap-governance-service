package ch.admin.bit.jeap.governance.rules.pact.cdc;

import ch.admin.bit.jeap.governance.archrepo.domain.RestApiRelationWithoutPact;
import ch.admin.bit.jeap.governance.archrepo.domain.RestApiRelationWithoutPactRepository;
import ch.admin.bit.jeap.governance.domain.SystemComponent;
import ch.admin.bit.jeap.governance.domain.plugin.rule.Rule;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleParameters;
import ch.admin.bit.jeap.governance.domain.plugin.rule.RuleResult;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.List;

@Slf4j
public abstract class AbstractConsumerContractRule implements Rule {

    private static final String SERVICES_TO_IGNORE = "services-to-ignore";
    private static final String RELATIONS_TO_IGNORE = "relations-to-ignore";
    private final RestApiRelationWithoutPactRepository restApiRelationWithoutPactRepository;

    protected AbstractConsumerContractRule(RestApiRelationWithoutPactRepository restApiRelationWithoutPactRepository) {
        this.restApiRelationWithoutPactRepository = restApiRelationWithoutPactRepository;
    }

    @Override
    public RuleResult evaluate(SystemComponent systemComponent, RuleParameters ruleParameters) {

        List<String> servicesToIgnore = ruleParameters.getParameterAsList(SERVICES_TO_IGNORE);
        List<String> relationsToIgnore = ruleParameters.getParameterAsList(RELATIONS_TO_IGNORE);

        List<RuleResult> results = new LinkedList<>();
        List<RestApiRelationWithoutPact> restApiRelationsWithoutPact = retrieveRestRelationsWithoutPact(systemComponent, servicesToIgnore, relationsToIgnore);

        for (RestApiRelationWithoutPact relation : restApiRelationsWithoutPact) {
            results.add(RuleResult.failed(formatFailMessage(relation)));
        }

        if (results.isEmpty()) {
            return RuleResult.ok("No rest relation without pact found");
        }
        return RuleResult.summarize(results);
    }

    protected abstract boolean filterServices(RestApiRelationWithoutPact relation);

    private List<RestApiRelationWithoutPact> retrieveRestRelationsWithoutPact(SystemComponent systemComponent, List<String> servicesToIgnore, List<String> relationsToIgnore) {
        return restApiRelationWithoutPactRepository.findAllByConsumerSystemComponentId(systemComponent.getId()).stream()
                .filter(this::filterServices)
                .filter(relation -> filterIgnoredServices(relation, servicesToIgnore))
                .filter(relation -> filterIgnoredRelations(relation, relationsToIgnore))
                .toList();
    }

    private boolean filterIgnoredServices(RestApiRelationWithoutPact relation, List<String> servicesToIgnore) {
        for (String service : servicesToIgnore) {
            if (relation.getConsumerSystemComponent().getName().contains(service) || relation.getProviderSystemComponent().getName().contains(service)) {
                return false;
            }
        }
        return true;
    }

    private boolean filterIgnoredRelations(RestApiRelationWithoutPact relation, List<String> relationsToIgnore) {
        String relationString = relation.getMethod() + " " + relation.getPath();
        return !relationsToIgnore.contains(relationString);
    }

    private static String formatFailMessage(RestApiRelationWithoutPact relation) {
        return "No consumer contract found for relation with %s on '%s %s'".formatted(
                relation.getProviderSystemComponent().getName(), relation.getMethod(), relation.getPath());
    }

}
