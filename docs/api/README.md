# API

`GET /api/v1/health`：公开的服务连通性接口，无需登录。

```json
{"status":"UP","application":"fate-stay-research"}
```

`GET /actuator/health`：公开的 Spring Boot 存活检查，只返回健康状态。

除下述四柱排盘接口外，其他路径默认禁止访问；尚无账户认证。Springdoc 依赖已声明，但文档路径尚未放行。

前端使用相对路径请求 `/api/v1/health`，由 Vite 开发代理转发至 `http://127.0.0.1:8080`。无需跨域配置。生产环境需单独配置反向代理。

## 四柱排盘

`POST /api/v1/bazi/charts`：公开、无状态的四柱计算接口，不存储出生信息。该路径单独豁免 CSRF；其他路径仍保持默认安全限制。

```json
{"birthDate":"1986-05-29","birthTime":"13:37","gender":"MALE"}
```

成功响应示例：

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

范围或精度错误会返回对应的具体中文说明。已提供十神、藏干、五行、纳音、起运、大运及流年，尚未实现服务端排盘保存；前端支持最多 3 份未登录本机记录。

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
- `pillarDetails`：按年、月、日、时顺序返回的四个条目，每项包含 `key`、`label`、`ganZhi`、`stem`、`branch`、`hiddenStems`、`naYin`。
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
  "naYin":"炉中火"
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

流年按干支年（立春起）标注，`startYear/endYear` 用于年份分组；起运及交运年份需要结合实际交运时刻区分，不能视为整年都属于同一步大运。当前不展开起运前童限、小运、流月或流日。出生范围仍为公历 1900–2100，大运输出可自然延伸至 2100 年之后。
