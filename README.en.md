# FateStay Research

English | [简体中文](README.md)

A Java-based platform for studying traditional Chinese metaphysics, starting with Bazi (Four Pillars) and designed to expand into Zi Wei Dou Shu, classical texts and AI-assisted research.

## Design principles

- **Modular architecture**: a monorepo with Maven modules and explicit domain boundaries.
- **Independent calculation engines**: calendar and Bazi modules remain independent of the web framework for testing, reuse and extension.
- **Reproducible research**: calculation strategies, engine versions and rule versions provide traceable foundations for analysis.
- **Consistent tooling**: pinned dependencies, standardized builds, CI and reviewed dependency updates.

## Technology baseline

| Technology | Version / policy |
| --- | --- |
| Java | JDK 26; Java 27 is experimental only |
| Maven | Wrapper 3.3.4 / Maven 3.10.0 with distribution SHA-256 |
| Spring Boot | 4.1.1; Framework, Security, JUnit and related versions managed by its BOM |
| MyBatis Starter | 4.1.0, opt-in persistence profile |
| Springdoc OpenAPI | 3.1.1 |
| Tyme4j | 1.5.2 |
| MySQL Connector/J and Flyway | Boot BOM-managed, opt-in persistence profile |
| Node.js / npm | 22.22.0 / 10.9.4 |
| Vue / TypeScript | 3.5.44 / 6.0.3 |
| Vite / Vue plugin | 8.3.5 / 6.0.10 |
| Element Plus / Pinia | 2.14.7 / 4.1.0 |
| Vue Router / Axios / vue-tsc | 5.4.0 / 1.20.0 / 3.3.13 |

Dependency versions are managed through the Maven BOM and npm lockfile. Renovate proposes dependency updates for review.

## Layout and boundaries

```text
fate-stay-research/
├── pom.xml
├── mvnw / mvnw.cmd / .mvn/
├── backend/
│   ├── fate-common/              # Framework-independent shared types
│   ├── fate-calendar/            # Depends on common and Tyme4j
│   ├── fate-bazi/                # Depends on common and calendar
│   └── fate-server/              # Server orchestration; depends on bazi
├── frontend/                     # Vue frontend
├── deploy/                       # Deployment configuration and documentation
├── docs/{architecture,api,database}/
├── .github/workflows/ci.yml
└── README.md / README.en.md
```

Coordinates: `dev.fatestay:fate-stay-research:0.1.0-SNAPSHOT`. Java package prefix: `dev.fatestay.research`. API namespace: `/api/v1`. See the bilingual [architecture document](docs/architecture/README.md) for domain boundaries and reproducibility requirements.

## Install and validate

Install JDK 26, set `JAVA_HOME`, and install Node 22.22.0 with npm 10.9.4. Initial setup needs access to Maven Central and the npm registry. The wrapper downloads Maven; a global Maven installation is unnecessary.

```powershell
# Windows, from repository root
.\mvnw.cmd --batch-mode --no-transfer-progress verify
# Optional dependency validation; does not create/connect to a database
.\mvnw.cmd --batch-mode --no-transfer-progress -Ppersistence verify
cd frontend
npm ci --ignore-scripts
npm run deps:check
```

```sh
# macOS / Linux, from repository root
chmod +x mvnw
./mvnw --batch-mode --no-transfer-progress verify
./mvnw --batch-mode --no-transfer-progress -Ppersistence verify
cd frontend
npm ci --ignore-scripts
npm run deps:check
```

`verify` validates the Maven project, while `deps:check` checks frontend dependency relationships. Persistence dependencies are enabled through the optional `persistence` profile.

## Local startup

```sh
# Terminal 1, repository root (JDK 26)
./mvnw --batch-mode --no-transfer-progress package
java -jar backend/fate-server/target/fate-server-0.1.0-SNAPSHOT.jar

# Terminal 2, repository root
cd frontend
npm ci --ignore-scripts
npm run dev
```

Open http://127.0.0.1:5173. The page calls `/api/v1/health` through the Vite proxy to port 8080. “重新检查” retries the request. Health endpoints and `POST /api/v1/bazi/charts` are public; other routes remain denied. No database is required.

Run `npm run build` for type checking and a production build. `npm run preview` only previews static assets; it does not provide the development API proxy. Production hosting must route `/api` to the backend. If `SERVER_PORT` changes, update the proxy target in `frontend/vite.config.ts` too.

