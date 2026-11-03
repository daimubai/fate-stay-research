package dev.fatestay.research.calendar;

import com.tyme.eightchar.EightChar;
import com.tyme.eightchar.ChildLimit;
import com.tyme.enums.Gender;
import com.tyme.sixtycycle.SixtyCycle;
import com.tyme.sixtycycle.SixtyCycleMonth;
import com.tyme.sixtycycle.SixtyCycleYear;
import com.tyme.sixtycycle.SixtyCycleDay;
import com.tyme.solar.SolarTerm;
import java.time.Duration;
import com.tyme.sixtycycle.HeavenStem;
import java.util.ArrayList;
import java.util.List;
import com.tyme.solar.SolarTime;
import com.tyme.solar.SolarDay;
import com.tyme.lunar.LunarDay;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Pinned Tyme4j rules: Li Chun year, solar-term month, day changes at 23:00. */
public final class TymeCalendar {
    public Pillars fourPillars(LocalDateTime time) {
        return analyze(time).pillars();
    }

    public Analysis analyze(LocalDateTime time) {
        // Use the explicit SixtyCycle API; do not mutate LunarHour's global provider.
        EightChar chart = SolarTime.fromYmdHms(time.getYear(), time.getMonthValue(),
                time.getDayOfMonth(), time.getHour(), time.getMinute(), 0)
                .getSixtyCycleHour().getEightChar();
        HeavenStem dayMaster = chart.getDay().getHeavenStem();
        var pillars = new Pillars(chart.getYear().getName(), chart.getMonth().getName(),
                chart.getDay().getName(), chart.getHour().getName());
        return new Analysis(pillars, new DayMaster(dayMaster.getName(), dayMaster.getElement().getName()),
                List.of(describe("year", "年柱", chart.getYear(), dayMaster),
                        describe("month", "月柱", chart.getMonth(), dayMaster),
                        describe("day", "日柱", chart.getDay(), dayMaster),
                        describe("hour", "时柱", chart.getHour(), dayMaster)));
    }

    private PillarDetail describe(String key, String label, SixtyCycle pillar, HeavenStem dayMaster) {
        var stem = pillar.getHeavenStem();
        var branch = pillar.getEarthBranch();
        List<HiddenStem> hidden = new ArrayList<>();
        addHidden(hidden, branch.getHideHeavenStemMain(), "本气", dayMaster);
        addHidden(hidden, branch.getHideHeavenStemMiddle(), "中气", dayMaster);
        addHidden(hidden, branch.getHideHeavenStemResidual(), "余气", dayMaster);
        return new PillarDetail(key, label, pillar.getName(),
                new Stem(stem.getName(), stem.getElement().getName(), dayMaster.getTenStar(stem).getName()),
                new Branch(branch.getName(), branch.getElement().getName()),
                List.copyOf(hidden), pillar.getSound().getName());
    }

    private void addHidden(List<HiddenStem> target, HeavenStem stem, String qi, HeavenStem dayMaster) {
        if (stem != null) {
            target.add(new HiddenStem(stem.getName(), stem.getElement().getName(),
                    dayMaster.getTenStar(stem).getName(), qi));
        }
    }

    public record DayMaster(String name, String element) {}
    public record Stem(String name, String element, String tenGod) {}
    public record Branch(String name, String element) {}
    public record HiddenStem(String name, String element, String tenGod, String qi) {}
    public record PillarDetail(String key, String label, String ganZhi, Stem stem, Branch branch,
                               List<HiddenStem> hiddenStems, String naYin) {}
    public record Analysis(Pillars pillars, DayMaster dayMaster, List<PillarDetail> pillarDetails) {}

