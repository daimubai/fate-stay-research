package dev.fatestay.research.bazi;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class BaziFlowTest {
    private final BaziCalculator calculator = new BaziCalculator();
    private final LocalDateTime birth = LocalDateTime.parse("1986-05-29T13:37");

    @Test
    void twelveJieMonthsUseExactTermTimesAndCrossCivilYear() {
        var result = calculator.flowMonths(birth, 2024);
        assertEquals("甲辰", result.yearGanZhi());
        assertEquals("癸", result.dayMaster().name());
        assertEquals(12, result.months().size());
        var first = result.months().getFirst();
        assertEquals("丙寅", first.ganZhi());
        assertEquals("立春", first.term());
        assertEquals("正财", first.stem().tenGod());
        assertEquals(LocalDateTime.parse("2024-02-04T16:27:07"), first.startTime());
        assertEquals(LocalDateTime.parse("2024-03-05T10:22:45"), first.endTime());
        var last = result.months().getLast();
        assertEquals("丁丑", last.ganZhi());
        assertEquals("小寒", last.term());
        assertEquals(2025, last.startTime().getYear());
        assertEquals(2025, last.endTime().getYear());
        assertEquals(calculator.flowMonths(birth, 2025).months().getFirst().startTime(), last.endTime());
        for (int i = 1; i < 12; i++) assertEquals(result.months().get(i-1).endTime(), result.months().get(i).startTime());
        assertThrows(UnsupportedOperationException.class, () -> result.months().clear());
    }

    @Test
    void firstAndLastDailySegmentsAreClippedAtTheExactJie() {
        var result = calculator.flowDays(birth, 2024, 1);
        var first = result.days().getFirst();
        assertEquals(LocalDate.parse("2024-02-04"), first.date());
        assertEquals("戊戌", first.ganZhi());
        assertEquals("正官", first.stem().tenGod());
        assertTrue(first.partial());
        assertEquals(result.month().startTime(), first.startTime());
        assertEquals(LocalDateTime.parse("2024-02-04T23:00"), first.endTime());
        var next = result.days().get(1);
        assertEquals(LocalDate.parse("2024-02-05"), next.date());
        assertEquals("己亥", next.ganZhi());
        assertEquals("七杀", next.stem().tenGod());
        assertFalse(next.partial());
        assertEquals(first.endTime(), next.startTime());
        assertEquals(result.month().endTime(), result.days().getLast().endTime());
        assertTrue(result.days().getLast().partial());
        assertThrows(UnsupportedOperationException.class, () -> result.days().clear());
    }

    @ParameterizedTest
    @ValueSource(ints = {1900, 2023, 2024, 2100, 2210})
    void allMonthsHaveConsecutivePositiveDailyIntervalsAndNoGap(int year) {
        for (int index = 1; index <= 12; index++) {
            var result = calculator.flowDays(birth, year, index);
            long seconds = 0;
            var cursor = result.month().startTime();
            assertTrue(result.days().size() >= 28 && result.days().size() <= 33);
            for (var day : result.days()) {
                assertEquals(cursor, day.startTime());
                assertTrue(day.endTime().isAfter(day.startTime()));
                long duration = Duration.between(day.startTime(), day.endTime()).getSeconds();
                assertTrue(duration <= 86400);
                seconds += duration;
                cursor = day.endTime();
            }
            assertEquals(result.month().endTime(), cursor);
            assertEquals(Duration.between(result.month().startTime(), result.month().endTime()).getSeconds(), seconds);
        }
    }

    @Test
    void leapDayAndLunarNewYearArePresentWithBothDateLabels() {
        var days = calculator.flowDays(birth, 2024, 1).days();
        var leap = days.stream().filter(day -> day.date().equals(LocalDate.parse("2024-02-29"))).findFirst().orElseThrow();
        assertEquals("癸亥", leap.ganZhi());
        assertEquals("比肩", leap.stem().tenGod());
        var newYear = days.stream().filter(day -> day.date().equals(LocalDate.parse("2024-02-10"))).findFirst().orElseThrow();
        assertEquals(2024, newYear.lunarDate().year());
        assertEquals(1, newYear.lunarDate().month());
        assertEquals(1, newYear.lunarDate().day());
        assertEquals(LocalDateTime.parse("2024-02-09T23:00"), newYear.startTime());
    }

    @Test
    void dayMasterReferenceUsesBirthTimeIncludingZiHourRollover() {
        assertEquals("正财", calculator.flowMonths(birth, 2024).months().getFirst().stem().tenGod());
        assertEquals("食神", calculator.flowMonths(LocalDateTime.parse("1986-05-29T23:00"), 2024).months().getFirst().stem().tenGod());
    }

    @Test
    void rejectsOutOfRangeYearMonthAndInvalidBirthPrecision() {
        assertThrows(IllegalArgumentException.class, () -> calculator.flowMonths(birth, 1899));
        assertThrows(IllegalArgumentException.class, () -> calculator.flowMonths(birth, 2211));
        assertThrows(IllegalArgumentException.class, () -> calculator.flowDays(birth, 2024, 0));
        assertThrows(IllegalArgumentException.class, () -> calculator.flowDays(birth, 2024, 13));
        assertThrows(IllegalArgumentException.class, () -> calculator.flowMonths(birth.plusSeconds(1), 2024));
        assertThrows(IllegalArgumentException.class, () -> calculator.flowMonths(null, 2024));
    }
}
