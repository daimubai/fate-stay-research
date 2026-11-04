# FateStay Research

[English](README.en.md) | 简体中文

基于 Java / Vue 的命理研究项目，当前已实现可在本机运行的八字排盘工作台。支持公农历输入、四柱详解、起运与大运，以及流年到流时的逐级查询。后续规划账号存储、更多术数、古籍知识库与 AI 辅助研究。

本文描述截至 **2026-11-04** 的实现状态。当前采用浏览器 + 本地后端运行方式，无需数据库；尚未部署为线上服务。

## 已实现功能

| 能力 | 当前实现 |
| --- | --- |
| 出生信息 | 公历 / 农历选择，普通月 / 闰月，日期、分钟精度时间及性别输入；同时输出对应公历和农历 |
| PC / 手机输入 | PC 使用 Element Plus 日期、时间控件，手机使用 Vant 底部选择面板；切换历法保留同一出生时刻 |
| 四柱详解 | 年、月、日、时四柱，日主、天干十神、藏干及藏干十神、干支五行、纳音 |
| 十二长生 | 日干对四支的十二长生，以及各柱本干对本支的自坐长生 |
| 七项神煞 | 天乙贵人、禄神、驿马、桃花、华盖、将星、旬空；附规则版本和命中依据 |
| 胎元与两宫 | Tyme4j 原生胎元、命宫、身宫，展示干支、天干十神、五行和纳音 |
| 起运与大运 | 起运公历时刻及间隔、顺逆方向，前 10 步大运，每步对应 10 个流年 |
| 流年 → 流月 → 流日 → 流时 | 逐级选择；流月按节令划分，流日按 23 点换日，流时保留十二时辰及实际覆盖区间 |
| 紧凑工作台 | PC 左侧出生信息 / 本机记录、右侧命盘 / 运程；手机保持四柱横向对照，已有信息默认收起 |
| 按需明细 | 长生神煞、胎元两宫、说明可折叠；时间选项点击再展示，月 / 日 / 时在打开选择器时才请求 |
| 未登录本机记录 | 当前浏览器最多 3 份，自动保存、切换、重命名、删除、显式替换、刷新恢复及历史快照升级 |
| 工程验证 | Spring Boot / Vue 启动入口、健康检查、Vite API 代理、核心 / HTTP / 前端测试及 CI |

四柱及其衍生计算由后端完成。前端 Tyme4ts 用于日期选择和公农历输入转换，不在浏览器重复计算八字十神或运程。

## 计算口径与边界

- 固定 Tyme4j **1.5.2**；输入按 **北京时间 UTC+08:00** 解释，不随浏览器或服务器时区改变。
- 出生时刻转换后的公历范围为 **1900-01-01 至 2100-12-31**，精确到分钟；新建完整排盘必须明确选择性别。
- **立春换年、节令换月、23 点换日**。日期显示仍按零点换日，故晚子时的日期标签与日柱换日规则有所区别。
- 起运采用 Tyme4j 默认童限规则；阳男阴女顺排、阴男阳女逆排。大运 / 流年年龄采用年序虚岁，交运当年须结合精确交运时刻区分。
- 流月是节令月，丑月可能跨入下一公历年；流日与流时会按节令边界裁切。区间含开始、不含结束，区间外时辰只作参考。流年查询范围为 1900–2210 年。
- 五行数量仅统计四干四支共 8 字，不含藏干权重或胎元两宫，不据此直接判断旺衰、喜用神。
- 神煞为基于 Tyme4j 四柱的**项目七项规则层**，与 Tyme4j 黄历吉凶神列表分开；十二长生和胎元两宫直接使用原生接口。
- 尚不支持出生地点、夏令时 / 真太阳时校正、多流派切换、起运前小运、账号登录或云端保存。

详细规则见 [十二长生与神煞](docs/natal-traits.md)、[胎元与两宫](docs/natal-palaces.md) 和 [API 文档](docs/api/README.md)。

## 页面与本机保存

PC 将输入和记录放在侧栏，主区域集中展示命盘及当前运程。手机在已有命盘时默认收起输入与记录列表，通过“修改出生信息”“切换 / 管理”打开。命盘始终保留四柱横向对照，附加说明按需展开。

