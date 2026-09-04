# Erlang review — issue #4322 Pipelines validation/problems

**Change class:** public REST adaptor surface (Admin GET validation/problems)

**Companions checked:** wire DTOs (ApplicationValidationProblem/Result), IPipelinesAdaptor method, PipelinesResource GET, Mockito resource tests, Spring TestPipelinesAdaptor stub, sitemanage PipelinesAdaptor + CollectingApplicationValidator, adaptor/unit tests, product-docs/8.2/developer/rest.md.

**Findings:** none blocking.
- Path-safe resolve (trusted catalog name only); 404/400 bodies do not echo raw idOrName.
- Admin gate + hidden rejection mirror start/stop.
- Collecting validator peers PSValidatorAdapter without inventing a new engine; throws disabled so problems accumulate.
- designGaps does not claim validation-read unsupported; graph edit / IR write remains listed.
- Cross-platform: no filesystem path construction; component path uses logical / breadcrumbs.
- C2: interface method added — stub + sitemanage reverse-dep clean install green.

**Builds:** cd rest && ../mvnw clean install BUILD SUCCESS (Tests run: 1121, Failures: 0); cd projects/sitemanage && ../../mvnw clean install BUILD SUCCESS (Tests run: 2304, Failures: 0). PipelinesResourceTest 25/0; PipelinesAdaptorTest 31/0; CollectingApplicationValidatorTest 2/0.
