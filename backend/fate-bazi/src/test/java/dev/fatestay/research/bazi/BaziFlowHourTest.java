package dev.fatestay.research.bazi;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class BaziFlowHourTest {
    private final BaziCalculator calculator = new BaziCalculator();
    private final LocalDateTime birth = LocalDateTime.parse("1986-05-29T13:37");

    @Test
    void twelveHoursOfGuiDayStartWithPreviousNightZiAndContainExpectedAttributes() {
        var result = calculator.flowHours(birth, 2024, 1, LocalDate.parse("2024-02-29"));
        assertEquals(12, result.hours().size());
        assertEquals(List.of("壬子","癸丑","甲寅","乙卯","丙辰","丁巳","戊午","己未","庚申","辛酉","壬戌","癸亥"),
                result.hours().stream().map(hour -> hour.ganZhi()).toList());
        var zi = result.hours().getFirst();
        assertEquals("子时", zi.name());
        assertEquals(LocalDateTime.parse("2024-02-28T23:00"), zi.startTime());
        assertEquals(LocalDateTime.parse("2024-02-29T01:00"), zi.endTime());
        assertEquals("劫财", zi.stem().tenGod());
        assertEquals("水", zi.stem().element());
        assertEquals("水", zi.branch().element());
        assertEquals("桑柘木", zi.naYin());
        assertEquals("比肩", result.hours().get(1).stem().tenGod());
        assertEquals(LocalDateTime.parse("2024-02-29T23:00"), result.hours().getLast().endTime());
        assertTrue(result.hours().stream().allMatch(hour -> hour.included() && !hour.partial()));
        assertThrows(UnsupportedOperationException.class, () -> result.hours().clear());
    }

    @Test
    void liChunPartialDayKeepsTwelveReferenceHoursButClipsOnlyIntersectingOnes() {
        var result = calculator.flowHours(birth, 2024, 1, LocalDate.parse("2024-02-04"));
        assertEquals(12, result.hours().size());
        assertEquals(4, result.hours().stream().filter(hour -> hour.included()).count());
        var zi = result.hours().getFirst();
        assertFalse(zi.included());
        assertNull(zi.startTime());
        assertNull(zi.endTime());
        var shen = result.hours().get(8);
        assertEquals("申时", shen.name());
        assertTrue(shen.partial());
        assertEquals(LocalDateTime.parse("2024-02-04T16:27:07"), shen.startTime());
        assertEquals(LocalDateTime.parse("2024-02-04T17:00"), shen.endTime());
    }

    @Test
    void adjacentMonthsSplitTheSameHourWithoutOverlapAtJingZhe() {
        var date = LocalDate.parse("2024-03-05");
        var before = calculator.flowHours(birth, 2024, 1, date).hours().get(5);
        var after = calculator.flowHours(birth, 2024, 2, date).hours().get(5);
        assertEquals("巳时", before.name());
        assertEquals(before.ganZhi(), after.ganZhi());
        assertTrue(before.partial()); assertTrue(after.partial());
        assertEquals(LocalDateTime.parse("2024-03-05T10:22:45"), before.endTime());
        assertEquals(before.endTime(), after.startTime());
        assertEquals(7200, Duration.between(before.startTime(), before.endTime()).getSeconds()
                + Duration.between(after.startTime(), after.endTime()).getSeconds());
    }

    @ParameterizedTest
    @ValueSource(ints = {1900,2023,2024,2100,2210})
    void sampledMonthEdgesAndNormalDaysAreExactlyCoveredByIncludedHours(int year) {
        for (int month : new int[]{1,6,12}) {
            var days = calculator.flowDays(birth, year, month).days();
            for (var day : List.of(days.getFirst(), days.get(1), days.getLast())) {
                var result = calculator.flowHours(birth, year, month, day.date());
                var cursor = day.startTime();
                long seconds = 0;
                for (int i = 0; i < 12; i++) {
                    var hour = result.hours().get(i);
                    assertEquals(i + 1, hour.index());
                    assertEquals(7200, Duration.between(hour.nominalStartTime(), hour.nominalEndTime()).getSeconds());
                    if (i > 0) assertEquals(result.hours().get(i-1).nominalEndTime(), hour.nominalStartTime());
                    if (hour.included()) {
                        assertEquals(cursor, hour.startTime());
                        assertTrue(hour.endTime().isAfter(hour.startTime()));
                        seconds += Duration.between(hour.startTime(), hour.endTime()).getSeconds();
                        cursor = hour.endTime();
                    } else { assertNull(hour.startTime()); assertNull(hour.endTime()); }
                }
                assertEquals(day.endTime(), cursor);
                assertEquals(Duration.between(day.startTime(), day.endTime()).getSeconds(), seconds);
            }
        }
    }

    @Test
    void tenGodsReferenceBirthDayNotTheSelectedDayAndRespectBirthZiRollover() {
        var date = LocalDate.parse("2024-02-05"); // 己 day, 子 hour is 甲子
        var normal = calculator.flowHours(birth, 2024, 1, date);
        var lateBirth = calculator.flowHours(LocalDateTime.parse("1986-05-29T23:00"), 2024, 1, date);
        assertEquals("甲子", normal.hours().getFirst().ganZhi());
        assertEquals("伤官", normal.hours().getFirst().stem().tenGod());
        assertEquals("比肩", lateBirth.hours().getFirst().stem().tenGod());
        assertEquals(normal.hours().getFirst().ganZhi(), lateBirth.hours().getFirst().ganZhi());
    }

    @Test
    void rejectsDatesOutsideSelectedMonthAndInvalidParameters() {
        assertThrows(IllegalArgumentException.class, () -> calculator.flowHours(birth,2024,1,LocalDate.parse("2024-01-01")));
        assertThrows(IllegalArgumentException.class, () -> calculator.flowHours(birth,2024,0,LocalDate.parse("2024-02-29")));
        assertThrows(IllegalArgumentException.class, () -> calculator.flowHours(birth,2211,1,LocalDate.parse("2211-02-20")));
        assertThrows(IllegalArgumentException.class, () -> calculator.flowHours(birth,2024,1,null));
        assertThrows(IllegalArgumentException.class, () -> calculator.flowHours(birth.plusSeconds(1),2024,1,LocalDate.parse("2024-02-29")));
    }
}
