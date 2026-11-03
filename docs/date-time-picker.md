# 公农历出生日期与时间选择器

选型核验日期：2026-11-03。

## 选型

| 用途 | 采用方案 | 依据与适配 |
| --- | --- | --- |
| PC | Element Plus 2.14.7 | 当前项目已固定、npm 查询仍为当前稳定版。公历 DatePicker 提供键盘输入及年/月快速导航，单元格展示农历；农历由可搜索年份和有效月份/日期 Select 组成，时间由 TimePicker 提供。 |
| 手机 | Vant 4.10.2 | npm 查询的当前稳定版。使用 Picker + Popup 底部滚轮及 TimePicker，适合触屏；农历通过自定义列适配，而非声称原生支持农历。 |
| 公农历适配 | Tyme4ts 1.5.2 | 与后端 Tyme4j 1.5.2 对齐。虽然 npm 当前发布为 1.5.4，本项目主动保留同版本计算基线；此库只负责客户端预览和有效选项，最终四柱仍由后端计算。 |

参考官方资料：

- [Element Plus DatePicker](https://element-plus.org/en-US/component/date-picker.html)：自定义单元格、输入格式、禁用日期、键盘输入。
- [Element Plus TimePicker](https://element-plus.org/en-US/component/time-picker.html)。
- [Vant Picker 官方文档源码](https://github.com/vant-ui/vant/blob/main/packages/vant/src/picker/README.zh-CN.md)：受控多列、确认/取消和弹出层。
- [Vant TimePicker 官方文档源码](https://github.com/vant-ui/vant/blob/main/packages/vant/src/time-picker/README.zh-CN.md)。
- [Tyme4ts 官方仓库](https://github.com/6tail/tyme4ts)。

检索到的专用公农历组件中，有的基于 Vue 2 / UniApp，有的仅包装数个原生 select。此处选择已有桌面组件体系和持续发布的 Vue 3 移动组件，按同源历法统一适配。

## 交互约定

- 宽度不超过 767px 时加载手机组件，其余加载桌面组件；两套组件与样式动态导入。切换设备尺寸保留日期、时间和历法，已打开的面板随旧组件销毁。
- 内部保留单一公历日期，切换公历/农历只改变显示和提交历法，保留同一天，不重新清空输入。
- 农历有效月份按顺序含闰月（如二月、闰二月、三月）。不提供不存在的闰月或日期。年份变化时若原闰月消失，回退至同名普通月；大小月变化时将日期限制到当月实际范围。
- 日期选项以转换后的公历 1900-01-01 至 2100-12-31 为界，覆盖下限对应的农历 1899 年。
- 手机弹层的滚动和历法切换只更新草稿，确认才提交；取消或点遮罩不修改已选日期/时间。支持年份直达、双历实时预览、24 小时制及安全区底部留白。
- 桌面时间支持直接输入 HH:mm；打开空时间面板以 12:00 作为滚动起点，避免使用当前时间作为出生时间。
- 日期或时间缺失时禁止发起排盘，并给出完整输入提示。保留已有的异步结果防过期逻辑。
- 北京时间固定 UTC+08:00、分钟精度，不依赖浏览器时区转换。

## 验证

`npm test` 验证闰月选项、日数联动、历法转换和范围边界；`npm run build` 进行类型检查与生产构建。端到端验证使用真实浏览器操作两套组件并比对后端响应。
