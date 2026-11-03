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

    public record Chart(LocalDateTime birthDateTime, String timeZone, String precision,
                        String engineVersion, String ruleVersion, TymeCalendar.Pillars pillars, String inputCalendar, LocalDate solarDate,
                        TymeCalendar.LunarDate lunarDate, TymeCalendar.DayMaster dayMaster,
                        List<TymeCalendar.PillarDetail> pillarDetails, Map<String, Integer> elementCounts,
                        BirthGender gender, TymeCalendar.FortuneTimeline fortune) {}
}
