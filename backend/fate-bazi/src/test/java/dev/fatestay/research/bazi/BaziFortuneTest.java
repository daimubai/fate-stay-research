package dev.fatestay.research.bazi;

import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static dev.fatestay.research.bazi.BaziCalculator.BirthGender.*;
import static org.junit.jupiter.api.Assertions.*;

class BaziFortuneTest {
    private final BaziCalculator calculator = new BaziCalculator();

    // Upstream EightCharTest child-limit fixtures (default provider).
    @ParameterizedTest
    @CsvSource({
        "2022-03-09T20:51, MALE, 2030-12-12T07:19, 8,9,2,10,28",
        "2018-06-11T09:30, FEMALE, 2020-03-21T11:12, 1,9,10,1,42",
        "1983-02-15T20:00, FEMALE, 1989-05-04T18:24, 6,2,18,22,24",
        "1992-02-02T12:00, MALE, 2001-02-11T18:58, 9,0,9,6,58"
    })
    void knownStartTimeAndIntervals(String birth, BaziCalculator.BirthGender gender, String start,
                                    int years, int months, int days, int hours, int minutes) {
        var fortune = calculator.calculate(LocalDateTime.parse(birth), gender).fortune();
        assertEquals(LocalDateTime.parse(start), fortune.startTime());
        assertEquals(years, fortune.startInterval().years());
        assertEquals(months, fortune.startInterval().months());
        assertEquals(days, fortune.startInterval().days());
        assertEquals(hours, fortune.startInterval().hours());
        assertEquals(minutes, fortune.startInterval().minutes());
    }

    @ParameterizedTest
    @CsvSource({"1986-05-29T13:37,MALE,FORWARD", "1986-05-29T13:37,FEMALE,REVERSE",
        "1983-02-15T20:00,MALE,REVERSE", "1983-02-15T20:00,FEMALE,FORWARD"})
    void yearYinYangAndGenderDetermineDirection(String birth, BaziCalculator.BirthGender gender, String direction) {
        assertEquals(direction, calculator.calculate(LocalDateTime.parse(birth), gender).fortune().direction());
    }

    @Test
    void forwardDecadesAndAnnualYearsMatchKnownFixture() {
        var chart = calculator.calculate(LocalDateTime.parse("1983-02-15T20:00"), FEMALE);
        var first = chart.fortune().decades().getFirst();
        assertEquals("乙卯", first.ganZhi());
        assertEquals("丙辰", chart.fortune().decades().get(1).ganZhi());
        assertEquals(1989, first.startYear());
        assertEquals(1998, first.endYear());
        assertEquals(7, first.startAge());
        assertEquals(16, first.endAge());
        assertEquals("己巳", first.annualYears().getFirst().ganZhi()); // annual year, not minor-fortune 辛巳
        assertEquals("正财", first.annualYears().getFirst().stem().tenGod());
        assertEquals("七杀", first.annualYears().get(1).stem().tenGod());
    }

    @Test
    void reverseDecadesStillListAnnualYearsChronologically() {
        var chart = calculator.calculate(LocalDateTime.parse("1992-02-02T12:00"), MALE);
        var first = chart.fortune().decades().getFirst();
        assertEquals("庚子", first.ganZhi());
        assertEquals("己亥", chart.fortune().decades().get(1).ganZhi());
        assertEquals(2001, first.startYear());
        assertEquals(10, first.startAge());
        assertEquals("辛巳", first.annualYears().getFirst().ganZhi());
        assertEquals("壬午", first.annualYears().get(1).ganZhi());
    }

    @Test
    void allTenDecadesContainTenConsecutiveAnnualYearsAndImmutableLists() {
        for (var gender : BaziCalculator.BirthGender.values()) {
            var timeline = calculator.calculate(LocalDateTime.parse("1986-05-29T23:00"), gender).fortune();
            assertEquals(10, timeline.decades().size());
            for (int i = 0; i < 10; i++) {
                var decade = timeline.decades().get(i);
                assertEquals(i + 1, decade.index());
                assertEquals(timeline.decades().getFirst().startYear() + i * 10, decade.startYear());
                assertEquals(decade.startYear() + 9, decade.endYear());
                assertEquals(decade.startAge() + 9, decade.endAge());
                assertEquals(10, decade.annualYears().size());
                for (int j = 0; j < 10; j++) {
                    assertEquals(decade.startYear() + j, decade.annualYears().get(j).year());
                    assertEquals(decade.startAge() + j, decade.annualYears().get(j).age());
                }
                assertThrows(UnsupportedOperationException.class, () -> decade.annualYears().clear());
            }
            assertThrows(UnsupportedOperationException.class, () -> timeline.decades().clear());
        }
    }

    @Test
    void genderDoesNotChangeFourPillarsAndLunarInputIsEquivalent() {
        var birth = LocalDateTime.parse("2023-03-22T13:37");
        var male = calculator.calculate(birth, MALE);
        var female = calculator.calculate(birth, FEMALE);
        assertEquals(male.pillarDetails(), female.pillarDetails());
        assertNotEquals(male.fortune().decades().getFirst().ganZhi(), female.fortune().decades().getFirst().ganZhi());
        assertEquals(male.fortune(), calculator.calculateLunar(2023,2,1,true,LocalTime.of(13,37),MALE).fortune());
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(birth, null));
    }

    @Test
    void liChunChangesYearStemAndDirectionAtTheBoundary() {
        assertEquals("REVERSE", calculator.calculate(LocalDateTime.parse("2024-02-04T16:27"), MALE).fortune().direction());
        assertEquals("FORWARD", calculator.calculate(LocalDateTime.parse("2024-02-04T16:28"), MALE).fortune().direction());
        assertNotNull(calculator.calculate(LocalDateTime.parse("2100-12-31T23:59"), FEMALE).fortune());
    }
}
