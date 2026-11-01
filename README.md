# Bedding Plants

[![Semgrep](https://github.com/ChrisSamo632/bedding-plants/actions/workflows/semgrep.yml/badge.svg)](https://github.com/ChrisSamo632/bedding-plants/actions/workflows/semgrep.yml)
[![CodeQL](https://github.com/ChrisSamo632/bedding-plants/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/ChrisSamo632/bedding-plants/actions/workflows/codeql-analysis.yml)
[![pmd](https://github.com/ChrisSamo632/bedding-plants/actions/workflows/pmd.yml/badge.svg)](https://github.com/ChrisSamo632/bedding-plants/actions/workflows/pmd.yml)
[![Qodana Code Quality](https://github.com/ChrisSamo632/bedding-plants/actions/workflows/qodana_code_quality.yml/badge.svg)](https://github.com/ChrisSamo632/bedding-plants/actions/workflows/qodana_code_quality.yml)

Spring Boot RESTful application for import/export of Bedding Plant spreadsheet data (250th Manchester Scout Group).

## Run the Local Application

The local launcher prompts without echoing for `GOOGLE_MAPS_API_KEY` and
`BEDDING_PLANTS_DB_PASSWORD` when they are not already defined:

```bash
bash ./run-local.sh
```

The Google Maps key must have the Geocoding, Static Maps, and Distance Matrix APIs
enabled. Distance Matrix requests may incur Google Maps Platform charges. The
database password is for the existing encrypted development H2 database. For
non-interactive use, define both environment variables before starting the
application; do not put their values in tracked files.

Application configuration is in `application.yaml`, with development and
release overrides in `application-dev.yaml` and `application-release.yaml`.
Properties under `beddingplants` are application-specific (and therefore are
not part of Spring Boot's built-in property catalog); their old dotted keys map
to the YAML paths as follows:

| Former property key | YAML path | Purpose |
| --- | --- | --- |
| `beddingplants.import.orderImportsName` | `beddingplants.import.order-imports-name` | Workbook order-sheet name |
| `beddingplants.import.plantImportsName` | `beddingplants.import.plant-imports-name` | Workbook plant-sheet name |
| `beddingplants.import.defaultCity` | `beddingplants.import.default-city` | Default imported-address city |
| `beddingplants.export.scoutHutLat` / `scoutHutLng` | `beddingplants.export.scout-hut-lat` / `scout-hut-lng` | Map centre coordinates |
| `beddingplants.export.defaultZoom` | `beddingplants.export.default-zoom` | Default map zoom |
| `beddingplants.export.viewportMaxLat` / `viewportMinLat` | `beddingplants.export.viewport-max-lat` / `viewport-min-lat` | Map viewport latitude limits |
| `beddingplants.export.viewportMaxLng` / `viewportMinLng` | `beddingplants.export.viewport-max-lng` / `viewport-min-lng` | Map viewport longitude limits |
| `beddingplants.deliveries.maxRoutePlantCount` | `beddingplants.deliveries.max-route-plant-count` | Generated-route plant capacity |
| `beddingplants.deliveries.maxRouteRadiusKm` | `beddingplants.deliveries.max-route-radius-km` | Maximum pairwise road distance |
| `beddingplants.geolocation.googleApiKey` | `beddingplants.geolocation.google-api-key` | Supplied from `GOOGLE_MAPS_API_KEY` |
| `beddingplants.geolocation.googleMapsImgScale/Width/Height/Language` | `beddingplants.geolocation.google-maps-img-scale/width/height/language` | Static-map image options |

Types and IDE descriptions for these custom properties are maintained in
`src/main/resources/META-INF/additional-spring-configuration-metadata.json`.
Spring's relaxed binding keeps the Java camel-case configuration fields
compatible with the kebab-case YAML keys. Standard Spring and Springdoc
properties retain their dotted names, represented as nested YAML sections.

To run tests, use `./mvnw test`. The server binds to loopback by default. The H2
console and extra actuator visibility are enabled only by the development
profile.

## Pre-commit Hooks

Install [pre-commit](https://pre-commit.com/) and enable the hooks once per clone:

```shell
pip install pre-commit
pre-commit install
```

Each commit runs file hygiene checks, secret scanning (gitleaks), shellcheck,
GitHub Actions linting (actionlint), Java formatting (Spotless) and, when `pom.xml`
or `src/` change, `./mvnw verify`, which builds, tests, lints (PMD) and runs SAST
(SpotBugs with FindSecBugs) on the API.
Run everything manually with `pre-commit run --all-files`; skip the slow build in an
emergency with `SKIP=maven-build-and-test git commit`.

## Code Quality

`./mvnw verify` (also run by the **Build** GitHub workflow on every push and pull
request) fails on any finding from:

| Concern    | Tool                          | Notes                                                              |
|------------|-------------------------------|--------------------------------------------------------------------|
| Formatting | Spotless                      | Unused imports, trailing whitespace, final newline; fix with `./mvnw spotless:apply` |
| Linting    | PMD (`quickstart` ruleset)    | Same ruleset as the PMD workflow                                   |
| SAST       | SpotBugs + FindSecBugs        | Medium threshold; documented suppressions in `spotbugs-exclude.xml` |

CodeQL, Semgrep, Qodana and dependency review continue to run as separate workflows.

VS Code uses the same rules: `.editorconfig` and `.vscode/settings.json` apply the
Spotless whitespace and import rules on save, `.vscode/extensions.json` recommends the
extensions, and `.vscode/tasks.json` has tasks that run the exact Maven goals above
(format, PMD, SpotBugs, verify, site).

## Site Reports

`./mvnw verify site` generates the project site in `target/site/index.html` (the Build
workflow uploads it as the `site` artifact). It includes project information and
dependencies, test results (Surefire), JaCoCo coverage, cross-referenced source, Javadoc,
PMD and CPD (copy/paste) lint, SpotBugs/FindSecBugs findings, and dependency/plugin
update reports. Run `verify` first so the coverage data exists.

## Swagger UI

Access Swagger UI via <http://localhost:8443/bedding-plants/>

## Generate and Print Delivery Routes

Use `GET /bedding-plants/deliveries/routes/{saleYear}` to generate or update
routes. Existing assignments are preserved. Orders are grouped only on the same
delivery day; every pair of stops must be within the configured road-distance
radius, and route plant totals must not exceed
`beddingplants.deliveries.maxRoutePlantCount`. Route numbers continue after the
highest route number already saved for that sale. Orders without usable
coordinates or above the plant limit remain unassigned for manual review.

Opening `GET /bedding-plants/export/routes/{saleYear}/pdf` generates routes
before producing the printout. The HTML route view at
`GET /bedding-plants/export/routes/{saleYear}/html` uses the same flow and
includes any orders that still need manual assignment. Printed routes sort by
delivery day and route number by default; the `sorts` parameter accepts
comma-separated `day`/`num` fields with `ASC` or `DESC` directions.
