package dev.fatestay.research.bazi;

import java.time.LocalDateTime;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class BaziCalculatorTest {
    private final BaziCalculator calculator = new BaziCalculator();

    // First three fixtures match upstream 6tail/tyme4j EightCharTest examples.
    @ParameterizedTest
    @CsvSource({
        "1986-05-29T13:37, 丙寅, 癸巳, 癸酉, 己未",
        "1994-12-06T02:00, 甲戌, 乙亥, 丙寅, 己丑",
        "1990-03-15T10:30, 庚午, 己卯, 己卯, 己巳",
        "2000-02-29T12:00, 庚辰, 戊寅, 丁巳, 丙午",
        // 2024 Li Chun is at 16:27:07; Jing Zhe at 10:22:45 (UTC+08).
        "2024-02-04T16:27, 癸卯, 乙丑, 戊戌, 庚申",
        "2024-02-04T16:28, 甲辰, 丙寅, 戊戌, 庚申",
        "2024-03-05T10:22, 甲辰, 丙寅, 戊辰, 丁巳",
        "2024-03-05T10:23, 甲辰, 丁卯, 戊辰, 丁巳",
        "1986-05-29T22:59, 丙寅, 癸巳, 癸酉, 癸亥",
        "1986-05-29T23:00, 丙寅, 癸巳, 甲戌, 甲子",
        "1986-05-30T00:00, 丙寅, 癸巳, 甲戌, 甲子"
    })
    void calculatesKnownAndBoundaryCharts(String input, String year, String month, String day, String hour) {
        var chart = calculator.calculate(LocalDateTime.parse(input));
        assertAll(() -> assertEquals(year, chart.pillars().year()),
                () -> assertEquals(month, chart.pillars().month()),
                () -> assertEquals(day, chart.pillars().day()),
                () -> assertEquals(hour, chart.pillars().hour()));
    }

    @Test
    void preservesInputAndCalculationPolicy() {
        var birth = LocalDateTime.of(1986, 5, 29, 13, 37);
        var chart = calculator.calculate(birth);
        assertEquals(birth, chart.birthDateTime());
        assertEquals("UTC+08:00", chart.timeZone());
        assertEquals("MINUTE", chart.precision());
        assertEquals("tyme4j-1.5.2", chart.engineVersion());
        assertEquals("lichun-jie-zi23-v1", chart.ruleVersion());
        IntStream.range(0, 50).parallel().forEach(i -> assertEquals(chart, calculator.calculate(birth)));
    }

    @Test
    void validatesSupportedRangeAndPrecision() {
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(null));
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(LocalDateTime.of(1899,12,31,23,59)));
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(LocalDateTime.of(2101,1,1,0,0)));
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(LocalDateTime.of(2000,1,1,0,0,1)));
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(LocalDateTime.of(2000,1,1,0,0,0,1)));
        assertDoesNotThrow(() -> calculator.calculate(LocalDateTime.of(1900,1,1,0,0)));
        assertDoesNotThrow(() -> calculator.calculate(LocalDateTime.of(2100,12,31,23,59)));
    }

    @ParameterizedTest
    @CsvSource({
        "2024-02-10, 2024, 1, 1, false",
        "2023-03-22, 2023, 2, 1, true",
        "2023-02-20, 2023, 2, 1, false",
        "2020-05-23, 2020, 4, 1, true",
        "2024-02-09, 2023, 12, 30, false"
    })
    void solarAndLunarInputsGiveSameChart(String solarDate, int year, int month, int day, boolean leap) {
        var solar = calculator.calculate(LocalDateTime.parse(solarDate + "T13:37"));
        var lunar = calculator.calculateLunar(year, month, day, leap, java.time.LocalTime.of(13,37));
        assertEquals(solar.pillars(), lunar.pillars());
        assertEquals(solar.birthDateTime(), lunar.birthDateTime());
        assertEquals(solar.solarDate(), lunar.solarDate());
        assertEquals(solar.lunarDate(), lunar.lunarDate());
        assertEquals(year, solar.lunarDate().year());
        assertEquals(month, solar.lunarDate().month());
        assertEquals(day, solar.lunarDate().day());
        assertEquals(leap, solar.lunarDate().leapMonth());
        assertEquals("LUNAR", lunar.inputCalendar());
        assertEquals("SOLAR", solar.inputCalendar());
        assertEquals(leap, lunar.lunarDate().display().contains("闰"));
    }

    @Test
    void rejectsInvalidLunarDatesAndNonexistentLeapMonth() {
        var time = java.time.LocalTime.NOON;
        assertThrows(IllegalArgumentException.class, () -> calculator.calculateLunar(2024,2,1,true,time));
        assertThrows(IllegalArgumentException.class, () -> calculator.calculateLunar(2023,3,1,true,time));
        assertThrows(IllegalArgumentException.class, () -> calculator.calculateLunar(2023,2,30,true,time));
        assertThrows(IllegalArgumentException.class, () -> calculator.calculateLunar(2024,0,1,false,time));
        assertThrows(IllegalArgumentException.class, () -> calculator.calculateLunar(2024,-2,1,false,time));
        assertThrows(IllegalArgumentException.class, () -> calculator.calculateLunar(2024,1,0,false,time));
        assertThrows(IllegalArgumentException.class, () -> calculator.calculateLunar(2100,12,29,false,time));
        assertThrows(IllegalArgumentException.class, () -> calculator.calculateLunar(2024,1,1,false,null));
    }

    @Test
    void calendarDatesUseMidnightEvenWhenPillarDayChangesAt23() {
        var chart = calculator.calculate(LocalDateTime.parse("2024-02-09T23:00"));
        assertEquals("2024-02-09", chart.solarDate().toString());
        assertEquals(2023, chart.lunarDate().year());
        assertEquals(30, chart.lunarDate().day());
        assertEquals(chart.pillars().day(), calculator.calculate(LocalDateTime.parse("2024-02-10T00:00")).pillars().day());
    }

    @Test
    void roundTripsBothSupportedSolarRangeEdges() {
        for (String date : new String[]{"1900-01-01T00:00", "2100-12-31T23:59"}) {
            var original = calculator.calculate(LocalDateTime.parse(date));
            var lunar = original.lunarDate();
            var converted = calculator.calculateLunar(lunar.year(), lunar.month(), lunar.day(), lunar.leapMonth(),
                    original.birthDateTime().toLocalTime());
            assertEquals(original.birthDateTime(), converted.birthDateTime());
            assertEquals(original.pillars(), converted.pillars());
        }
    }
}
