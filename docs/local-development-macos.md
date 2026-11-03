# macOS 本地开发

本机环境配置日期：2026-11-03，Apple Silicon（arm64）。

## 当前项目状态

已补齐 Spring Boot 和 Vue/Vite 入口。前端首页会请求后端健康接口，显示连接状态并支持重新检查；已提供基础八字排盘表单与四柱接口，账户、数据库、大运和流年仍未实现。

| 目录 | 职责与当前内容 |
| --- | --- |
| `backend/fate-common` | 公共类型，尚为空模块 |
| `backend/fate-calendar` | Tyme4j 四柱历法适配 |
| `backend/fate-bazi` | 独立四柱计算、策略元数据与核心测试 |
| `backend/fate-server` | Boot 入口、健康/排盘接口、安全配置及集成测试 |
| `frontend` | Vue 入口、四柱表单与结果、连接检查、Vite 代理及构建脚本 |
| `docs`、`deploy` | 设计及本地启动文档，尚无生产部署配置 |

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

打开 http://127.0.0.1:5173，预期显示“后端连接正常”。通过 Ctrl+C 分别停止服务。

后端默认端口 8080，前端固定 5173，端口占用时直接报错。前端通过 Vite 将 `/api` 转发到后端，无需 CORS。修改后端 `SERVER_PORT` 时需同步修改 `frontend/vite.config.ts` 的代理目标。后端 Java 文件修改后需重新打包启动；前端开发服务器支持热更新。

可直接访问 http://127.0.0.1:8080/api/v1/health 或 `/actuator/health`。其他路径默认禁止访问。`npm run build` 进行类型检查和生产构建；`npm run preview` 仅预览静态资源，不提供 API 代理。

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
```

`persistence` 仅启用预留的 MyBatis、MySQL 驱动和 Flyway 依赖，不会创建数据库。当前不需要安装 MySQL、Redis 或 Docker。

2026-11-03 已验证默认构建、健康接口和安全边界集成测试、前端类型检查及生产构建。前端 CI 已增加生产构建检查。

原锁文件的 `follow-redirects` 高危漏洞已通过更新至 1.16.2 修复，`npm audit` 为 0 漏洞。另将 TypeScript 从 7.0.2 调整为 6.0.3，解决 vue-tsc 访问编译器 API 时的构建错误。

当前不需要安装数据库。`local` 环境通过 `application-local.yml` 排除数据源自动配置，因此即便启用可选 `persistence` 依赖也不会尝试创建数据源。后续接入本地数据库时需删除该排除项并提供连接配置。

首页可选择公历或农历日期和时间进行基础排盘，农历可选闰月，结果同时展示对应公历和农历。计算约定及接口示例见 [API 文档](api/README.md)。

PC/手机选择器已分别接入 Element Plus/Vant，选择器适配测试运行 `cd frontend && npm test`，交互与选型见 [选择器说明](date-time-picker.md)。

排盘结果已扩展十神、藏干、五行和纳音；PC 为对照表，手机为逐柱卡片。详细字段及五行计数口径见 [API 文档](api/README.md)。

未登录模式会自动缓存最多 3 份完整排盘到当前浏览器，刷新可恢复并切换；本机记录说明见 [本机记录](local-chart-library.md)。开发时清除 localStorage 对应键可重置本机样例，后端不存储这些记录。

新排盘需选择性别，结果已加入起运时间、10 步大运及每步对应流年，可点选切换；旧本机记录需补选性别并重算以升级运程快照。

选中流年后会按需加载节令流月，选中流月再加载流日。首次加载明细需本地后端运行；明细只在本页内存有限缓存，不增加三份本机记录的体积。

选择流日后可查看十二时辰，并点选干支、十神、五行和纳音详情；标准时段与节令裁切后的实际覆盖分开显示。流时明细需要后端按需计算，仍不写入本机快照。