    public FortuneTimeline fortune(LocalDateTime time, boolean male) {
        var solar = SolarTime.fromYmdHms(time.getYear(), time.getMonthValue(), time.getDayOfMonth(),
                time.getHour(), time.getMinute(), 0);
        // Use Tyme4j's default ChildLimit provider; this application never mutates global providers.
        var limit = ChildLimit.fromSolarTime(solar, male ? Gender.MAN : Gender.WOMAN);
        var dayMaster = solar.getSixtyCycleHour().getEightChar().getDay().getHeavenStem();
        List<Decade> decades = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            var decade = limit.getStartDecadeFortune().next(i);
            var cycle = decade.getSixtyCycle();
            List<AnnualYear> years = new ArrayList<>();
            for (int j = 0; j < 10; j++) {
                // Fortune.getSixtyCycle() is a minor fortune, NOT the annual Gan-Zhi.
                var year = decade.getStartSixtyCycleYear().next(j);
                var annual = year.getSixtyCycle();
                years.add(new AnnualYear(year.getYear(), decade.getStartAge() + j, annual.getName(),
                        stem(annual, dayMaster), branch(annual), annual.getSound().getName()));
            }
            decades.add(new Decade(i + 1, cycle.getName(), decade.getStartAge(), decade.getEndAge(),
                    decade.getStartSixtyCycleYear().getYear(), decade.getEndSixtyCycleYear().getYear(),
                    stem(cycle, dayMaster), branch(cycle), cycle.getSound().getName(), List.copyOf(years)));
        }
        var start = limit.getEndTime();
        return new FortuneTimeline("tyme-default-child-limit-v1", limit.isForward() ? "FORWARD" : "REVERSE",
                LocalDateTime.of(start.getYear(), start.getMonth(), start.getDay(), start.getHour(), start.getMinute(), start.getSecond()),
                new StartInterval(limit.getYearCount(), limit.getMonthCount(), limit.getDayCount(),
                        limit.getHourCount(), limit.getMinuteCount()), "YEAR_DIFF_PLUS_ONE", List.copyOf(decades));
    }

    private Stem stem(SixtyCycle cycle, HeavenStem dayMaster) {
        var stem = cycle.getHeavenStem();
        return new Stem(stem.getName(), stem.getElement().getName(), dayMaster.getTenStar(stem).getName());
    }
    private Branch branch(SixtyCycle cycle) {
        var branch = cycle.getEarthBranch();
        return new Branch(branch.getName(), branch.getElement().getName());
    }
    public record StartInterval(int years, int months, int days, int hours, int minutes) {}
    public record AnnualYear(int year, int age, String ganZhi, Stem stem, Branch branch, String naYin) {}
    public record Decade(int index, String ganZhi, int startAge, int endAge, int startYear, int endYear,
                         Stem stem, Branch branch, String naYin, List<AnnualYear> annualYears) {}
    public record FortuneTimeline(String ruleVersion, String direction, LocalDateTime startTime,
                                  StartInterval startInterval, String ageConvention, List<Decade> decades) {}

    public FlowMonths flowMonths(int year, String dayStem) {
        var master = HeavenStem.fromName(dayStem);
        List<FlowMonth> months = new ArrayList<>();
        for (int i = 0; i < 12; i++) months.add(flowMonth(year, i + 1, master));
        return new FlowMonths(year, SixtyCycleYear.fromYear(year).getSixtyCycle().getName(),
                "jie-month-zi23-day-v1", new DayMaster(master.getName(), master.getElement().getName()), List.copyOf(months));
    }

    private FlowMonth flowMonth(int year, int index, HeavenStem master) {
        var month = SixtyCycleMonth.fromIndex(year, index - 1).getSixtyCycle();
        var term = SolarTerm.fromIndex(year, 3 + (index - 1) * 2);
        return new FlowMonth(index, month.getEarthBranch().getName() + "月", term.getName(),
                localTime(term.getJulianDay().getSolarTime()), localTime(term.next(2).getJulianDay().getSolarTime()),
                month.getName(), stem(month, master), branch(month), month.getSound().getName());
    }

    public FlowDays flowDays(int year, int monthIndex, String dayStem) {
        var master = HeavenStem.fromName(dayStem);
        var month = flowMonth(year, monthIndex, master);
        List<FlowDay> days = new ArrayList<>();
        var cursor = month.startTime();
        while (cursor.isBefore(month.endTime())) {
            var boundary = cursor.toLocalDate().atTime(23, 0);
            if (!cursor.isBefore(boundary)) boundary = boundary.plusDays(1);
            var end = boundary.isBefore(month.endTime()) ? boundary : month.endTime();
            var cycle = SolarTime.fromYmdHms(cursor.getYear(), cursor.getMonthValue(), cursor.getDayOfMonth(),
                    cursor.getHour(), cursor.getMinute(), cursor.getSecond()).getSixtyCycleHour().getDay();
            // Label by the civil date mainly covered by the 23:00-to-23:00 Gan-Zhi day.
            var date = cursor.getHour() >= 23 ? cursor.toLocalDate().plusDays(1) : cursor.toLocalDate();
            days.add(new FlowDay(date, toLunar(date), cursor, end, Duration.between(cursor, end).getSeconds() != 86400,
                    cycle.getName(), stem(cycle, master), branch(cycle), cycle.getSound().getName()));
            cursor = end;
        }
        return new FlowDays(year, monthIndex, "jie-month-zi23-day-v1",
                new DayMaster(master.getName(), master.getElement().getName()), month, List.copyOf(days));
    }

    public FlowHours flowHours(int year, int monthIndex, LocalDate date, String dayStem) {
        var flowDays = flowDays(year, monthIndex, dayStem);
        var day = flowDays.days().stream().filter(item -> item.date().equals(date)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("流日不属于所选节令流月，请重新选择"));
        var master = HeavenStem.fromName(dayStem);
        var cycleDay = SixtyCycleDay.fromSolarDay(SolarDay.fromYmd(date.getYear(), date.getMonthValue(), date.getDayOfMonth()));
        List<FlowHour> hours = new ArrayList<>();
        int index = 0;
        for (var hour : cycleDay.getHours()) {
            var nominalStart = localTime(hour.getSolarTime());
            var nominalEnd = nominalStart.plusHours(2);
            var start = nominalStart.isAfter(day.startTime()) ? nominalStart : day.startTime();
            var end = nominalEnd.isBefore(day.endTime()) ? nominalEnd : day.endTime();
            boolean included = start.isBefore(end);
            var cycle = hour.getSixtyCycle();
            hours.add(new FlowHour(++index, cycle.getEarthBranch().getName() + "时", nominalStart, nominalEnd,
                    included ? start : null, included ? end : null, included,
                    included && (!start.equals(nominalStart) || !end.equals(nominalEnd)),
                    cycle.getName(), stem(cycle, master), branch(cycle), cycle.getSound().getName()));
        }
        return new FlowHours(year, monthIndex, date, "zi23-two-hour-jie-clip-v1", flowDays.dayMaster(), day, List.copyOf(hours));
    }

    public record FlowHour(int index, String name, LocalDateTime nominalStartTime, LocalDateTime nominalEndTime,
                           LocalDateTime startTime, LocalDateTime endTime, boolean included, boolean partial,
                           String ganZhi, Stem stem, Branch branch, String naYin) {}
    public record FlowHours(int year, int monthIndex, LocalDate date, String ruleVersion,
                            DayMaster dayMaster, FlowDay day, List<FlowHour> hours) {}

    private LocalDateTime localTime(SolarTime time) {
        return LocalDateTime.of(time.getYear(), time.getMonth(), time.getDay(), time.getHour(), time.getMinute(), time.getSecond());
    }
    public record FlowMonth(int index, String name, String term, LocalDateTime startTime, LocalDateTime endTime,
                            String ganZhi, Stem stem, Branch branch, String naYin) {}
    public record FlowDay(LocalDate date, LunarDate lunarDate, LocalDateTime startTime, LocalDateTime endTime,
                          boolean partial, String ganZhi, Stem stem, Branch branch, String naYin) {}
    public record FlowMonths(int year, String yearGanZhi, String ruleVersion, DayMaster dayMaster, List<FlowMonth> months) {}
    public record FlowDays(int year, int monthIndex, String ruleVersion, DayMaster dayMaster, FlowMonth month, List<FlowDay> days) {}

    public LocalDate toSolar(int year, int month, int day, boolean leapMonth) {
        if (year < 1899 || year > 2100 || month < 1 || month > 12 || day < 1 || day > 30) {
            throw new IllegalArgumentException("请提供有效的农历年月日，月份为 1–12，日期为 1–30");
        }
        try {
            var solar = LunarDay.fromYmd(year, leapMonth ? -month : month, day).getSolarDay();
            return LocalDate.of(solar.getYear(), solar.getMonth(), solar.getDay());
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("农历日期无效，请检查该年是否存在所选闰月及该月天数");
        }
    }

    public LunarDate toLunar(LocalDate date) {
        var lunar = SolarDay.fromYmd(date.getYear(), date.getMonthValue(), date.getDayOfMonth()).getLunarDay();
        var month = lunar.getLunarMonth();
        return new LunarDate(lunar.getYear(), month.getMonth(), lunar.getDay(), month.isLeap(),
                lunar.getYear() + "年" + month.getName() + lunar.getName());
    }

    public record LunarDate(int year, int month, int day, boolean leapMonth, String display) {}

    public record Pillars(String year, String month, String day, String hour) {}
}
