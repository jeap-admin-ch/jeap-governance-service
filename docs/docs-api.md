# Docs API

The docs API is a read-only API for the [jEAP Doc Service](https://github.com/jeap-admin-ch/jeap-doc-service). It
delivers everything the governance reports show - scores, trends, histories, rule states and conformance - and every
rule the governance service knows, with its explanation, weight, share and exemptions. The Doc Service imports it and
renders the reports as a documentation site of its own. The scoring and the trends stay in the governance service;
the Doc Service only renders them.

## Availability and access

The docs API is always available under `/docs-api`, without a setting to switch it on. It is secured like
`/api/management`, by the jEAP resource server, but with a role of its own on every resource.

Every resource requires a bearer token carrying the semantic role `governance-documentation` / `read`, which resolves
to `<system-name>_@governance-documentation_#read` with the system name configured in
`jeap.security.oauth2.resourceserver.system-name`. A request without a token or with an invalid one is answered
`401`, a token without the role `403`, as the jEAP resource server answers them.

## Resources

| Method and resource              | Content                                                                                                                                                               |
|----------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `GET /docs-api/systems`          | The time of the last completed scoring run, the trend period, and every scored system with score, trend and `etag`                                                    |
| `GET /docs-api/systems/{system}` | One system: score, trend, daily history, and its components, each with score, trend, history and rule states                                                          |
| `GET /docs-api/rules`            | The time of the last completed scoring run, and every known rule with rule ID, name, `active`, weight, share, conformance rate, trend, number of exemptions and `etag` |
| `GET /docs-api/rules/{ruleId}`   | One rule: explanation, weight, share, violation delay, parameters, component exemptions, conformance rate, rate per system, non-compliant and grace-period components |

- `{system}` is the system name as the governance service stores it, `{ruleId}` the rule ID. An unknown system or rule
  is answered `404` with an RFC 9457 problem document (`application/problem+json`) naming it in `detail`.
- Every index entry carries a `url`, the path of its resource with the service's context path and without host.
- The payloads are described in the OpenAPI group `Governance-Service-Docs-API` of the Swagger UI.

### Values

- Timestamps are ISO 8601 with offset, days ISO dates. Scores and rates are whole percent, `null` where there is no
  value yet. Histories hold one value per scored day of the trend period, oldest first. The period covers
  `jeap.governance.report-data.trend-period-days` days up to and including the day of the last completed scoring run.
- A trend is `UP`, `DOWN` or `STABLE`, as the governance service calculates it over the trend period
  (`jeap.governance.report-data.trend-period-days`); `UNKNOWN` with a single value, `NO_DATA` without any.
- A rule state is `OK`, `FAIL`, `PAUSED`, `DISABLED` or `UNKNOWN`. Its comment is plain text, not Markdown: `comment` is
  its first line, the summary, and `details` its further lines, one item each (see [Rules](rules.md#rule-comments)).
- **Known rules** are the rule beans of the running service, active or not. An inactive rule carries its name and
  explanation only, `active: false`, and no weight, share, rate, trend or exemptions.
- **The share** of an active rule is its weight divided by the sum of the weights of all active rules, in percent,
  rounded to one decimal, computed from the weights the scoring uses. A rule ID configured as active without a rule
  bean is left out, as the scoring leaves it out. The share is nominal: on a component for which other rules are
  disabled, the rule weighs more.
- **An exemption** carries its ID, component, reason, `until` day, its own parameters and its `effect`: `PAUSED` (before
  the `until` day), `DISABLED` (no `until`) or `EXPIRED` (from the `until` day on: the rule applies again on that
  day). An exemption naming several rules is listed under each of them. The `exemptionCount` of the rule index counts
  only the exemptions in force, not the expired ones.
- **The explanation** is the rule's Markdown resource (see [Rules](rules.md#rule-explanations)), delivered unchanged,
  or `null` for a rule that ships none. It is not sanitized: render it with raw HTML shown as text.
- **The parameters** are the rule's configured `parameters`, as name and value.
- **The scoring time** `scoredAt` is the end of the last completed scoring run. It is part of both indexes only, so the
  indexes change with every scoring run while a system or rule resource changes only with its content.

## Conditional requests

Every resource carries a strong `ETag` of the form `"sha256:<hex>"` over its body and `Cache-Control: no-cache`. A
request whose `If-None-Match` matches the current `ETag` is answered `304 Not Modified` with an empty body.

The `etag` of an index entry is byte-identical to the `ETag` header of the resource it names. A consumer that holds a
resource with that tag does not need to request it again.

## Webhook after scoring

The docs API is passive. To have the Doc Service import it right after every scoring run, an instance switches on the
webhook call with `jeap.governance.doc-service.webhook.enabled`:

- After every completed scoring run, once the run is recorded, the governance service calls
  `POST <jeap.governance.doc-service.webhook.url>` with the body `{"eventId": "<uuid>", "scoredAt": "<timestamp>"}`
  and a bearer token of the OAuth2 client registration `jeap.governance.doc-service.webhook.client-registration`. The
  Doc Service requires the role `governance-notifications` / `write` for it.
- The call is made on a thread of its own, outside the scoring transaction and lock. A failure is logged as a warning
  and never fails the scoring; there is no retry, the Doc Service's scheduled import catches up.
- The docs API reads from the read replica. Right after the call, a replica that lags behind can still deliver the
  previous run. A consumer compares the `scoredAt` of the index with the one of the call, and if the index is older,
  imports again a little later instead of concluding that nothing has changed.
- With the webhook on, the URL and the client registration are required: the service refuses to start without them
  and names the missing setting. With it off (the default), no call is made, and the docs API is still available.

See [Configuration](configuration.md#doc-service) for the properties and the client registration.

## See also

- [Configuration](configuration.md) — the webhook and report data properties.
- [Rules](rules.md) — rule explanations and the rule comment format.
