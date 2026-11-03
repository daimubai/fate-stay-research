# Scaffold verification / 骨架验证

Date / 日期: 2026-10-21

- Windows, Node 22.22.0, local JDK 19.
- Maven Wrapper downloads Maven 3.10.0; distribution SHA-256 is recorded and checked. Its downloaded archive was also compared with the official SHA-512.
- `mvnw.cmd -Ppersistence verify`: passed for all four modules. Resolves the optional MyBatis, MySQL driver and Flyway dependencies without a database connection.
- `npm run deps:check`: passed with no invalid top-level dependency relationships.
- No source code or tests exist. No application startup, API, database, UI or JDK 26 compilation has been tested. CI is configured to use JDK 26; its first GitHub run is pending.
- Maven 3.10 emitted optional remote prefix-metadata HTTP 401 warnings for repositories declared transitively by Flyway; the build still succeeded and no credentials are required for this scaffold.

本次仅验证目录、工程配置和依赖。构建成功不代表业务功能可用；本机 JDK 19，未验证真实源码在 JDK 26 下的编译。GitHub CI 已配置 JDK 26，首次远端执行仍待提交后确认。

## 2026-11-03 macOS entry-point verification

- Apple Silicon; OpenJDK 26.0.2.1, Node 22.22.0, npm 10.9.4, Maven Wrapper 3.10.0.
- Added a Boot executable entry point, application health endpoint, explicit security allowlist and Vue/Vite connection-check page.
- Three integration tests cover public application health, public Actuator health and denial of other routes.
- Frontend clean lockfile install, typecheck/build and dependency check pass. TypeScript is pinned to 6.0.3 for vue-tsc compatibility.
- `follow-redirects` is locked to 1.16.2; `npm audit` reports zero vulnerabilities at verification time.
- Browser plugin runtime/skill unavailable; Playwright with installed Chrome checked desktop 1280×900 and mobile 390×844. Verified page identity, meaningful content, no Vite overlay, clean console on normal load/retry, successful real API proxy request, injected 503 failure and recovery, and no mobile horizontal overflow.
- Local profile excludes datasource auto-configuration until database implementation exists. No database, authentication or calculation functionality was added.

## 2026-11-03 minimum Bazi calculation

