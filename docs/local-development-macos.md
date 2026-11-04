# macOS 本地开发

本机环境配置日期：2026-11-03，Apple Silicon（arm64）。

## 当前项目状态

截至 2026-11-04，已提供公农历 / 闰月输入、四柱详解、十二长生与七项神煞、合冲刑破害、胎元 / 命宫 / 身宫、起运与大运、流年 / 月 / 日 / 时，以及三份未登录本机快照。PC 为左右布局，手机保持四柱横向对照，附加信息默认折叠；时间明细在打开对应选择器时才请求。

后端为本机 Spring Boot 无状态计算服务，前端为 Vue / Vite。账号、数据库与云端同步尚未实现；不需要安装 MySQL、Redis 或 Docker。能力列表和边界见 [项目 README](../README.md)，模块设计见 [系统架构](architecture/README.md)。

## 本地启动

终端 1：

```sh
source /Volumes/MacData/development/env.sh
cd /Volumes/MacData/projects/java/FateStayResearch
./mvnw --batch-mode --no-transfer-progress package
java -jar backend/fate-server/target/fate-server-0.1.0-SNAPSHOT.jar
```

终端 2：

```sh
source /Volumes/MacData/development/env.sh
cd /Volumes/MacData/projects/java/FateStayResearch/frontend
npm ci --ignore-scripts
npm run dev
```

打开 [本地工作台](http://127.0.0.1:5173/)，PC 侧栏的服务状态应显示“本地服务已连接”。手机可直接开始排盘。通过 Ctrl+C 分别停止服务。

后端默认端口 8080，前端固定 5173，端口占用时直接报错。前端通过 Vite 将 `/api` 转发到后端，无需 CORS。修改后端 `SERVER_PORT` 时需同步修改 `frontend/vite.config.ts` 的代理目标。后端 Java 文件修改后需重新打包启动；前端开发服务器支持热更新。

可直接访问 [应用健康接口](http://127.0.0.1:8080/api/v1/health) 或 [Actuator 健康接口](http://127.0.0.1:8080/actuator/health)。四条 `POST /api/v1/bazi/{charts,flow-months,flow-days,flow-hours}` 计算接口也已放行，其他路径默认禁止访问。`npm run build` 进行类型检查和生产构建；`npm run preview` 仅预览静态资源，不提供 API 代理。

## 已安装环境

工具根目录：`/Volumes/MacData/development`。

- OpenJDK 26.0.2.1：`tools/jdk-26.0.2.1.jdk/Contents/Home`
- Node.js 22.22.0 / npm 10.9.4：`tools/node-v22.22.0-darwin-arm64`
- Maven 3.10.0：由项目 `./mvnw` 下载，保存在 `cache/maven/wrapper/dists`
- Maven 依赖仓库：`cache/maven/repository`
- npm 缓存：`cache/npm`
- 官方下载包与校验信息：`downloads`
- 环境脚本：`env.sh`

JDK 和 Node 下载包均已核对官方 SHA-256；Maven 发行包由 Wrapper 按仓库配置核对 SHA-256。无需单独安装全局 Maven。

`~/.zshrc` 已添加环境脚本加载语句，修改前内容备份为 `/Volumes/MacData/development/zshrc.before-fatestay`。新终端自动生效；已有终端或非交互 shell 可执行：

```sh
source /Volumes/MacData/development/env.sh
java -version
node -v
npm -v
```

## 验证命令

```sh
cd /Volumes/MacData/projects/java/FateStayResearch
source /Volumes/MacData/development/env.sh
./mvnw --version
./mvnw --batch-mode --no-transfer-progress verify
./mvnw --batch-mode --no-transfer-progress -Ppersistence verify
cd frontend
npm ci --ignore-scripts
npm run deps:check
npm test
npm run build
```

`persistence` 仅启用预留的 MyBatis、MySQL 驱动和 Flyway 依赖，不会创建数据库。当前不需要安装 MySQL、Redis 或 Docker。

截至 2026-11-04，最近一次功能验证为后端 152 项核心 + 54 项 HTTP / 安全测试、前端 25 项测试和生产构建通过；桌面及 390px / 320px 临时浏览器检查也已完成。前端 CI 执行依赖检查、测试及生产构建，后端执行默认与可选持久化构建。详细证据和未覆盖平台见 [验证记录](verification.md)。

依赖处理历史（2026-11-03）：将 `follow-redirects` 更新至 1.16.2 修复原锁文件漏洞，将 TypeScript 固定为 6.0.3 解决与 vue-tsc 的构建兼容问题。该记录不代替以后依赖更新时的安全检查。

## 开发时的行为与数据

- 后端默认 `local` 配置排除数据源自动配置。未来接数据库时，需删除该排除项并提供连接及迁移配置；当前只有可选依赖，无数据库模型。
- 后端代码变更需先停止旧服务，再重新打包启动；前端通过 Vite 热更新。运行中的 JAR 不应被重新打包覆盖。
- PC 使用 Element Plus，手机使用 Vant，支持公农历切换与闰月。实际排盘结果始终由本地后端计算，前端 Tyme4ts 只用于日期输入适配。
- 已有出生信息、记录列表和附加详情可收起。打开命盘不会请求流月 / 日 / 时，打开相应选择器时再加载。精确节令、日期与时辰边界在详情内查看。
- `localStorage` 键为 `fatestay.guest-charts.v1`，最多三份出生排盘快照。清除该键可重置样例；后端不会保存这些记录。换域名 / 端口会使用不同的站点存储。
- 流月 / 日 / 时只在当前页面有限缓存，刷新后首次查看仍需后端连接。已有页面中的快照切换不需重算，但尚无离线网站加载或纯前端排盘。

相关说明：[日期选择器](date-time-picker.md) · [紧凑工作台](compact-workspace.md) · [本机记录](local-chart-library.md) · [API](api/README.md)。
