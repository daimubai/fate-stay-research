# FateStay Research

[English](README.en.md) | 简体中文

基于 Java 的命理研究平台，以八字为起点，后续扩展紫微斗数、传统术数、古籍知识库和 AI 辅助研究。

## 设计原则

- **模块化架构**：采用 Monorepo 与 Maven 多模块，明确领域边界。
- **独立计算核心**：历法与八字模块独立于 Web 框架，便于测试、复用和扩展。
- **可复现研究**：以计算策略、算法版本和规则版本追踪推演依据。
- **一致的工程规范**：固定依赖版本，统一构建工具，并通过 CI 与依赖更新流程维护工程质量。

## 技术与版本

| 技术 | 锁定版本 / 策略 |
| --- | --- |
| Java | JDK 26；Java 27 暂不作为正式基线 |
| Maven | Wrapper 3.3.4 / Maven 3.10.0，发行包 SHA-256 校验 |
| Spring Boot | 4.1.1；Spring Framework、Security、JUnit 等由 Boot BOM 管理 |
| MyBatis Starter | 4.1.0，默认关闭的 persistence 配置 |
| Springdoc OpenAPI | 3.1.1 |
| Tyme4j | 1.5.2 |
| MySQL Connector/J、Flyway | 由 Boot BOM 管理，默认关闭的 persistence 配置 |
| Node.js / npm | Node 22.22.0 / npm 10.9.4 |
| Vue / TypeScript | 3.5.44 / 6.0.3 |
| Vite / Vue 插件 | 8.3.5 / 6.0.10 |
| Element Plus / Pinia | 2.14.7 / 4.1.0 |
| Vue Router / Axios / vue-tsc | 5.4.0 / 1.20.0 / 3.3.13 |

依赖版本由 Maven BOM 与 npm 锁文件统一管理，使用 Renovate 提交依赖更新供审核。

## 目录与依赖方向

```text
fate-stay-research/
├── pom.xml                       # Maven 聚合与版本管理
├── mvnw / mvnw.cmd / .mvn/       # 官方 Maven Wrapper
├── backend/
│   ├── fate-common/              # 无框架依赖的公共类型
│   ├── fate-calendar/            # 历法与节气，依赖 common、Tyme4j
│   ├── fate-bazi/                # 八字核心，依赖 common、calendar
│   └── fate-server/              # 服务端编排，依赖 bazi
├── frontend/                     # Vue 前端
├── deploy/                       # 部署配置与文档
├── docs/{architecture,api,database}/
├── .github/workflows/ci.yml
└── README.md / README.en.md
```

项目坐标为 `dev.fatestay:fate-stay-research:0.1.0-SNAPSHOT`；Java 包前缀 `dev.fatestay.research`；API 命名空间 `/api/v1`。模块边界及扩展约束见 [架构说明](docs/architecture/README.md)。

## 安装与验证

安装 JDK 26（正确设置 `JAVA_HOME`）、Node 22.22.0 和 npm 10.9.4。首次运行需要访问 Maven Central 与 npm 官方仓库。Wrapper 会下载 Maven，无需全局安装 Maven。

```powershell
# Windows，在仓库根目录
.\mvnw.cmd --batch-mode --no-transfer-progress verify
# 可选：验证预留持久化依赖，不创建或连接数据库
.\mvnw.cmd --batch-mode --no-transfer-progress -Ppersistence verify
cd frontend
npm ci --ignore-scripts
npm run deps:check
```

```sh
# macOS / Linux，在仓库根目录
chmod +x mvnw
./mvnw --batch-mode --no-transfer-progress verify
./mvnw --batch-mode --no-transfer-progress -Ppersistence verify
cd frontend
npm ci --ignore-scripts
npm run deps:check
```

`verify` 用于验证 Maven 工程，`deps:check` 用于检查前端依赖关系。持久化依赖通过 `persistence` 配置按需启用。

## 本地启动

```sh
# 终端 1：仓库根目录，需 JDK 26
./mvnw --batch-mode --no-transfer-progress package
java -jar backend/fate-server/target/fate-server-0.1.0-SNAPSHOT.jar

# 终端 2：仓库根目录
cd frontend
npm ci --ignore-scripts
npm run dev
```

访问 http://127.0.0.1:5173，页面通过 Vite 将 `/api/v1/health` 代理到后端 8080，显示连接状态，点击“重新检查”可重试。当前仅放行应用健康接口与 `/actuator/health`，其他路径仍禁止访问，无需数据库。

`npm run build` 执行类型检查和生产构建。`npm run preview` 仅预览静态资源，不提供开发 API 代理；生产部署需将 `/api` 转发到后端。若通过 `SERVER_PORT` 修改后端端口，需同步修改 `frontend/vite.config.ts` 的代理目标。

原有 TypeScript 7 与 vue-tsc 不兼容，已固定为 TypeScript 6.0.3；锁文件中的漏洞间接依赖 `follow-redirects` 已更新至 1.16.2。

本机 MacData 环境路径见 [macOS 开发说明](docs/local-development-macos.md)。历法、八字计算、账户和持久化仍未实现。

## 路线图

1. 已完成 Spring Boot、Vue/Vite 入口、健康接口及本地联调。
2. 实现计算配置、历法及八字接口、独立核心测试。
3. 确定数据库版本和数据模型后，再启用持久化、迁移及认证。
4. 接入页面、API、OpenAPI 和部署配置。
5. 后续再扩展紫微斗数、古籍检索及 AI。

## 相关文档

- [架构设计](docs/architecture/README.md)
- [API 设计](docs/api/README.md)
- [数据库规划](docs/database/README.md)
- [部署说明](deploy/README.md)

## 技术参考

[Spring Boot](https://spring.io/projects/spring-boot) · [MyBatis](https://github.com/mybatis/spring-boot-starter) · [Springdoc](https://springdoc.org/) · [Tyme4j](https://github.com/6tail/tyme4j) · [Vue](https://vuejs.org/) · [Vite](https://vite.dev/)

## 许可证

本项目采用 [Apache License 2.0](LICENSE) 许可证。第三方依赖遵循各自的许可证。
