# FateStay Research

English | [简体中文](README.md)

A Java / Vue research project for traditional Chinese metaphysics. The current local Bazi workspace supports solar/lunar birth input, enriched Four Pillars, fortune start and decade cycles, and annual-to-double-hour navigation. Accounts, server-side storage, other systems, classical texts and AI-assisted research are future work.

This document describes the implementation as of **2026-11-04**. It runs with a browser and local Java backend, requires no database, and has not been deployed as a hosted service.

## Implemented features

| Area | Current behavior |
| --- | --- |
| Birth input | Gregorian or Chinese lunar date, regular/leap month, minute-precision time and explicit gender; both calendar dates returned |
| Responsive pickers | Element Plus desktop date/time controls and Vant mobile sheets; calendar switching preserves the same birth instant |
| Four Pillars | Year/month/day/hour, day master, stem and hidden-stem ten gods, hidden stems, stem/branch elements and Na Yin |
| Twelve life stages | Day stem against each branch, plus each pillar's own stem against its branch |
| Seven Shen Sha rules | Tian Yi, Lu Shen, Yi Ma, Tao Hua, Hua Gai, Jiang Xing and Xun Kong, with rule versions and matching evidence |
| Auxiliary palaces | Tyme4j-native fetal origin, own sign and body sign, including elements, stem ten god and Na Yin |
| Fortune timeline | Exact starting time and converted interval, direction, ten decades and ten annual years per decade |
| Monthly/daily/hourly flow | Jie-based months, days changing at 23:00, twelve double-hours with nominal and actual clipped intervals |
| Compact workspace | Desktop input/record sidebar beside the chart; mobile retains a four-column comparison with optional information collapsed |
| On-demand navigation | Time options appear only when opened; month/day/hour requests are deferred until their picker opens |
| Guest local library | Up to three browser snapshots; autosave, switch, rename, delete, explicit replacement, reload recovery and legacy upgrades |
| Engineering | Boot/Vue entry points, health checks, Vite API proxy, core/HTTP/frontend tests and CI |

The backend computes Bazi relationships and fortune timelines. Frontend Tyme4ts supports date input and calendar conversion only.

## Calculation conventions and limits

- Pinned **Tyme4j 1.5.2**, fixed **UTC+08:00** clock time, independent of browser/server timezone.
- Resolved Gregorian birth range: **1900-01-01 through 2100-12-31**, minute precision. New complete chart requests require `MALE` or `FEMALE`.
- Year changes at **Li Chun**, month at **Jie solar terms**, and Gan-Zhi day at **23:00**. Displayed calendar dates still change at midnight.
- Fortune start uses Tyme4j's default ChildLimit convention. Ages use year difference plus one; annual grouping does not replace the exact handover time.
- Flow years support 1900–2210. Chou month may extend into the next civil year. Intervals include their start and exclude their end; month boundaries clip days/hours. Outside hours remain references without fabricated active intervals.
- Element counts include only the eight visible natal characters, not hidden-stem weights or auxiliary palaces; they are not strength or favorable-element scores.
- Shen Sha is an explicitly versioned seven-rule project layer over Tyme-derived pillars, separate from almanac God lists. Life stages and auxiliary palaces use native Tyme4j APIs.
- Location, daylight-saving/true-solar-time corrections, selectable schools, pre-start minor fortunes, accounts and cloud storage remain unimplemented.

See [life stages and Shen Sha](docs/natal-traits.md), [auxiliary palaces](docs/natal-palaces.md) and [API conventions](docs/api/README.md).

## Interface and storage

Desktop places birth input and records in a sidebar, with the chart and compact fortune controls in the main column. On mobile, existing birth information and the record list are collapsed; both devices retain a horizontal four-pillar table. Additional attributes and explanations expand on demand.

Five controls select decade, year, month, day and double-hour. Desktop opens a centered dialog; mobile opens a bottom sheet. Opening a chart makes no month/day/hour requests. Changing a parent resets incompatible descendants and aborts obsolete requests. Each tier retries independently and caches up to 24 responses in page memory.

Successful calculations save up to three snapshots to same-origin `localStorage`. The resolved solar minute plus gender identifies a record; equivalent solar/lunar input updates the same slot and preserves its name. A full library requires explicit replacement or leaving the result unsaved. Legacy snapshots remain readable and are upgraded in place when recalculated.

Saved natal details, fortune start, decades and annual years can be restored without recalculation in an already loaded page. Month/day/hour data is not persisted and needs the backend when uncached. Clearing site data, switching browsers or changing origin does not share records. There is no offline application loading or browser-only Bazi engine.

See [workspace design](docs/compact-workspace.md) and [local library design](docs/local-chart-library.md).

## Technology baseline

| Technology | Repository version / role |
| --- | --- |
| Java / Maven | JDK 26; Wrapper 3.3.4 / Maven 3.10.0 with SHA-256 validation |
| Spring Boot | 4.1.1; Framework, Security and JUnit managed by its BOM |
| Tyme4j / Tyme4ts | 1.5.2 / 1.5.2 |
| Node.js / npm | 22.22.0 / 10.9.4 |
| Vue / TypeScript | 3.5.44 / 6.0.3 |
| Vite / Vue plugin / vue-tsc | 8.3.5 / 6.0.10 / 3.3.13 |
| Element Plus / Vant / Axios | 2.14.7 / 4.10.2 / 1.20.0 |
| Pinia / Vue Router | Declared as 4.1.0 / 5.4.0; the current single page mainly uses Vue reactive state and component composition |
| Springdoc | 3.1.1 declared; OpenAPI/Swagger routes are not publicly enabled |
| Optional persistence | MyBatis Starter 4.1.0; MySQL driver/Flyway managed by Boot BOM, enabled only by the `persistence` Maven profile |