大运、流年、流月、流日、流时统一通过时间选择入口切换：PC 打开居中弹窗，手机打开底部面板。初次打开命盘不请求流月、流日或流时；选择父级也不会自动加载下级。父级改变时重置下级，取消过期请求，明细可独立重试。月 / 日 / 时各保留最多 24 份页面内存缓存。

成功排盘自动保存到当前来源的 `localStorage`，最多 3 份。同一公历出生分钟、同性别更新原位置，等价公农历输入不会重复占位；不同性别分别占位。满额时明确选择替换或暂不保存，不自动淘汰记录。旧快照可继续查看，重新排盘后补齐新字段并保留原名称与位置。

已保存的命盘、起运、大运和流年可在已加载页面中切换，无需重算；流月 / 日 / 时只保留页面内存缓存，首次加载仍需后端。清除网站数据、换浏览器或换域名 / 端口不会共享记录。当前没有离线应用加载、前端独立排盘或云端同步。

交互细节见 [紧凑工作台设计](docs/compact-workspace.md)、[本机记录设计](docs/local-chart-library.md)。

## 技术栈

| 技术 | 仓库锁定版本 / 用途 |
| --- | --- |
| Java / Maven | JDK 26；Wrapper 3.3.4 / Maven 3.10.0，发行包 SHA-256 校验 |
| Spring Boot | 4.1.1，Framework / Security / JUnit 等由 Boot BOM 管理 |
| Tyme4j / Tyme4ts | 1.5.2 / 1.5.2，后端计算与前端日期适配版本对齐 |
| Node.js / npm | 22.22.0 / 10.9.4 |
| Vue / TypeScript | 3.5.44 / 6.0.3 |
| Vite / Vue 插件 / vue-tsc | 8.3.5 / 6.0.10 / 3.3.13 |
| Element Plus / Vant / Axios | 2.14.7 / 4.10.2 / 1.20.0 |
| Pinia / Vue Router | 已声明 4.1.0 / 5.4.0；当前页面主要使用 Vue 响应式状态与组件组合 |
| Springdoc | 已声明 3.1.1；OpenAPI / Swagger 页面尚未公开放行 |
| 可选持久化 | MyBatis Starter 4.1.0，MySQL 驱动 / Flyway 由 Boot BOM 管理；通过 `persistence` 启用依赖，尚无数据库实现 |

依赖由 Maven BOM 与 npm 锁文件管理，Renovate 提交更新供审核。版本表描述仓库配置，不表示外部软件的最新版本。

## 项目结构

```text
FateStayResearch/
├── pom.xml / mvnw / mvnw.cmd / .mvn/
├── backend/
│   ├── fate-common/       # 预留无框架公共模块，当前尚无业务类型
│   ├── fate-calendar/     # Tyme4j 适配：历法、四柱属性、起运及运程
│   ├── fate-bazi/         # 范围校验、排盘编排、五行计数、项目神煞规则
│   └── fate-server/       # Spring Boot、HTTP DTO、校验、安全与健康检查
├── frontend/
│   ├── src/components/   # 出生选择器、命盘、本机记录、宫位及时间选择面板
│   ├── src/lib/          # 日期适配、本机快照、按需运程状态
│   └── tests/            # 日期 / 缓存测试及样例
├── docs/                 # 架构、API、规则、交互与验证记录
├── deploy/               # 本地 JAR / 前端构建说明，暂无生产部署配置
└── .github/              # CI 与依赖更新配置
```

后端依赖方向为 `fate-server → fate-bazi → fate-calendar → fate-common`，历法模块另依赖 Tyme4j；计算核心不依赖 Spring、Web 或数据库。项目坐标 `dev.fatestay:fate-stay-research:0.1.0-SNAPSHOT`，包前缀 `dev.fatestay.research`。

## 本地启动

准备 JDK 26、Node 22.22.0 和 npm 10.9.4，无需全局 Maven、MySQL、Redis 或 Docker。首次安装依赖需连接 Maven Central / npm 仓库。

本机 MacData 工具已配置时，在两个终端分别执行：

```sh
source /Volumes/MacData/development/env.sh
```

终端 1，在仓库根目录启动后端：

```sh
./mvnw --batch-mode --no-transfer-progress package
java -jar backend/fate-server/target/fate-server-0.1.0-SNAPSHOT.jar
```

终端 2，在仓库根目录启动前端：

```sh
cd frontend
npm ci --ignore-scripts
npm run dev
```

