package ch.admin.bit.jeap.governance.domain.rule;

import java.util.Objects;

public record RuleId(String id) {

    public static RuleId of(String id) {
        return new RuleId(Objects.requireNonNull(id));
    }

    @Override
    public String toString() {
        return id;
    }
}