The existing TypeScript 7 / vue-tsc incompatibility was resolved by pinning TypeScript 6.0.3. The vulnerable transitive dependency `follow-redirects` was updated to 1.16.2 in the lockfile.

See [macOS setup](docs/local-development-macos.md) for the MacData environment paths. Four pillars, ten-god relationships, hidden stems, five-element attributes and Na Yin are available. Accounts and server-side persistence remain unimplemented.

## Basic four-pillars chart

Choose Gregorian or Chinese lunar birth date input on the homepage; lunar input distinguishes regular and leap months. Both dates are returned and displayed. Inputs support 1900–2100 with minute precision and use fixed UTC+08:00, without daylight-saving or true-solar-time corrections. Tyme4j 1.5.2 rules: Li Chun year boundary, solar-term month boundary, day changes at 23:00. Results include the original input, offset, precision and engine/rule versions. See [API documentation](docs/api/README.md). Fortune timelines are available; server-side chart persistence is not implemented.

Desktop uses Element Plus date/time controls; mobile uses Vant bottom-sheet wheels with year jump and confirm/cancel. Calendar switching preserves the selected day, and lunar month/day choices use the pinned Tyme4ts adapter. See [picker selection notes](docs/date-time-picker.md). Run `npm test` in `frontend` for adapter tests.

The result includes the day master, stem/hidden-stem ten-god relationships, main/middle/residual hidden stems, stem/branch elements and Na Yin. Desktop uses a comparison table; mobile uses one card per pillar. Element counts include only the eight visible characters and do not represent weighted strength or favorable elements.

## Fortune timeline

New requests require `gender: MALE/FEMALE`. Tyme4j default child-limit rules provide the exact starting datetime and converted interval, then ten decades with ten chronological annual Gan-Zhi entries each. Desktop and mobile allow selecting a decade and annual year. Ages use year difference plus one, not completed years; annual groups are not exact mid-year handover intervals. See [API notes](docs/api/README.md).

Annual-year selection now loads twelve Jie-based flow months; selecting a month loads Gan-Zhi days. Month boundaries use exact solar-term times; days change at 23:00 and are clipped at the month's boundaries. Results include solar/lunar labels, precise intervals, ten gods, elements and Na Yin. Details are fetched on demand with bounded page-memory reuse, cancellation and independent retry; they are not added to local snapshots and need the backend when not already loaded.

Selected flow days now expose twelve double-hours (Zi from the previous date at 23:00 through Hai ending at 23:00). Each shows Gan-Zhi, ten god relative to the birth day master, elements and Na Yin. Standard intervals are distinguished from their actual overlap with the selected Jie-clipped day; outside hours remain reference-only, with no fabricated active interval. Hour details have independent retry, stale-request cancellation and bounded page-memory reuse.

## Guest local records

Successful charts automatically save the birth input and full result in this browser, up to three slots. Equivalent solar/lunar inputs with the same gender update one slot. Different genders use separate slots; explicitly recalculating a legacy snapshot upgrades its original slot. Switch, rename, delete or explicitly replace records; refresh restores the active snapshot without recalculation. The result section also offers quick switching. Account login and cloud storage remain future work. Clearing site data removes local records; unavailable storage falls back to in-page use with a notice. See [local library notes](docs/local-chart-library.md).

## Roadmap

1. Completed Boot and Vue/Vite entry points, health endpoint and local integration.
2. Four-pillar details, start fortune, decade and annual timelines and independent tests are complete; extend calculation profiles and additional timeline features next.
3. Select a database version and design the model before enabling persistence, migrations and authentication.
4. Implement pages, APIs, OpenAPI and deployment configuration.
5. Extend into other systems, classical texts and AI when needed.

## Documentation

- [Architecture](docs/architecture/README.md)
- [API design](docs/api/README.md)
- [Database planning](docs/database/README.md)
- [Deployment](deploy/README.md)

## References

[Spring Boot](https://spring.io/projects/spring-boot) · [MyBatis](https://github.com/mybatis/spring-boot-starter) · [Springdoc](https://springdoc.org/) · [Tyme4j](https://github.com/6tail/tyme4j) · [Vue](https://vuejs.org/) · [Vite](https://vite.dev/)

## License

This project is licensed under the [Apache License 2.0](LICENSE). Third-party dependencies remain subject to their respective licenses.