打开 [本地页面](http://127.0.0.1:5173/)。Vite 将 `/api` 代理至后端 8080；侧栏可展开查看服务连接状态。健康接口为 [应用健康检查](http://127.0.0.1:8080/api/v1/health) 和 [Actuator 健康检查](http://127.0.0.1:8080/actuator/health)。后端默认 `local` 环境排除数据源自动配置，不连接数据库。

前端文件支持热更新。后端代码变更需停止旧服务后重新打包启动，避免运行中的 JAR 被覆盖。若修改 `SERVER_PORT`，需同步修改 `frontend/vite.config.ts`。Windows 使用 `mvnw.cmd` 替代 `./mvnw`；其他机器自行设置 JDK / Node 环境，不使用本机 MacData 路径。

`npm run preview` 仅预览静态构建，不提供 API 代理；生产托管需另外配置 `/api` 反向代理。完整环境说明见 [macOS 开发文档](docs/local-development-macos.md)。

## API 概览

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | `/api/v1/health` | 应用连通性 |
| GET | `/actuator/health` | Spring Boot 存活检查 |
| POST | `/api/v1/bazi/charts` | 完整出生排盘、公农历转换及起运运程 |
| POST | `/api/v1/bazi/flow-months` | 所选流年的节令流月 |
| POST | `/api/v1/bazi/flow-days` | 所选节令流月的流日 |
| POST | `/api/v1/bazi/flow-hours` | 所选流日的十二时辰 |

上述计算接口无需登录、无状态，不保存出生信息；四条计算路径单独豁免 CSRF，其余路径默认拒绝访问。详细 DTO、响应和错误见 [API 设计](docs/api/README.md)。

## 测试与验证

```sh
# 仓库根目录；Windows 改用 mvnw.cmd
./mvnw --batch-mode --no-transfer-progress verify
./mvnw --batch-mode --no-transfer-progress -Ppersistence verify

cd frontend
npm ci --ignore-scripts
npm run deps:check
npm test
npm run build
```

`persistence` 只验证可选依赖，在当前 `local` 配置下不创建数据库。最近一次功能验证为后端 **132 项核心 + 52 项 HTTP / 安全测试**、前端 **23 项测试**及生产构建通过；前端另用 Playwright / 本机 Chrome 检查了桌面与 390px / 320px 页面、按需请求、失败重试、旧响应取消、缓存升级和输入流程。浏览器检查脚本为临时人工验证，不属于仓库 CI；Safari / Android 真机尚未验证。详见 [验证记录](docs/verification.md)。

CI 包含默认 / 可选持久化 Maven 构建，以及前端依赖检查、测试和生产构建。

## 设计文档

| 文档 | 内容 |
| --- | --- |
| [系统架构与设计](docs/architecture/README.md) | 模块边界、计算链路、数据结构、安全边界和扩展方向 |
| [紧凑工作台](docs/compact-workspace.md) | PC / 手机布局、折叠策略、时间选择与按需加载 |
| [日期时间选择器](docs/date-time-picker.md) | Element Plus / Vant 选型、公农历适配 |
| [十二长生与神煞](docs/natal-traits.md) | 原生 API、七项规则查表、命中依据及版本 |
| [胎元、命宫与身宫](docs/natal-palaces.md) | 原生接口、响应样例和计算口径 |
| [本机记录](docs/local-chart-library.md) | 三份快照、身份、兼容升级及存储边界 |
| [API](docs/api/README.md) | 请求 / 响应、计算范围及错误 |
| [macOS 开发](docs/local-development-macos.md) | MacData 环境、本地启动和验证命令 |
| [数据库规划](docs/database/README.md) / [部署说明](deploy/README.md) | 当前预留项与后续接入边界 |
| [验证记录](docs/verification.md) | 分阶段测试范围、结果及尚未覆盖的平台 |

## 后续计划

1. 设计账号、服务端排盘持久化与本机记录迁移，明确数据库版本后再接入迁移和认证。
2. 扩展可版本化计算策略、输入时区 / 地点及历法校正，并补充对应边界测试。
3. 完善可公开的 OpenAPI、生产反向代理、部署配置和跨浏览器验证。
4. 在八字能力稳定后扩展紫微斗数、古籍检索与 AI 辅助研究。

## 许可证

本项目采用 [Apache License 2.0](LICENSE)。第三方依赖遵循各自许可证。
