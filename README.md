# jeap-governance-service

Service template to provide a quick overview of system and service compliance with defined policies. A service instance 
can be created by depending on this template,then adding specific configuration and extending it with plugin 
implementations.

## Key Features
      
- **Developer Feedback:** Teams receive clear information on whether their services comply with 
        organizational policies
- **Flexible Rule Management:** Administrators can temporarily deactivate individual rules for 
     specific systems or services when exceptions are needed
- **Extensible Rules Engine:** New compliance rules can be easily added through a plugin mechanism, 
     allowing teams to provide their own custom rules

## Installing / Getting started

Normally you will not use this project directly, but instead set up your own governance service depending on this
common library. See [Architecture](docs/architecture.md) for the module layout and [Configuration](docs/configuration.md)
for how to configure an instance.

## Documentation

| Topic                                              | File                                                 |
|-----------------------------------------------------|-------------------------------------------------------|
| Architecture, module overview, domain model, database schema | [docs/architecture.md](docs/architecture.md)         |
| Configuration reference (`jeap.governance.*`)      | [docs/configuration.md](docs/configuration.md)       |
| Built-in and custom rules                          | [docs/rules.md](docs/rules.md)                       |
| Plugin mechanism (data import, data deletion)       | [docs/plugin-mechanism.md](docs/plugin-mechanism.md) |
| Reporting to Confluence                            | [docs/reporting.md](docs/reporting.md)               |
| Metrics and recommended alerts                     | [docs/metrics.md](docs/metrics.md)                   |

> Internal, BIT-only documentation (e.g. GovDashboard rule explanations for the BAZG Governance Dashboard) remains
> on Confluence and is not part of this public repository.

## Changes
This library needs to be versioned using [Semantic Versioning](http://semver.org/) and all changes need to be documented at [CHANGELOG.md](./CHANGELOG.md) following the format defined in [Keep a Changelog](http://keepachangelog.com/)

## Note

This repository is part of the open source distribution of jEAP. See [github.com/jeap-admin-ch/jeap](https://github.com/jeap-admin-ch/jeap)
for more information.

## License

This repository is Open Source Software licensed under the [Apache License 2.0](./LICENSE).
