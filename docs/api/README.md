# API

更新日期：2026-11-04。当前接口为本地后端无状态计算服务，完整模块和结果结构见 [系统设计](../architecture/README.md)。

`GET /api/v1/health`：公开的服务连通性接口，无需登录。

```json
{"status":"UP","application":"fate-stay-research"}
```

`GET /actuator/health`：公开的 Spring Boot 存活检查，只返回健康状态。

除两个健康接口和四条 `POST /api/v1/bazi/{charts,flow-months,flow-days,flow-hours}` 计算接口外，其他路径默认禁止访问；尚无账户认证。Springdoc 依赖已声明，但文档路径尚未放行。

前端使用相对路径请求 `/api/v1/health`，由 Vite 开发代理转发至 `http://127.0.0.1:8080`。无需跨域配置。生产环境需单独配置反向代理。

## 四柱排盘

`POST /api/v1/bazi/charts`：公开、无状态的四柱计算接口，不存储出生信息。该路径单独豁免 CSRF；其他路径仍保持默认安全限制。

```json
{"birthDate":"1986-05-29","birthTime":"13:37","gender":"MALE"}
```

基础响应节选（完整结果还包含下文的详解、神煞、胎元两宫、性别和起运运程字段）：

```json
{
  "inputCalendar":"SOLAR",
  "solarDate":"1986-05-29",
  "lunarDate":{"year":1986,"month":4,"day":21,"leapMonth":false,"display":"1986年四月廿一"},
  "birthDateTime":"1986-05-29T13:37:00",
  "timeZone":"UTC+08:00",
  "precision":"MINUTE",
  "engineVersion":"tyme4j-1.5.2",
  "ruleVersion":"lichun-jie-zi23-v1",
  "pillars":{"year":"丙寅","month":"癸巳","day":"癸酉","hour":"己未"}
}
```

输入按固定 UTC+08:00 钟表时间解释，不使用服务器时区，也不自动按浏览器时区转换。支持公历 1900-01-01 至 2100-12-31，精确到分钟，不接受非零秒或小数秒。支持公历和农历输入，不支持出生地点、夏令时或真太阳时校正。

使用 Tyme4j 1.5.2 的显式 `getSixtyCycleHour().getEightChar()`：立春时刻换年、节令时刻换月、23:00 换日。不同换日约定可能产生不同日柱，此版暂不提供策略切换。

缺少字段、无效日期/时间、超出范围或非分钟精度时返回 HTTP 400：

```json
{"message":"请提供有效的历法、出生日期和时间；公历格式为 YYYY-MM-DD，时间为 HH:mm"}
```

范围或精度错误会返回对应的具体中文说明。完整出生排盘包含四柱详解、长生神煞、胎元两宫及起运 / 大运 / 流年；流月、流日和流时使用独立接口。尚无服务端排盘保存，前端支持最多 3 份未登录本机快照。

### 公历与农历输入

公历请求可以显式指定 `calendarType: "SOLAR"`，省略时仍按公历处理；新请求必须提供性别 `gender`。公历模式仅提供 `birthDate`；农历模式仅提供 `lunarDate`，不要混传两种日期。

```json
{
  "calendarType":"LUNAR",
  "gender":"MALE",
  "lunarDate":{"year":2023,"month":2,"day":1,"leapMonth":true},
  "birthTime":"13:37"
}
```

上例输入农历 2023 年闰二月初一，对应公历 `2023-03-22`。`month` 始终为正整数 1–12，以 `leapMonth` 区分闰月；农历请求必须显式提供年、月、日及闰月标记。不存在的闰月或超过实际月份天数的日期返回 400。普通二月初一对应 `2023-02-20`，与闰二月不同。

农历年份允许 1899–2100（覆盖公历范围下限对应的上一农历年），但最终转换所得公历必须仍在 1900-01-01 至 2100-12-31 之间。公历/农历任一输入都会返回 `solarDate` 与结构化 `lunarDate`，并以 `inputCalendar` 记录输入历法；`birthDateTime` 始终是转换后的公历时间。四柱基于同一公历时刻计算，所以等价的公农历输入得到相同四柱。

日期展示按零点换日；23:00 的农历日期仍是当天，四柱日柱按既定 23 点换日规则计算，不能用日柱倒推展示日期。

### 排盘详解字段

原有 `pillars` 字符串字段保留，新增：

- `dayMaster: {name, element}`：日干及其五行。
- `pillarDetails`：按年、月、日、时顺序返回的四个条目，每项包含 `key`、`label`、`ganZhi`、`stem`、`branch`、`hiddenStems`、`naYin`、`terrain`、`selfTerrain`。
- `stem: {name, element, tenGod}`：天干名称、五行和相对于日干的十神；日干自身也返回数学关系“比肩”，界面另标记日主。
- `branch: {name, element}`：地支名称及本身的五行。
- `hiddenStems: [{name, element, tenGod, qi}]`：藏干按本气、中气、余气排列，只返回存在的项，不填补空项。藏干的十神仍以日干为参照。
- `naYin`：当前干支的纳音名称，不能与干支自身五行混为一谈。
- `elementCounts`：固定包含木、火、土、金、水五个键；四干四支各计一次，总数为 8，包含零值。不计藏干、不按旺衰/月令加权，不表示完整命局五行强弱或喜用神。

