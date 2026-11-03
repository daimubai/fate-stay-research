# Scaffold verification / 骨架验证

Date / 日期: 2026-10-21

- Windows, Node 22.22.0, local JDK 19.
- Maven Wrapper downloads Maven 3.10.0; distribution SHA-256 is recorded and checked. Its downloaded archive was also compared with the official SHA-512.
- `mvnw.cmd -Ppersistence verify`: passed for all four modules. Resolves the optional MyBatis, MySQL driver and Flyway dependencies without a database connection.
- `npm run deps:check`: passed with no invalid top-level dependency relationships.
- No source code or tests exist. No application startup, API, database, UI or JDK 26 compilation has been tested. CI is configured to use JDK 26; its first GitHub run is pending.
- Maven 3.10 emitted optional remote prefix-metadata HTTP 401 warnings for repositories declared transitively by Flyway; the build still succeeded and no credentials are required for this scaffold.

本次仅验证目录、工程配置和依赖。构建成功不代表业务功能可用；本机 JDK 19，未验证真实源码在 JDK 26 下的编译。GitHub CI 已配置 JDK 26，首次远端执行仍待提交后确认。

## 2026-11-03 macOS entry-point verification

- Apple Silicon; OpenJDK 26.0.2.1, Node 22.22.0, npm 10.9.4, Maven Wrapper 3.10.0.
- Added a Boot executable entry point, application health endpoint, explicit security allowlist and Vue/Vite connection-check page.
- Three integration tests cover public application health, public Actuator health and denial of other routes.
- Frontend clean lockfile install, typecheck/build and dependency check pass. TypeScript is pinned to 6.0.3 for vue-tsc compatibility.
- `follow-redirects` is locked to 1.16.2; `npm audit` reports zero vulnerabilities at verification time.
- Browser plugin runtime/skill unavailable; Playwright with installed Chrome checked desktop 1280×900 and mobile 390×844. Verified page identity, meaningful content, no Vite overlay, clean console on normal load/retry, successful real API proxy request, injected 503 failure and recovery, and no mobile horizontal overflow.
- Local profile excludes datasource auto-configuration until database implementation exists. No database, authentication or calculation functionality was added.