- Added a framework-independent Tyme4j calendar adapter and Bazi calculation core, plus `POST /api/v1/bazi/charts` and the Vue birth-date/time form with four-pillar results.
- Input policy: Gregorian 1900–2100, minute precision, fixed UTC+08:00 without daylight-saving/true-solar correction. Explicit SixtyCycle API uses Li Chun year, solar-term month and 23:00 day boundaries; no global provider mutation.
- `./mvnw verify`: passed. 13 core tests cover three upstream fixture examples, leap day, Li Chun/Jing Zhe boundaries, 22:59/23:00/midnight, range/precision rejection, metadata and concurrent determinism. 11 HTTP/security tests cover calculation, invalid dates/times, malformed/missing input and existing route restrictions.
- `./mvnw -Ppersistence test`: passed, same 24 tests, no database required.
- `npm run build` and `npm audit`: passed, zero reported vulnerabilities.
- Browser runtime/skill unavailable; Playwright + installed Chrome used at desktop 1280×1000 and mobile 390×844 (also 390×1100). Real POST through Vite returned 丙寅/癸巳/癸酉/己未 for 1986-05-29 13:37. Verified title, content, no error overlay, clean console on normal flow, four-pillar display, 23:00 rollover, clearing old results on edits, injected 503 feedback, retry recovery, invalid-date HTTP 400 and no horizontal overflow.
- Calendar/engine fixtures reference [upstream EightCharTest](https://github.com/6tail/tyme4j/blob/master/src/test/java/com/tyme/test/EightCharTest.java). Boundary fixtures are pinned to Tyme4j 1.5.2; they test integration with that ruleset rather than independent astronomical accuracy.
- Not implemented: lunar input, location/time-zone selection, daylight-saving correction, true solar time, fortune cycles, account login or persistence. No new runtime dependencies were introduced.

## 2026-11-03 Gregorian / lunar input extension

- Added explicit SOLAR/LUNAR input selection, regular/leap lunar month selection and both Gregorian/lunar date outputs. Existing requests without calendarType remain Gregorian.
- Tyme4j validates actual lunar month lengths and leap-month existence; converted Gregorian dates must remain within 1900–2100. Lunar year 1899 is accepted to cover the lower Gregorian boundary. No global calendar provider changes.
- `./mvnw verify` and `./mvnw -Ppersistence test`: passed with 21 core tests and 17 HTTP/security tests. New fixtures cover lunar new year, 2023 leap second month, 2020 leap fourth month, regular vs leap months, equivalence of charts for converted inputs, invalid leap/month/day input, midnight vs 23:00 distinction, and both Gregorian range edges.
- `npm run build`: type checking and production build passed.
- Playwright + Chrome, desktop 1280×1100 and mobile 390×844: actual Gregorian and lunar requests return matching pillars; both date displays, changing calendar clears old results, ordinary/leap month distinction, invalid leap-month feedback and successful retry verified. Normal flow console clean, no framework overlay or mobile horizontal overflow. Browser runtime/skill unavailable; existing temporary Playwright setup reused.

## 2026-11-03 desktop / mobile picker upgrade

- Researched official Element Plus, Vant and Tyme documentation and checked npm registry versions. Desktop: Element Plus 2.14.7. Mobile: Vant 4.10.2. Lunar adaptation uses Tyme4ts 1.5.2 to match backend Tyme4j 1.5.2. Details: [picker selection](date-time-picker.md).
- Replaced native date/time/select form with separate async desktop and mobile components. Common model stores a solar day and explicit input calendar; switching calendar and resizing preserve the selected date/time.
- Desktop supports date input/year navigation, dual-calendar day cells, searchable lunar year, valid regular/leap months and minute-precision time input. Mobile uses draft-based bottom sheets, year jump, actual valid month/day wheels, double-calendar preview, confirm/cancel, 24-hour time, 500ms inertia and safe-area padding.
- `npm test`: 5 adapter regression tests passed (actual leap month choices/day lengths, same-day conversion, day clamping, Gregorian range edges, positive-month plus explicit leap-flag API payload). CI now runs these tests.
- `npm run build`, `npm run deps:check` and `npm audit`: passed, zero reported vulnerabilities. Desktop/mobile scripts and component styles build as separate lazy chunks.
- Browser skill/runtime absent; existing Playwright + Chrome used. Desktop 1280×1100: keyboard entry preserves 2023-03-22 13:37; calendar switch preserves date; year search removes nonexistent leap month; real lunar API response exactly matches input and preview; normal-flow console clean and no Vite overlay.
- Mobile 390×844 with touch emulation: tapped wheels select dates, year jump and leap-month conversion verified; cancellation leaves original values unchanged; date/time confirmations produce exact backend 2024-01-01 13:02 response; resize to desktop preserves both fields. Actual CDP touch swipes move time wheels, and confirmation matches the highlighted settled value. 320×568 popup confirmation/scroll works without horizontal overflow.
- Screenshots inspected for desktop results, mobile date sheet, mobile time sheet and mobile result layout. No real-device Safari/Android hardware run performed.

## 2026-11-03 ten gods / hidden stems / elements / Na Yin

- Added backend enrichment based on the same pinned Tyme4j EightChar result: day master, four pillar details, stem ten gods, stem/branch elements, main/middle/residual hidden stems with their elements and ten gods, plus Na Yin. Existing pillar strings remain compatible.
- Ten-god reference is always the actual day stem, including after 23:00 rollover. The day stem itself returns 比肩 and the UI marks 日主 separately. Missing middle/residual hidden stems are omitted, not padded with nulls.
- Element counts include four stems and four branches only (total 8), always contain all five keys, and do not include hidden stems, Na Yin or weighting. UI explains the scope and does not infer strength/favorable elements from these counts.
- `./mvnw verify` and `./mvnw -Ppersistence test`: passed, 45 core tests (21 existing plus 24 detail tests) and 18 HTTP/security tests. Fixtures cover a known 癸水 chart, all ten relationships for both 甲/癸 day masters, all twelve branches' hidden stems and order, Na Yin, immutable output, zero/total element counts, lunar/solar equivalence and 23:00 reference changes.
- `npm run build` and `npm test`: passed, type/production build and all 5 calendar-adapter regressions. No new dependencies added.
- Existing Playwright + Chrome used (Browser skill/runtime absent). Desktop 1280×1100: real 1986-05-29 13:37 response and table match 日主癸水, 正财/比肩/比肩/七杀, 炉中火/长流水/剑锋金/天上火, and 木1火2土2金1水2; help disclosure opens/closes; normal flow console clean/no Vite overlay.
- Mobile 390×844 and 320×568: four readable pillar cards, one- and three-item hidden-stem cases, element labels/counts and no horizontal overflow. Editing time clears enrichment; re-submission at 23:00 changes day master to 甲 and the year stem relationship to 食神. Desktop and mobile screenshots inspected. No real-device Safari test performed.

## 2026-11-03 guest browser-local chart library

- Added browser-only localStorage snapshots under `fatestay.guest-charts.v1`, with explicit guest/local UI and a maximum of three records. No account/database/record API was added.
- Successful calculation auto-saves the birth input, chosen calendar, full result and engine/rule versions. Equivalent solar/lunar input at the same solar minute updates one record, preserving its name. The active record restores on refresh; new blank draft keeps records but resets active selection.
- Added record cards, current selection, rename/delete with inline confirmation, explicit replacement when full, optional unsaved result display, manual save and result-area quick switching. Switching uses the saved snapshot without recalculation; stale async computation results cannot overwrite a selected birth input.
- Added read-time validation/recovery for malformed, oversized, incompatible and incomplete snapshots. Storage denial/quota failure falls back to in-page records with a visible warning.
- `npm test`: 16 total tests pass (5 calendar-adapter plus 11 library tests). New cases cover snapshot isolation, equivalent-calendar deduplication, three-slot capacity, explicit replacement, serialization/active state, deletion/naming, invalid cache/details, quota/disabled storage and reactive proxies. `npm run build` passes type checking and production build. No dependencies added.
- Existing Playwright + Chrome used (Browser skill/runtime unavailable), desktop 1280×1000 and mobile 390×844. Verified real chart autosave, rename, fourth-record rejection, temporarily unsaved/retry, explicit replacement, repeated-chart deduplication, header/result quick switching without POST, refresh of active full details, offline in-page snapshot switching, delete/re-save, blank new draft after refresh, corrupted cache and storage quota denial. Normal flow console clean, no error overlay, mobile no horizontal overflow. Screenshots inspected.
- Offline switching applies to an already loaded page; no service worker or offline application loading was introduced. Real-device Safari/Android testing remains unperformed.

## 2026-11-03 gender / start fortune / decades / annual years

- New HTTP chart requests require explicit MALE/FEMALE gender. Gender changes fortune direction, not the four pillars. Legacy pure-core overloads still allow four-pillars-only use; legacy cached snapshots remain viewable and prompt for gender before recalculating timelines.
- Pinned Tyme4j default ChildLimit rules: year-stem Yin/Yang with gender determines direction; solar-term interval converts using three days per year. No application code mutates global providers. Outputs include exact start datetime, converted years/months/days/hours/minutes, ten decades and ten annual years for each decade.
- Annual Gan-Zhi uses SixtyCycleYear, not Fortune.getSixtyCycle (the latter is minor fortune). Annual years remain ascending even for reverse decades. Ages use YEAR_DIFF_PLUS_ONE, and UI explains nominal-year grouping versus actual mid-year handover; no pre-start minor fortune/month/day timeline added.
- `./mvnw verify` and `./mvnw -Ppersistence test`: passed, 58 core tests (including 13 new fortune cases) and 25 HTTP/security tests. New fixtures cover four upstream default start-time/interval examples, all four direction combinations, known forward/reverse decades and true annual Gan-Zhi, ten-by-ten continuity, immutability, solar/lunar equivalence, gender invariance of pillars, null/unknown/numeric gender, Li Chun boundary and upper birth range.
- Frontend `npm test`: 20 total tests passed. New coverage includes required gender, same-minute gender-separated cache identities, legacy slot upgrade preserving name, complete timeline round-trip and rejection of wrong gender/incomplete or nonchronological timeline snapshots. `npm run build` passed; no new dependencies added.
- Existing Playwright + Chrome (Browser skill/runtime absent), PC 1280×1100 and mobile 390×844 / 320×568: required gender blocks requests until selected; 1983-02-15 20:00 FEMALE returns 1989-05-04 18:24, first decade 乙卯 and first annual year 己巳. Selecting decade/annual year updates details; desktop normal-flow console clean/no Vite overlay. Mobile horizontal decade controls and annual cards work with no page overflow. Screenshots inspected.
- Browser cache checks passed: old genderless record retained, explicit gender recalculation upgrades its original slot, different genders occupy separate slots, full fortune snapshot survives refresh and offline in-page switching. Real-device Safari/Android checks not performed.
