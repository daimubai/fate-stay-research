package dev.fatestay.research.bazi;

import dev.fatestay.research.calendar.TymeCalendar;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Framework-independent minimum four-pillars calculation at fixed UTC+08:00. */
public final class BaziCalculator {
    public enum BirthGender { MALE, FEMALE }

    private final TymeCalendar calendar = new TymeCalendar();

    public Chart calculateLunar(int year, int month, int day, boolean leapMonth, LocalTime time) {
        if (time == null) throw new IllegalArgumentException("请提供出生时间");
        return calculate(calendar.toSolar(year, month, day, leapMonth).atTime(time), "LUNAR", null);
    }

    public Chart calculate(LocalDateTime birthTime) {
        return calculate(birthTime, "SOLAR", null);
    }

    public Chart calculate(LocalDateTime birthTime, BirthGender gender) {
        if (gender == null) throw new IllegalArgumentException("请选择性别以计算起运和大运");
        return calculate(birthTime, "SOLAR", gender);
    }

    public Chart calculateLunar(int year, int month, int day, boolean leapMonth, LocalTime time, BirthGender gender) {
        if (gender == null) throw new IllegalArgumentException("请选择性别以计算起运和大运");
        if (time == null) throw new IllegalArgumentException("请提供出生时间");
        return calculate(calendar.toSolar(year, month, day, leapMonth).atTime(time), "LUNAR", gender);
    }

    private Chart calculate(LocalDateTime birthTime, String inputCalendar, BirthGender gender) {
        if (birthTime == null || birthTime.getYear() < 1900 || birthTime.getYear() > 2100) {
            throw new IllegalArgumentException("出生日期须在 1900-01-01 至 2100-12-31 之间");
        }
        if (birthTime.getSecond() != 0 || birthTime.getNano() != 0) {
            throw new IllegalArgumentException("出生时间请精确到分钟，不支持秒或小数秒");
        }
        var analysis = calendar.analyze(birthTime);
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String element : List.of("木", "火", "土", "金", "水")) counts.put(element, 0);
        // Eight visible characters only: one count per stem/branch; hidden stems are not weighted here.
        for (var pillar : analysis.pillarDetails()) {
            counts.compute(pillar.stem().element(), (key, count) -> count + 1);
            counts.compute(pillar.branch().element(), (key, count) -> count + 1);
        }
        return new Chart(birthTime, "UTC+08:00", "MINUTE", "tyme4j-1.5.2",
                "lichun-jie-zi23-v1", analysis.pillars(), inputCalendar, birthTime.toLocalDate(),
                calendar.toLunar(birthTime.toLocalDate()), analysis.dayMaster(), analysis.pillarDetails(),
                Collections.unmodifiableMap(counts), gender, gender == null ? null : calendar.fortune(birthTime, gender == BirthGender.MALE));
    }

    public TymeCalendar.FlowMonths flowMonths(LocalDateTime birthTime, int year) {
        validateFlowYear(year);
        var chart = calculate(birthTime);
        return calendar.flowMonths(year, chart.dayMaster().name());
    }

    public TymeCalendar.FlowDays flowDays(LocalDateTime birthTime, int year, int monthIndex) {
        validateFlowYear(year);
        if (monthIndex < 1 || monthIndex > 12) throw new IllegalArgumentException("流月序号须为 1–12，寅月为 1");
        var chart = calculate(birthTime);
        return calendar.flowDays(year, monthIndex, chart.dayMaster().name());
    }

    public TymeCalendar.FlowHours flowHours(LocalDateTime birthTime, int year, int monthIndex, LocalDate date) {
        validateFlowYear(year);
        if (monthIndex < 1 || monthIndex > 12) throw new IllegalArgumentException("流月序号须为 1–12，寅月为 1");
        if (date == null) throw new IllegalArgumentException("请选择有效的流日日期");
        var chart = calculate(birthTime);
        return calendar.flowHours(year, monthIndex, date, chart.dayMaster().name());
    }

    private void validateFlowYear(int year) {
        if (year < 1900 || year > 2210) throw new IllegalArgumentException("流年须在 1900–2210 年之间");
    }

    public record Chart(LocalDateTime birthDateTime, String timeZone, String precision,
                        String engineVersion, String ruleVersion, TymeCalendar.Pillars pillars, String inputCalendar, LocalDate solarDate,
                        TymeCalendar.LunarDate lunarDate, TymeCalendar.DayMaster dayMaster,
                        List<TymeCalendar.PillarDetail> pillarDetails, Map<String, Integer> elementCounts,
                        BirthGender gender, TymeCalendar.FortuneTimeline fortune) {}
}
