package dev.fatestay.research.bazi;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class BaziDetailsTest {
    private final BaziCalculator calculator = new BaziCalculator();

    @Test
    void knownGuiWaterDayMasterChartHasExpectedDetails() {
        var chart = calculator.calculate(LocalDateTime.parse("1986-05-29T13:37"));
        assertEquals("癸", chart.dayMaster().name());
        assertEquals("水", chart.dayMaster().element());
        assertEquals(List.of("year", "month", "day", "hour"), chart.pillarDetails().stream().map(p -> p.key()).toList());
        assertEquals(List.of("正财", "比肩", "比肩", "七杀"), chart.pillarDetails().stream().map(p -> p.stem().tenGod()).toList());
        assertEquals(List.of("炉中火", "长流水", "剑锋金", "天上火"), chart.pillarDetails().stream().map(p -> p.naYin()).toList());
        var year = chart.pillarDetails().getFirst();
        assertEquals("丙寅", year.ganZhi());
        assertEquals("火", year.stem().element());
        assertEquals("木", year.branch().element());
        assertEquals(List.of("甲", "丙", "戊"), year.hiddenStems().stream().map(s -> s.name()).toList());
        assertEquals(List.of("伤官", "正财", "正官"), year.hiddenStems().stream().map(s -> s.tenGod()).toList());
        assertEquals(List.of("本气", "中气", "余气"), year.hiddenStems().stream().map(s -> s.qi()).toList());
        assertEquals(Map.of("木", 1, "火", 2, "土", 2, "金", 1, "水", 2), chart.elementCounts());
        var day = chart.pillarDetails().get(2);
        assertEquals(1, day.hiddenStems().size());
        assertEquals("辛", day.hiddenStems().getFirst().name());
        assertEquals("偏印", day.hiddenStems().getFirst().tenGod());
        assertThrows(UnsupportedOperationException.class, () -> year.hiddenStems().clear());
        assertThrows(UnsupportedOperationException.class, () -> chart.elementCounts().put("木", 99));
    }

    // 1986-05-30 is a Jia day. Its first ten double-hours cover all ten target stems.
    @ParameterizedTest
    @CsvSource({"0,甲,比肩", "2,乙,劫财", "4,丙,食神", "6,丁,伤官", "8,戊,偏财",
            "10,己,正财", "12,庚,七杀", "14,辛,正官", "16,壬,偏印", "18,癸,正印"})
    void allTenGodRelationsAreRelativeToTheDayStem(int hour, String stem, String tenGod) {
        var chart = calculator.calculate(LocalDateTime.of(1986,5,30,hour,0));
        assertEquals("甲", chart.dayMaster().name());
        assertEquals(stem, chart.pillarDetails().get(3).stem().name());
        assertEquals(tenGod, chart.pillarDetails().get(3).stem().tenGod());
    }

    @ParameterizedTest
    @CsvSource({"0,壬,劫财", "2,癸,比肩", "4,甲,伤官", "6,乙,食神", "8,丙,正财",
            "10,丁,偏财", "12,戊,正官", "14,己,七杀", "16,庚,正印", "18,辛,偏印"})
    void yinDayMasterAlsoCoversAllTenGodRelations(int hour, String stem, String tenGod) {
        var chart = calculator.calculate(LocalDateTime.of(1986,5,29,hour,0));
        assertEquals("癸", chart.dayMaster().name());
        assertEquals(stem, chart.pillarDetails().get(3).stem().name());
        assertEquals(tenGod, chart.pillarDetails().get(3).stem().tenGod());
    }

    @Test
    void coversAllTwelveBranchHiddenStemListsWithoutNullEntries() {
        Map<String, List<String>> expected = Map.ofEntries(
                Map.entry("子", List.of("癸")), Map.entry("丑", List.of("己","癸","辛")),
                Map.entry("寅", List.of("甲","丙","戊")), Map.entry("卯", List.of("乙")),
                Map.entry("辰", List.of("戊","乙","癸")), Map.entry("巳", List.of("丙","庚","戊")),
                Map.entry("午", List.of("丁","己")), Map.entry("未", List.of("己","丁","乙")),
                Map.entry("申", List.of("庚","壬","戊")), Map.entry("酉", List.of("辛")),
                Map.entry("戌", List.of("戊","辛","丁")), Map.entry("亥", List.of("壬","甲")));
        Map<String, List<String>> actual = new HashMap<>();
        for (int i = 0; i < 12; i++) {
            var chart = calculator.calculate(LocalDateTime.of(2024,1,1,12,0).plusDays(i));
            var day = chart.pillarDetails().get(2);
            actual.put(day.branch().name(), day.hiddenStems().stream().map(s -> s.name()).toList());
            assertEquals(8, chart.elementCounts().values().stream().mapToInt(Integer::intValue).sum());
        }
        assertEquals(expected, actual);
    }

    @Test
    void ziHourRolloverChangesDayMasterAndTenGodReference() {
        var before = calculator.calculate(LocalDateTime.parse("1986-05-29T22:59"));
        var after = calculator.calculate(LocalDateTime.parse("1986-05-29T23:00"));
        assertEquals("癸", before.dayMaster().name());
        assertEquals("正财", before.pillarDetails().getFirst().stem().tenGod());
        assertEquals("甲", after.dayMaster().name());
        assertEquals("食神", after.pillarDetails().getFirst().stem().tenGod());
        assertEquals(after.pillarDetails(), calculator.calculate(LocalDateTime.parse("1986-05-30T00:00")).pillarDetails());
    }

    @Test
    void solarAndLunarRequestsHaveIdenticalEnrichmentAndIncludeZeroCounts() {
        var solar = calculator.calculate(LocalDateTime.parse("2023-03-22T13:37"));
        var lunar = calculator.calculateLunar(2023,2,1,true, LocalTime.of(13,37));
        assertEquals(solar.pillarDetails(), lunar.pillarDetails());
        assertEquals(solar.dayMaster(), lunar.dayMaster());
        assertEquals(solar.elementCounts(), lunar.elementCounts());
        assertEquals(5, solar.elementCounts().size());
        assertEquals(1, solar.elementCounts().get("金"));
        assertEquals(0, solar.elementCounts().get("火"));
    }
}