例如公历 `1986-05-29 13:37` 的日主为癸水，其年柱详解条目为：

```json
{
  "key":"year",
  "label":"年柱",
  "ganZhi":"丙寅",
  "stem":{"name":"丙","element":"火","tenGod":"正财"},
  "branch":{"name":"寅","element":"木"},
  "hiddenStems":[
    {"name":"甲","element":"木","tenGod":"伤官","qi":"本气"},
    {"name":"丙","element":"火","tenGod":"正财","qi":"中气"},
    {"name":"戊","element":"土","tenGod":"正官","qi":"余气"}
  ],
  "naYin":"炉中火",
  "terrain":"沐浴",
  "selfTerrain":"长生"
}
```

该样例的表层五行计数为 `{"木":1,"火":2,"土":2,"金":1,"水":2}`。23:00 换日后使用新的日干重新计算所有十神，展示日期仍按零点换日。

### 性别、起运、大运和流年

新排盘请求必须提供 `gender: "MALE" | "FEMALE"`，缺少、null 或非法值返回 400，不默认猜测性别。性别不改变四柱，但用于传统起运顺逆规则。

响应新增 `gender` 与 `fortune`：

- `fortune.ruleVersion`：`tyme-default-child-limit-v1`；固定使用 Tyme4j 1.5.2 默认童限算法，本应用不改动全局 provider。
- `direction`：`FORWARD` / `REVERSE`，依据年干阴阳和性别（阳男阴女顺、阴男阳女逆）。
- `startTime`：起运公历时刻，按响应中的 UTC+08:00 解释。
- `startInterval: {years, months, days, hours, minutes}`：出生至起运的折算间隔，按节令时差三天折一年计算，不是日历实岁差的简写。
- `ageConvention`：`YEAR_DIFF_PLUS_ONE`。大运与流年的年龄是库返回的年序虚岁，即对应年份减出生公历年加 1，不是周岁。
- `decades`：前 10 步大运，按时间顺序返回。每项含 `index`（1 起）、`ganZhi`、`startAge`、`endAge`、`startYear`、`endYear`、`stem`、`branch`、`naYin` 和 `annualYears`。
- `annualYears`：每步包含连续 10 年，每项有 `year`、`age`、`ganZhi`、`stem`、`branch`、`naYin`。十神以出生时的日干为参照。

示例：`{"birthDate":"1983-02-15","birthTime":"20:00","gender":"FEMALE"}` 返回顺排，起运 `1989-05-04T18:24:00`，间隔 6 年 2 月 18 日 22 小时 24 分。首步乙卯大运为 1989–1998 年、7–16 岁；首个流年 1989 己巳，而不是小运辛巳。

流年干支取 Tyme4j 的 `SixtyCycleYear`，不使用表示小运的 `Fortune.getSixtyCycle()`。逆排只影响大运干支方向，流年始终按年份递增。

流年按干支年（立春起）标注，`startYear/endYear` 用于年份分组；起运及交运年份需要结合实际交运时刻区分，不能视为整年都属于同一步大运。当前不展开起运前童限和小运；流月与流日通过下述独立接口按需加载。出生范围仍为公历 1900–2100，大运输出可自然延伸至 2100 年之后。

### 流月与流日（按需计算）

`POST /api/v1/bazi/flow-months`：

```json
{"birthDate":"1986-05-29","birthTime":"13:37","year":2024}
```

`POST /api/v1/bazi/flow-days`：

```json
{"birthDate":"1986-05-29","birthTime":"13:37","year":2024,"monthIndex":1}
```

两个接口均为公开、无状态计算，单独豁免 CSRF，其他路径仍受限制。出生日期必须是排盘结果转换后的公历日期，时间为 UTC+08:00 分钟精度。性别不影响流月/流日干支及其相对于出生日干的十神，所以明细请求不需要性别；新建完整排盘的 `/charts` 仍要求性别。

流年范围 1900–2210，覆盖现有大运可能延伸的年份；出生范围仍为 1900–2100。`monthIndex` 为 1–12，1=寅月（立春），12=丑月（下一公历年小寒），不是公历月号或农历月号。参数缺失、日期/精度无效、年份或月序越界返回 400。

流月响应包含 `year`、`yearGanZhi`、`ruleVersion: "jie-month-zi23-day-v1"`、出生 `dayMaster` 和 12 个 `months`。每月有 `index`、`name`、`term`（起始节令）、`startTime`、`endTime`、`ganZhi`、`stem`、`branch`、`naYin`。月干支使用 Tyme4j 的 `SixtyCycleMonth`，区间使用 `SolarTerm.getJulianDay()` 的精确节令时刻，起点含、终点不含。中气不另划分月份。