Versions describe repository configuration, not the latest external releases. Maven BOM and the npm lockfile manage dependencies; Renovate proposes reviewed updates.

## Repository layout

```text
FateStayResearch/
├── pom.xml / mvnw / mvnw.cmd / .mvn/
├── backend/
│   ├── fate-common/       # Reserved framework-independent common module
│   ├── fate-calendar/     # Tyme4j calendar, natal attributes and timeline adapter
│   ├── fate-bazi/         # Validation, chart orchestration, counts and Shen Sha rules
│   └── fate-server/       # Boot, HTTP DTOs, security and health checks
├── frontend/
│   ├── src/components/   # Input, chart, records, palaces and time dialogs
│   ├── src/lib/          # Calendar adapter, snapshots and lazy flow state
│   └── tests/            # Calendar/cache tests and fixtures
├── docs/                 # Architecture, API, rules, interaction and validation
├── deploy/               # Local packaging notes; no production deployment yet
└── .github/              # CI and dependency update configuration
```

Dependencies flow from server to Bazi to calendar/common; the calculation core does not depend on Spring, Web or a database. Coordinates: `dev.fatestay:fate-stay-research:0.1.0-SNAPSHOT`. Package prefix: `dev.fatestay.research`.

## Local startup

Prepare JDK 26, Node 22.22.0 and npm 10.9.4. No global Maven, database, Redis or Docker is required. Initial dependency installation needs Maven Central/npm access.

On this configured Mac, load the environment in both terminals:

```sh
source /Volumes/MacData/development/env.sh
```

Terminal 1, repository root:

```sh
./mvnw --batch-mode --no-transfer-progress package
java -jar backend/fate-server/target/fate-server-0.1.0-SNAPSHOT.jar
```

Terminal 2, repository root:

```sh
cd frontend
npm ci --ignore-scripts
npm run dev
```

Open [the local workspace](http://127.0.0.1:5173/). Vite proxies `/api` to port 8080; service status is expandable in the desktop sidebar. The default `local` Spring profile excludes datasource auto-configuration, including with optional persistence dependencies present.

Frontend changes hot-reload. Stop the backend before repackaging/restarting it. If `SERVER_PORT` changes, update `frontend/vite.config.ts` too. Windows uses `mvnw.cmd`; other machines configure their own JDK/Node environment instead of using this Mac-specific path.

`npm run preview` serves only static build output. Production hosting must separately proxy `/api` to the backend. See [macOS setup](docs/local-development-macos.md).

## API overview

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/api/v1/health` | Application health |
| GET | `/actuator/health` | Boot health |
| POST | `/api/v1/bazi/charts` | Complete birth chart, calendars, attributes and fortune timeline |
| POST | `/api/v1/bazi/flow-months` | Jie-based months for a selected year |
| POST | `/api/v1/bazi/flow-days` | Days for a selected flow month |
| POST | `/api/v1/bazi/flow-hours` | Twelve double-hours for a selected flow day |

Calculation endpoints are public and stateless, do not persist birth data, and are individually exempted from CSRF. Other routes are denied by default. See [API DTOs and errors](docs/api/README.md).

## Validation

```sh
# Repository root; use mvnw.cmd on Windows
./mvnw --batch-mode --no-transfer-progress verify
./mvnw --batch-mode --no-transfer-progress -Ppersistence verify

cd frontend
npm ci --ignore-scripts
npm run deps:check
npm test
npm run build
```

The optional persistence profile validates dependencies without creating a database under the current local configuration. Latest feature validation passed **132 core + 52 HTTP/security tests**, **23 frontend tests**, and the production build. Temporary Playwright/local Chrome checks covered desktop and 390px/320px layouts, lazy requests, retry/cancellation, legacy upgrades and real birth input. Browser scripts are not part of repository CI; real Safari/Android devices remain untested. See [validation history](docs/verification.md).

CI runs default/optional-persistence Maven builds and frontend dependency checks, tests and build.

## Design documentation

- [System architecture](docs/architecture/README.md): boundaries, calculation flow, models, security and future design.
- [Compact workspace](docs/compact-workspace.md): responsive layout, disclosures, time selection and lazy loading.
- [Date/time pickers](docs/date-time-picker.md): desktop/mobile calendar input.
- [Life stages and Shen Sha](docs/natal-traits.md): fixed rules, evidence and versioning.
- [Auxiliary palaces](docs/natal-palaces.md): native APIs and response examples.
- [Guest local library](docs/local-chart-library.md): identity, capacity, compatibility and storage boundaries.
- [API](docs/api/README.md), [macOS setup](docs/local-development-macos.md), [database planning](docs/database/README.md), [deployment](deploy/README.md) and [validation](docs/verification.md).

## Next steps

1. Design account authentication, database-backed charts and explicit local-snapshot migration.
2. Extend versioned calculation strategies, timezone/location input and calendar corrections with boundary tests.
3. Add production configuration, reverse proxy, public OpenAPI documentation and cross-browser verification.
4. Expand into Zi Wei Dou Shu, classical-text retrieval and AI-assisted research after stabilizing the Bazi core.

## License

[Apache License 2.0](LICENSE). Third-party dependencies retain their respective licenses.
