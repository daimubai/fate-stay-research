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

Open http://127.0.0.1:5173. The page calls `/api/v1/health` through the Vite proxy to port 8080. “重新检查” retries the request. Only the application and Actuator health endpoints are public; other routes remain denied. No database is required.

Run `npm run build` for type checking and a production build. `npm run preview` only previews static assets; it does not provide the development API proxy. Production hosting must route `/api` to the backend. If `SERVER_PORT` changes, update the proxy target in `frontend/vite.config.ts` too.

The existing TypeScript 7 / vue-tsc incompatibility was resolved by pinning TypeScript 6.0.3. The vulnerable transitive dependency `follow-redirects` was updated to 1.16.2 in the lockfile.

See [macOS setup](docs/local-development-macos.md) for the MacData environment paths. Calendar and Bazi calculations, accounts and persistence remain unimplemented.

## Roadmap

1. Completed Boot and Vue/Vite entry points, health endpoint and local integration.
2. Implement calculation profiles, calendar/Bazi contracts and independent engine tests.
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
