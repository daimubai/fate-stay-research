package dev.fatestay.research.bazi;

import dev.fatestay.research.calendar.TymeCalendar;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;

/** Framework-independent minimum four-pillars calculation at fixed UTC+08:00. */
public final class BaziCalculator {
    private final TymeCalendar calendar = new TymeCalendar();

    public Chart calculateLunar(int year, int month, int day, boolean leapMonth, LocalTime time) {
        if (time == null) throw new IllegalArgumentException("请提供出生时间");
        return calculate(calendar.toSolar(year, month, day, leapMonth).atTime(time), "LUNAR");
    }

    public Chart calculate(LocalDateTime birthTime) {
        return calculate(birthTime, "SOLAR");
    }

    private Chart calculate(LocalDateTime birthTime, String inputCalendar) {
        if (birthTime == null || birthTime.getYear() < 1900 || birthTime.getYear() > 2100) {
            throw new IllegalArgumentException("出生日期须在 1900-01-01 至 2100-12-31 之间");
        }
        if (birthTime.getSecond() != 0 || birthTime.getNano() != 0) {
            throw new IllegalArgumentException("出生时间请精确到分钟，不支持秒或小数秒");
        }
        var pillars = calendar.fourPillars(birthTime);
        return new Chart(birthTime, "UTC+08:00", "MINUTE", "tyme4j-1.5.2",
                "lichun-jie-zi23-v1", pillars, inputCalendar, birthTime.toLocalDate(),
                calendar.toLunar(birthTime.toLocalDate()));
    }

    public record Chart(LocalDateTime birthDateTime, String timeZone, String precision,
                        String engineVersion, String ruleVersion, TymeCalendar.Pillars pillars, String inputCalendar, LocalDate solarDate,
                        TymeCalendar.LunarDate lunarDate) {}
}
