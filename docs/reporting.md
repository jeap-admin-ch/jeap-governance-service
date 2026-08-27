# Reporting

The reporting module periodically generates and publishes Confluence pages that provide an overview of the
governance status across all systems and components. It presents scores, trends, and rule conformance rates,
giving teams and administrators a central place to monitor compliance with defined governance policies.

The module generates the following Confluence pages:

```
Root Page
├── System Scores                                     ← Overview of all systems with score and trend
│   └── <System Name> (System scores)                 ← Score, trend, score history chart and component list for a single system
│       └── <Component Name> (Component scores)       ← Score, trend, score history chart and rule compliance breakdown for a single component
└── Rules                                             ← Overview of all rules with conformance rate and trend
    └── <Rule Name>                                   ← Rule metadata, conformance rate per system, grace periods and non-compliant components
```

For rules with a configured `violation-delay`, each rule page lists components currently within the grace period,
including the rule comment, when the violation was detected and when the grace period ends. Multiline comments show
their details as a list, making individual violations visible from the rule overview. These components remain
compliant for scoring until the grace period expires. Timestamps use the runtime's local timezone.

To find out the ancestor of a page, you can use the Confluence REST API, the following example retrieves the
ancestors of a page with the title "BAZG-Governance" in the space "ARCDOCDEV":

```
confluence.yourcompany/rest/api/content?spaceKey=ARCDOCDEV&title=BAZG-Governance&expand=ancestors
```

See [Configuration](configuration.md) for the `jeap.governance.reporting.*` properties, including the Confluence
connection settings.

## See also

- [Configuration](configuration.md) — reporting configuration properties.
- [Metrics](metrics.md) — reporting job metrics and recommended alerts.
