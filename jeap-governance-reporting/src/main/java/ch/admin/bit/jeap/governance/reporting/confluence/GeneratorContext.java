package ch.admin.bit.jeap.governance.reporting.confluence;

import lombok.Value;

import java.util.HashSet;
import java.util.Set;

@Value
class GeneratorContext {
    String rootPageId;
    Set<String> generatedPageIds = new HashSet<>();

    void addGeneratedPageIds(String... pageIds) {
        generatedPageIds.addAll(Set.of(pageIds));
    }
}
