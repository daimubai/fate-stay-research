# Scaffold verification / 骨架验证

Date / 日期: 2026-10-21

- Windows, Node 22.22.0, local JDK 19.
- Maven Wrapper downloads Maven 3.10.0; distribution SHA-256 is recorded and checked. Its downloaded archive was also compared with the official SHA-512.
- `mvnw.cmd -Ppersistence verify`: passed for all four modules. Resolves the optional MyBatis, MySQL driver and Flyway dependencies without a database connection.
- `npm run deps:check`: passed with no invalid top-level dependency relationships.
- No source code or tests exist. No application startup, API, database, UI or JDK 26 compilation has been tested. CI is configured to use JDK 26; its first GitHub run is pending.
- Maven 3.10 emitted optional remote prefix-metadata HTTP 401 warnings for repositories declared transitively by Flyway; the build still succeeded and no credentials are required for this scaffold.

本次仅验证目录、工程配置和依赖。构建成功不代表业务功能可用；本机 JDK 19，未验证真实源码在 JDK 26 下的编译。GitHub CI 已配置 JDK 26，首次远端执行仍待提交后确认。
