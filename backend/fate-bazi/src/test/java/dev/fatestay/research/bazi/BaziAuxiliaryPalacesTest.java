package dev.fatestay.research.bazi;

import dev.fatestay.research.calendar.TymeCalendar.AuxiliaryPalaces;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class BaziAuxiliaryPalacesTest {
    private final BaziCalculator calculator = new BaziCalculator();
    private AuxiliaryPalaces palaces(String time) { return calculator.calculate(LocalDateTime.parse(time)).auxiliaryPalaces(); }
    private void assertCycles(AuxiliaryPalaces p, String fetal, String own, String body) {
        assertAll(() -> assertEquals(fetal, p.fetalOrigin().ganZhi()),
            () -> assertEquals(own, p.ownSign().ganZhi()), () -> assertEquals(body, p.bodySign().ganZhi()));
    }

    // Published upstream EightCharTest fixtures (tests 20, 26, 27, 49).
    @ParameterizedTest @CsvSource({
        "1986-05-29T13:37,甲申,癸巳,辛丑",
        "1994-12-06T02:00,丙寅,己巳,丁丑",
        "1980-06-15T12:30,癸酉,辛巳,己丑",
        "2024-01-29T09:33,丙辰,癸亥,己未"
    })
    void matchesUpstreamBirthFixtures(String time, String fetal, String own, String body) {
        var p = palaces(time);
        assertEquals("tyme-eight-char-palaces-v1", p.ruleVersion());
        assertCycles(p, fetal, own, body);
    }

    @ParameterizedTest @CsvSource({
        "2019-03-07T08:00,甲戌", "2019-03-27T02:00,丁丑"
    })
    void ownSignMatchesUpstreamRegressionFixtures(String time, String own) {
        assertEquals(own, palaces(time).ownSign().ganZhi());
    }
    @Test void bodySignMatchesLunarNewYearRegressionFixture() {
        assertEquals("丙寅", palaces("1990-01-27T00:00").bodySign().ganZhi());
    }
    @Test void derivedAttributesUseBirthDayMasterAndDoNotChangeEightCharacterCounts() {
        var chart = calculator.calculate(LocalDateTime.of(1986,5,29,13,37));
        var p = chart.auxiliaryPalaces();
        assertEquals("甲",p.fetalOrigin().stem().name());
        assertEquals("木",p.fetalOrigin().stem().element());
        assertEquals("伤官",p.fetalOrigin().stem().tenGod());
        assertEquals("金",p.fetalOrigin().branch().element());
        assertEquals("泉中水",p.fetalOrigin().naYin());
        assertEquals("比肩",p.ownSign().stem().tenGod());
        assertEquals("长流水",p.ownSign().naYin());
        assertEquals("偏印",p.bodySign().stem().tenGod());
        assertEquals("土",p.bodySign().branch().element());
        assertEquals("壁上土",p.bodySign().naYin());
        assertEquals(4,chart.pillarDetails().size());
        assertEquals(java.util.Map.of("木",1,"火",2,"土",2,"金",1,"水",2),chart.elementCounts());
    }
    @Test void usesSolarTermMonthRatherThanLunarMonth() {
        assertCycles(palaces("2024-03-05T10:22"),"丁巳","甲戌","壬申");
        assertCycles(palaces("2024-03-05T10:23"),"戊午","癸酉","癸酉");
    }
    @Test void usesLiChunYearRatherThanCivilOrLunarNewYear() {
        assertCycles(palaces("2024-02-04T16:27"),"丙辰","庚申","壬戌");
        assertCycles(palaces("2024-02-04T16:28"),"丁巳","辛未","乙亥");
    }
    @ParameterizedTest @CsvSource({
        "00:00,庚子,甲午", "02:00,己亥,乙未", "04:00,戊戌,丙申", "06:00,丁酉,丁酉",
        "08:00,丙申,戊戌", "10:00,乙未,己亥", "12:00,甲午,庚子", "14:00,癸巳,辛丑",
        "16:00,壬辰,庚寅", "18:00,辛卯,辛卯", "20:00,庚寅,壬辰", "22:00,辛丑,癸巳"
    })
    void handlesAllTwelveHourBranchesIncludingCycleWrap(String time, String own, String body) {
        assertCycles(palaces("1986-05-29T"+time),"甲申",own,body);
    }
    @Test void respectsHourBoundariesAndZiDayMasterRollover() {
        assertEquals(palaces("1986-05-29T13:00"),palaces("1986-05-29T14:59"));
        assertCycles(palaces("1986-05-29T15:00"),"甲申","壬辰","庚寅");
        var before = palaces("1986-05-29T22:59");
        var after = palaces("1986-05-29T23:00");
        assertCycles(before,"甲申","辛丑","癸巳");
        assertCycles(after,"甲申","庚子","甲午");
        assertEquals("伤官",before.fetalOrigin().stem().tenGod());
        assertEquals("比肩",after.fetalOrigin().stem().tenGod());
        assertEquals(after,palaces("1986-05-30T00:00"));
        assertEquals(palaces("1986-05-29T00:59").fetalOrigin(),palaces("1986-05-29T01:00").fetalOrigin());
        assertNotEquals(palaces("1986-05-29T00:59").ownSign(),palaces("1986-05-29T01:00").ownSign());
    }
    @Test void equivalentCalendarsAndGendersShareResultsIncludingLeapMonth() {
        var time = LocalDateTime.of(1986,5,29,13,37);
        var male = calculator.calculate(time,BaziCalculator.BirthGender.MALE).auxiliaryPalaces();
        assertEquals(male,calculator.calculate(time,BaziCalculator.BirthGender.FEMALE).auxiliaryPalaces());
        assertEquals(male,calculator.calculateLunar(1986,4,21,false,LocalTime.of(13,37)).auxiliaryPalaces());
        assertEquals(palaces("2023-03-22T12:00"),calculator.calculateLunar(2023,2,1,true,LocalTime.NOON).auxiliaryPalaces());
    }
    @ParameterizedTest @CsvSource({"1900-01-01T00:00", "2100-12-31T23:59"})
    void supportsBothBirthRangeExtremes(String time) {
        var p = palaces(time);
        for (var item : List.of(p.fetalOrigin(),p.ownSign(),p.bodySign())) {
            assertEquals(2,item.ganZhi().length());
            assertEquals(item.stem().name()+item.branch().name(),item.ganZhi());
            assertFalse(item.naYin().isBlank());
        }
    }
}
