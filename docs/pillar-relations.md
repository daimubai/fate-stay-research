# 四柱合冲刑破害

## 范围与原则

本功能仅比较出生四柱的四个天干和四个地支，含非相邻柱位；不包含藏干、胎元两宫或大运流年等外部干支。固定依赖 Tyme4j 1.5.2，并以 `relations.ruleVersion = natal-pillar-relations-v1`、`scope = NATAL_FOUR_PILLARS` 标记口径。

关系是结构匹配结果，不判定吉凶、强弱、争合、合走、相互抵消或已合化。`associatedElement` 为合的传统配属五行，不替换原干支五行，不改动表层 8 字计数。

## 七类规则

| 类型 `kind` | 规则 | 来源 |
| --- | --- | --- |
| `STEM_COMBINATION` | 甲己土、乙庚金、丙辛水、丁壬木、戊癸火 | Tyme4j `HeavenStem.combine` |
| `BRANCH_SIX_COMBINATION` | 子丑土、寅亥木、卯戌火、辰酉金、巳申水、午未土 | Tyme4j `EarthBranch.combine` |
| `BRANCH_THREE_COMBINATION` | 申子辰水、亥卯未木、寅午戌火、巳酉丑金，须三支齐见 | 项目固定规则 |
| `BRANCH_PUNISHMENT` | 子卯互刑；寅→巳→申→寅；丑→戌→未→丑；辰午酉亥重复自刑 | 项目固定规则 |
| `BRANCH_CLASH` | 子午、丑未、寅申、卯酉、辰戌、巳亥 | Tyme4j `EarthBranch.getOpposite` |
| `BRANCH_BREAK` | 子酉、卯午、辰丑、未戌、寅亥、巳申 | 项目固定规则 |
| `BRANCH_HARM` | 子未、丑午、寅巳、卯辰、申亥、酉戌 | Tyme4j `EarthBranch.getHarm` |

当前 Tyme4j 的六十甲子天干 / 地支类型直接提供五合、六合、冲、害，未提供本功能所需的完整三合 / 刑 / 破接口。原生关系经 `fate-calendar` 的 `TymeRelations` 适配，三合 / 刑 / 破在 `fate-bazi` 的 `PillarRelationsCalculator` 中按上表匹配；不使用黄历神煞代替四柱关系。

### 三合、三刑与重复支

- 三合只认三个不同地支齐见，不输出半合、拱合或三会。三支齐见也不等同于已合化。
- 子卯相刑按互刑处理。寅巳申、丑戌未仅见两支时也列相刑，`directed=true`，参与者按刑向排序，不强制按年月日时排序。
- 三刑组的三支全部出现时，列出三柱项，`form=TRIPLE`，依据说明保留完整刑向；不再重复列出该组三个两支项。不同柱位组合仍分别保留。
- 自刑须在不同柱位重复出现辰、午、酉或亥；单支不列自刑，其他重复支也不列自刑。两个相同支为一项，三个相同支为三项，四个相同支为六项，均有具体柱位。
- 同一柱位对允许同时命中多种规则。例如寅亥可以同时显示六合和破，不做抵消。
- 身份按“关系类型 + 按年月日时排序的柱位组合”生成稳定 `id`，不按文字去重。第四柱重复三合中的一支时，保留两个不同的三柱组合。

## API 与数据结构

`POST /api/v1/bazi/charts` 请求不变，新增 `relations`；不新增公开接口或依赖，不改变四柱规则版本。公历 / 农历等价输入、性别切换均不改变同一出生四柱的关系。

| 字段 | 含义 |
| --- | --- |
| `ruleVersion`、`scope` | 固定关系规则版本及出生四柱范围 |
| `rules` | 七类目录，含 `kind`、`label`、`provider`；原生为 `TYME4J`，项目规则为 `PROJECT_RULES_ON_TYME4J` |
| `hits` | 匹配条目；无命中时为空数组 |
| `id`、`kind`、`pattern` | 稳定柱位组合 ID、关系类别、固定模式；两支相刑的 `pattern` 仍标明所属三刑组 |
| `form` | `PAIR` 两柱关系、`TRIPLE` 三支齐见、`SELF_PAIR` 不同柱位同支自刑 |
| `directed` | 是否按刑向排列参与者；三刑齐见通过 `basis` 表达环形刑向 |
| `participants` | 每项含 `pillarKey/pillarLabel/ganZhi/value`，其中 `value` 为相应天干或地支 |
| `associatedElement` | 五合 / 六合 / 三合的配属五行，其他类别为 `null` |
| `basis` | 本次匹配的简短判定依据 |

例如公历 1994-12-06 02:00，四柱为甲戌、乙亥、丙寅、己丑。`relations.hits` 中的五合项为：

```json
{
  "id": "STEM_COMBINATION:year-hour",
  "kind": "STEM_COMBINATION",
  "pattern": "甲己",
  "form": "PAIR",
  "directed": false,
  "participants": [
    {"pillarKey":"year","pillarLabel":"年柱","ganZhi":"甲戌","value":"甲"},
    {"pillarKey":"hour","pillarLabel":"时柱","ganZhi":"己丑","value":"己"}
  ],
  "associatedElement": "土",
  "basis": "Tyme4j 天干五合；配属五行，不判合化"
}
```

同盘还保留月日寅亥六合、破和时年丑刑戌。公历 2024-08-08 00:00 的四柱为甲辰、壬申、甲辰、甲子，保留“年月时”与“月日时”两项申子辰三合，以及年日辰辰自刑。

## PC / 手机展示与缓存

沿用紧凑工作台，在命盘下增加默认收起的“合冲刑破害”，摘要显示各类数量。展开后可按七类筛选，显示柱位、干支、两支 / 三支 / 自刑状态、配属五行和依据；详细口径继续收在说明内。

点击关系可标记命盘对应天干或地支。切换筛选、取消选中或关闭明细会清除标记；手机提供返回已标记柱位的链接。PC 为横向行式对照，手机为单列条目，均保留同样的数据。此功能不请求流月以下数据。

`relations` 随出生 Chart 快照缓存。旧记录缺字段时仍可查看，展开后提示重新排盘补齐；重算沿用原槽位及名称。有该字段时校验规则版本、类别目录、唯一 ID、形态、配属五行及参与者与原四柱的对应关系，跳过损坏记录。前端不重新计算后端关系。

## 验证范围

穷举 100 种天干配对和 144 种地支配对，覆盖原生映射及所有不命中情况；测试四组三合及全部排列、缺支不成立、刑向、三刑齐见合并、自刑门槛、重复支的不同柱位、非相邻关系、多关系并存、稳定 ID、不可变结果、公农历 / 性别一致性及 23 点换日。HTTP 验证字段和来源，缓存测试覆盖原位升级及损坏数据恢复。最新结果见 [验证记录](verification.md)。

## 参照资料

- [Tyme 官方文档](https://6tail.cn/tyme.html)：天干五合、地支六合、六冲、六害及配属五行接口；实际实现固定为 1.5.2。
- [《三命通会》卷二](https://zh.wikisource.org/wiki/三命通會/卷二)：十干合、六合、三合、三刑与六害。仅取结构查表，不使用命运断语。
- [《御定星历考原》三合](https://www.shidianguji.com/book/SK1618/chapter/1jurstsethma1)：四组三合的五行配属。
- [《权衡一书》刑冲破害](https://www.shidianguji.com/mid-page/7549312309756166154)：六破查表及刑冲害条目。自刑门槛与三刑展示合并方式以上述项目口径为准。
