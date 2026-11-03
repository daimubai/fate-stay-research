package dev.fatestay.research.calendar;

import com.tyme.eightchar.EightChar;
import com.tyme.solar.SolarTime;
import com.tyme.solar.SolarDay;
import com.tyme.lunar.LunarDay;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Pinned Tyme4j rules: Li Chun year, solar-term month, day changes at 23:00. */
public final class TymeCalendar {
    public Pillars fourPillars(LocalDateTime time) {
        // Use the explicit SixtyCycle API; do not mutate LunarHour's global provider.
        EightChar chart = SolarTime.fromYmdHms(time.getYear(), time.getMonthValue(),
                time.getDayOfMonth(), time.getHour(), time.getMinute(), 0)
                .getSixtyCycleHour().getEightChar();
        return new Pillars(chart.getYear().getName(), chart.getMonth().getName(),
                chart.getDay().getName(), chart.getHour().getName());
    }

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
