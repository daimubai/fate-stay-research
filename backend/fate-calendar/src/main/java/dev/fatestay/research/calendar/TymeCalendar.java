package dev.fatestay.research.calendar;

import com.tyme.eightchar.EightChar;
import com.tyme.sixtycycle.SixtyCycle;
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