流日响应包含 `year`、`monthIndex`、`ruleVersion`、出生 `dayMaster`、对应 `month` 和 `days`。每个流日含 `date`（主要覆盖的公历日标签）、该标签对应 `lunarDate`、`startTime`、`endTime`、`partial`（是否被节令截短）、`ganZhi`、`stem`、`branch`、`naYin`。

流日从前一天 23:00 至当天 23:00，月首/月末再与节令区间取交集，所以同一干支日在相邻两个月可能分别出现部分时段。流日标签及农历日期不是区间起始夜晚的民用日期；使用返回的起止时刻判断实际覆盖范围。

固定样例：2024 年寅月丙寅，从 `2024-02-04T16:27:07` 至 `2024-03-05T10:22:45`。第一段为 `2024-02-04` 戊戌，覆盖立春至当天 23:00；下一段为 `2024-02-05` 己亥，从 2 月 4 日 23:00 开始。该月包含闰日 2 月 29 日癸亥。丑月跨至 2025 年，但仍属于所选 2024 干支年的月序。

前端仅在用户打开对应选择器时加载 12 流月或所选流月的流日，选择上级不会自动加载下级；切换上级时取消过期请求并丢弃旧响应。每个页面最多保留 24 份流月列表和 24 份流日列表以复用，不写入 localStorage。本机快照的四柱/大运/流年仍可离线查看，未加载的流月/流日需要后端连接。

### 流时：所选流日的十二时辰

`POST /api/v1/bazi/flow-hours`：

```json
{"birthDate":"1986-05-29","birthTime":"13:37","year":2024,"monthIndex":1,"date":"2024-02-29"}
```

请求沿用流月/流日的公历出生信息、流年和月序，`date` 必须是该节令流月返回的流日标签。性别不影响流时干支，不需要提供。缺失、非法日期、范围越界或所选日期不属于该月时返回 400；精确计算路径公开并单独豁免 CSRF。

响应包含 `year`、`monthIndex`、`date`、`ruleVersion: "zi23-two-hour-jie-clip-v1"`、出生 `dayMaster`、所选 `day` 及固定 12 个 `hours`。使用 Tyme4j `SixtyCycleDay.getHours()`，从前一日 23:00 子时至当日 23:00，依次子、丑、寅、卯、辰、巳、午、未、申、酉、戌、亥。

每个时辰包含：

- `index`：1–12；`name`：子时等名称。
- `nominalStartTime`、`nominalEndTime`：标准两小时区间，起点含、终点不含。
- `included`：是否与当前节令流月内的所选流日区间有交集。
- `startTime`、`endTime`：实际交集区间；没有交集时都为 null，不虚构时间段。
- `partial`：有交集但被节令边界截短。
- `ganZhi`、`stem`（含五行/十神）、`branch`（含五行）、`naYin`：时辰属性。十神参照出生时日干，不是所选流日的日干。

示例：2024-02-29 癸亥流日的子时为壬子，标准及实际区间均为 `2024-02-28T23:00:00` 至 `2024-02-29T01:00:00`；相对于样例出生日干癸，十神为劫财，五行为水水，纳音桑柘木。

2024 年寅月首日从立春 16:27:07 才开始，仍返回十二时辰供对照，但前八个时辰 `included=false`。申时实际区间为 16:27:07–17:00，标记 `partial=true`，之后酉/戌/亥完整覆盖。惊蛰落在巳时中间时，相邻两个月的同一流日分别覆盖该巳时的前、后部分，区间不重叠。

前端在流时选择面板中展示十二时辰选项，选定后仅展示该时辰详情，明确区间外/部分覆盖状态，并区分标准区间和实际交集。流日变化会取消旧请求，流时可单独重试；同一排盘联动区域最多保留 24 份流时响应的内存缓存，跨月返回也可复用，不写入 localStorage。

## 十二长生、神煞与胎元两宫

| 响应字段 | 内容 | 详细说明 |
| --- | --- | --- |
| `pillarDetails[].terrain` | 日干对该柱地支的十二长生 | [长生与神煞](../natal-traits.md) |
| `pillarDetails[].selfTerrain` | 该柱本干对本支的自坐长生 | [长生与神煞](../natal-traits.md) |
| `shenSha` | `provider`、固定 `ruleVersion`、七项 `rules` 目录和含 `bases` 参照依据的逐柱 `hits` | [规则查表与命中结构](../natal-traits.md) |
| `auxiliaryPalaces` | `ruleVersion`、胎元 `fetalOrigin`、命宫 `ownSign`、身宫 `bodySign`；每项含干支、天干十神、干支五行及纳音 | [原生接口与完整样例](../natal-palaces.md) |

以上属于同一次出生 Chart，不计入流月 / 日 / 时接口的结果范围；不改变四柱数量、表层五行 8 字统计或请求字段。前端历史快照可缺新字段，新 HTTP 计算会返回完整新字段。
