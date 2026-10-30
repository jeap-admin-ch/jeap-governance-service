Checks that the component name follows the convention `{system-name}-{context}-{type-id}`, so that the owning system and the kind of component can be recognised from the name alone.

## Why

Consistent names make components easy to find and attribute across deployments, monitoring, logs and the architecture repository. Many tools and dashboards rely on the system name being the first part of the component name.

## How the rule is evaluated

The component name is split at every `-`. The rule fails if the name has fewer than three parts. Otherwise it checks:

1. **System name** (first part): must start with a lowercase letter followed by lowercase letters, digits or `_`, and must equal the name of the owning system or one of its aliases (case-insensitive).
2. **Context** (all parts between the first and the last, joined by `-`): must start with a lowercase letter followed by lowercase letters, digits or `-`.
3. **Type id** (last part): must be one of `service`, `ui`, `scs`, `mobileapp`, `gateway` or `db`.

All violations found are listed in the rule comment.

## How to comply

- Use only lowercase letters, digits and hyphens in the name (underscores are allowed in the system name part).
- Start the name with your system name or one of its aliases as registered in the architecture repository.
- End the name with one of the supported type ids, e.g. `mysystem-orders-service` or `mysystem-orders-ui`.

## Exemptions

The rule has no parameters. If a component cannot be renamed, it can be exempted from the rule (permanently or until a given date) by the governance service instance's configuration.
