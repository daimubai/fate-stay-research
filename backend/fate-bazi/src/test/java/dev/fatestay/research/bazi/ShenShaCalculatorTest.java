package dev.fatestay.research.bazi;

import dev.fatestay.research.calendar.TymeCalendar;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class ShenShaCalculatorTest {
    private final BaziCalculator calculator = new BaziCalculator();
    @Test void knownChartHasNativeTerrainsAndTraceableHits() {
        var chart = calculator.calculate(LocalDateTime.of(1986, 5, 29, 13, 37));
        assertEquals(List.of("沐浴", "胎", "病", "墓"), chart.pillarDetails().stream().map(TymeCalendar.PillarDetail::terrain).toList());
        assertEquals(List.of("长生", "胎", "病", "冠带"), chart.pillarDetails().stream().map(TymeCalendar.PillarDetail::selfTerrain).toList());
        assertEquals("PROJECT_RULES_ON_TYME4J", chart.shenSha().provider());
        assertEquals(7, chart.shenSha().rules().size());
        assertEquals(List.of("TIAN_YI:month", "JIANG_XING:day"), chart.shenSha().hits().stream().map(h -> h.id()+":"+h.pillarKey()).toList());
        assertEquals("癸", chart.shenSha().hits().getFirst().bases().getFirst().sourceValue());
        assertEquals(List.of("卯", "巳"), chart.shenSha().hits().getFirst().bases().getFirst().targetValues());
        assertThrows(UnsupportedOperationException.class, () -> chart.shenSha().hits().clear());
    }
    @ParameterizedTest @CsvSource({"甲,丑未,寅", "乙,子申,卯", "丙,亥酉,巳", "丁,亥酉,午", "戊,丑未,巳", "己,子申,午", "庚,丑未,申", "辛,寅午,酉", "壬,卯巳,亥", "癸,卯巳,子"})
    void allTenDayStemsHaveExplicitNobleAndNativeLu(String stem, String noble, String lu) {
        assertEquals(noble, ShenShaCalculator.noble(stem));
        var calendar = new TymeCalendar();
        var start = LocalDateTime.of(2024, 1, 1, 12, 0);
        var analysis = java.util.stream.IntStream.range(0, 10).mapToObj(i -> calendar.analyze(start.plusDays(i)))
            .filter(a -> a.dayMaster().name().equals(stem)).findFirst().orElseThrow();
        assertEquals(lu, analysis.dayLuBranch());
    }
    @ParameterizedTest @CsvSource({"申,寅酉辰子", "子,寅酉辰子", "辰,寅酉辰子", "寅,申卯戌午", "午,申卯戌午", "戌,申卯戌午", "亥,巳子未卯", "卯,巳子未卯", "未,巳子未卯", "巳,亥午丑酉", "酉,亥午丑酉", "丑,亥午丑酉"})
    void allTrineReferencesHaveFourCorrectTargets(String branch, String expected) {
        for (int i=0;i<4;i++) assertEquals(expected.substring(i,i+1), ShenShaCalculator.groupTarget(branch,i));
    }
    @Test void mergesYearAndDayEvidenceAndAllowsReferencePillar() {
        var a = new TymeCalendar().analyze(LocalDateTime.of(1990,3,15,10,30)); // 年午、日卯
        // Synthetic year 未 and day 卯 share the 亥卯未 trine; month 巳 and hour 子 exercise other matches.
        var details = List.of(p("year","未"),p("month","巳"),p("day","卯"),p("hour","子"));
        var synthetic = new TymeCalendar.Analysis(a.pillars(), a.dayMaster(),details,a.dayLuBranch(),List.of("巳","午"));
        var result = ShenShaCalculator.calculate(synthetic);
        var self = result.hits().stream().filter(h -> h.id().equals("JIANG_XING") && h.pillarKey().equals("day")).findFirst().orElseThrow();
        // Year 未 and day 卯 belong to the same trine: one tag with both references.
        assertEquals(2,self.bases().size());
        assertEquals(List.of("年支","日支"),self.bases().stream().map(ShenShaCalculator.Basis::reference).toList());
        assertTrue(result.hits().stream().anyMatch(h -> h.id().equals("XUN_KONG") && h.targetBranch().equals("巳")));
        assertTrue(result.hits().stream().anyMatch(h -> h.id().equals("HUA_GAI") && h.pillarKey().equals("year")));
        assertTrue(result.hits().stream().anyMatch(h -> h.id().equals("TAO_HUA") && h.pillarKey().equals("hour")));
        assertTrue(result.hits().stream().anyMatch(h -> h.id().equals("YI_MA") && h.pillarKey().equals("month")));
    }
    private TymeCalendar.PillarDetail p(String key,String branch) {
        return new TymeCalendar.PillarDetail(key,key,"甲"+branch,new TymeCalendar.Stem("甲","木","比肩"),new TymeCalendar.Branch(branch,"木"),List.of(),"测试","长生","长生");
    }
    @Test void solarLunarAndGenderShareNatalTraitsAndZiBoundaryChangesThem() {
        var solar = calculator.calculate(LocalDateTime.of(1986,5,29,13,37), BaziCalculator.BirthGender.MALE);
        var lunar = calculator.calculateLunar(1986,4,21,false,java.time.LocalTime.of(13,37),BaziCalculator.BirthGender.FEMALE);
        assertEquals(solar.shenSha(),lunar.shenSha());
        assertEquals(solar.pillarDetails(),lunar.pillarDetails());
        var before = new TymeCalendar().analyze(LocalDateTime.of(1986,5,29,22,59));
        var after = new TymeCalendar().analyze(LocalDateTime.of(1986,5,29,23,0));
        assertEquals(List.of("戌","亥"),before.dayVoidBranches());
        assertEquals(List.of("申","酉"),after.dayVoidBranches());
        assertNotEquals(before.pillarDetails().getFirst().terrain(),after.pillarDetails().getFirst().terrain());
    }
    @Test void emptyMatchesAndNativeLuAreHandledWithoutInventedTags() {
        var a = new TymeCalendar().analyze(LocalDateTime.of(1986,5,29,13,37));
        var onlyYin = List.of(p("year","寅"),p("month","寅"),p("day","寅"),p("hour","寅"));
        var empty = ShenShaCalculator.calculate(new TymeCalendar.Analysis(a.pillars(),a.dayMaster(),onlyYin,a.dayLuBranch(),a.dayVoidBranches()));
        assertTrue(empty.hits().isEmpty());
        var onlyZi = List.of(p("year","子"),p("month","子"),p("day","子"),p("hour","子"));
        var lu = ShenShaCalculator.calculate(new TymeCalendar.Analysis(a.pillars(),a.dayMaster(),onlyZi,a.dayLuBranch(),a.dayVoidBranches()));
        assertEquals(4,lu.hits().stream().filter(h -> h.id().equals("LU_SHEN")).count());
    }
    @Test void terrainCoversTenStemsTwelveBranchesAndBothDirections() {
        String stems = "甲乙丙丁戊己庚辛壬癸";
        // Traditional 长生 branch for each stem, 子=0. Earth follows fire.
        int[] starts = {11,6,2,9,2,9,5,0,8,3};
        var stages = List.of("长生","沐浴","冠带","临官","帝旺","衰","病","死","墓","绝","胎","养");
        var calendar = new TymeCalendar();
        var start = LocalDateTime.of(2024,1,1,0,0);
        for(int day=0;day<10;day++) {
            var base = calendar.analyze(start.plusDays(day));
            int stem = stems.indexOf(base.dayMaster().name());
            for(int branch=0;branch<12;branch++) {
                var hour = calendar.analyze(start.plusDays(day).plusHours(branch*2)).pillarDetails().get(3);
                assertEquals(stages.get(Math.floorMod((branch-starts[stem])*(stem%2==0?1:-1),12)),hour.terrain(),base.dayMaster().name()+hour.branch().name());
            }
        }
    }
    @Test void sixXunVoidPairsRepeatForAllSixtyDays() {
        var start = LocalDateTime.of(2024,1,1,12,0);
        var pairs = new java.util.HashSet<List<String>>();
        for(int i=0;i<60;i++) {
            var a = new TymeCalendar().analyze(start.plusDays(i));
            assertEquals(2,a.dayVoidBranches().size());
            assertFalse(a.dayVoidBranches().contains(a.pillarDetails().get(2).branch().name()));
            pairs.add(a.dayVoidBranches());
        }
        assertEquals(java.util.Set.of(List.of("戌","亥"),List.of("申","酉"),List.of("午","未"),List.of("辰","巳"),List.of("寅","卯"),List.of("子","丑")),pairs);
    }
}
