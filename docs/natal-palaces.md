# 胎元、命宫与身宫

## 计算口径

基于固定版本 Tyme4j 1.5.2，使用同一份出生四柱的原生接口：

| 项目 | 接口 | 参照 |
| --- | --- | --- |
| 胎元 | `EightChar.getFetalOrigin()` | 月干顺进一位、月支顺进三位 |
| 命宫 | `EightChar.getOwnSign()` | 出生年干、月支、时支 |
| 身宫 | `EightChar.getBodySign()` | 出生年干、月支、时支 |

四柱通过显式的 `SolarTime.getSixtyCycleHour().getEightChar()` 获取，不修改全局 provider。沿用北京时间 UTC+08:00、立春换年、节令换月、23 点换日，不校正夏令时或真太阳时。农历输入先转为同一出生时刻再计算，闰月也沿用该路径；不直接拿输入的农历月数计算两宫。性别影响起运顺逆，不影响胎元和两宫。

这是 Tyme4j 的八字宫位口径，不切换为紫微斗数命身宫。胎元只展示干支推导结果，不推算真实受孕日期。三项作为附加信息单独展示，不加入四柱数组、神煞查柱或表层五行 8 字计数。

来源：[Tyme 官方文档「八字」](https://6tail.cn/tyme.html)、固定依赖 1.5.2 的 `EightChar` 源码；样例参考 [Tyme4j 上游 EightCharTest](https://github.com/6tail/tyme4j/blob/main/src/test/java/com/tyme/test/EightCharTest.java)。

## 接口

现有 `POST /api/v1/bazi/charts` 的请求不变，新增响应 `auxiliaryPalaces`。不新增公开接口或依赖，不改变原有四柱规则版本。

```json
{
  "auxiliaryPalaces": {
    "ruleVersion": "tyme-eight-char-palaces-v1",
    "fetalOrigin": {
      "ganZhi": "甲申",
      "stem": { "name": "甲", "element": "木", "tenGod": "伤官" },
      "branch": { "name": "申", "element": "金" },
      "naYin": "泉中水"
    },
    "ownSign": {
      "ganZhi": "癸巳",
      "stem": { "name": "癸", "element": "水", "tenGod": "比肩" },
      "branch": { "name": "巳", "element": "火" },
      "naYin": "长流水"
    },
    "bodySign": {
      "ganZhi": "辛丑",
      "stem": { "name": "辛", "element": "金", "tenGod": "偏印" },
      "branch": { "name": "丑", "element": "土" },
      "naYin": "壁上土"
    }
  }
}
```

样例出生时间为公历 1986-05-29 13:37（农历丙寅年四月廿一），四柱丙寅、癸巳、癸酉、己未。每项的天干十神均相对出生日干癸计算。纳音是整个干支的分类，与天干或地支的自身五行分别展示。

## 展示与缓存

默认收起，仅显示胎元和两宫摘要；展开后 PC 使用三列卡片，手机使用纵向卡片；展示干支、干支五行、天干十神、纳音和推导来源，展开可查看计算口径与版本。切换本机记录时同步切换三项结果，不另发请求。

`auxiliaryPalaces` 在前端类型中可缺省，仅用于兼容历史快照。旧记录继续显示已有排盘信息，并提示重新排盘补齐；相同出生时刻、性别重新排盘时更新原位置，保留名称。存在新字段时，校验三个结果和规则版本，缺项或损坏的缓存跳过。仍最多三份，本功能不引入登录或服务端保存。

## 核心验证

- 上游四个完整出生样例与命宫、身宫历史回归样例。
- 胎元和两宫的干支、五行、十神、纳音，原四柱与五行统计不受影响。
- 十二时辰、同一时辰内结果一致、时辰边界、23 点换日后的十神参照。
- 立春前后、惊蛰交节前后，确保使用出生四柱的年干、月支和时支。
- 公历/农历等价输入、闰月转换、性别一致性和出生范围两端。
- HTTP 新字段、公历/农历响应、旧缓存升级与损坏缓存恢复。
