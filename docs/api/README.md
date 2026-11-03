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
{"birthDate":"1986-05-29","birthTime":"13:37"}
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

范围或精度错误会返回对应的具体中文说明。尚未实现大运、流年、十神、藏干或排盘保存。

### 公历与农历输入

公历请求可以显式指定 `calendarType: "SOLAR"`，省略时仍按公历处理，兼容原接口。公历模式仅提供 `birthDate`；农历模式仅提供 `lunarDate`，不要混传两种日期。

```json
{
  "calendarType":"LUNAR",
  "lunarDate":{"year":2023,"month":2,"day":1,"leapMonth":true},
  "birthTime":"13:37"
}
```

上例输入农历 2023 年闰二月初一，对应公历 `2023-03-22`。`month` 始终为正整数 1–12，以 `leapMonth` 区分闰月；农历请求必须显式提供年、月、日及闰月标记。不存在的闰月或超过实际月份天数的日期返回 400。普通二月初一对应 `2023-02-20`，与闰二月不同。

农历年份允许 1899–2100（覆盖公历范围下限对应的上一农历年），但最终转换所得公历必须仍在 1900-01-01 至 2100-12-31 之间。公历/农历任一输入都会返回 `solarDate` 与结构化 `lunarDate`，并以 `inputCalendar` 记录输入历法；`birthDateTime` 始终是转换后的公历时间。四柱基于同一公历时刻计算，所以等价的公农历输入得到相同四柱。

日期展示按零点换日；23:00 的农历日期仍是当天，四柱日柱按既定 23 点换日规则计算，不能用日柱倒推展示日期。
