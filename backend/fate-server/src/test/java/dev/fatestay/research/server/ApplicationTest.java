package dev.fatestay.research.server;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import org.springframework.http.MediaType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ApplicationTest {
    @Autowired
    MockMvc mvc;

    @Test
    void healthIsPublicAndReturnsApplicationIdentity() throws Exception {
        mvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.application").value("fate-stay-research"));
    }

    @Test
    void actuatorHealthIsPublic() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void otherRoutesAreNotPublic() throws Exception {
        mvc.perform(get("/api/v1/private")).andExpect(status().isForbidden());
    }

    @Test
    void calculatesChartWithoutAuthenticationOrCsrfToken() throws Exception {
        mvc.perform(post("/api/v1/bazi/charts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"gender\":\"MALE\",\"birthDate\":\"1986-05-29\",\"birthTime\":\"13:37\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pillars.year").value("丙寅"))
                .andExpect(jsonPath("$.pillars.month").value("癸巳"))
                .andExpect(jsonPath("$.pillars.day").value("癸酉"))
                .andExpect(jsonPath("$.pillars.hour").value("己未"))
                .andExpect(jsonPath("$.birthDateTime").value("1986-05-29T13:37:00"))
                .andExpect(jsonPath("$.timeZone").value("UTC+08:00"))
                .andExpect(jsonPath("$.solarDate").value("1986-05-29"))
                .andExpect(jsonPath("$.lunarDate.display").value("1986年四月廿一"))
                .andExpect(jsonPath("$.dayMaster.name").value("癸"))
                .andExpect(jsonPath("$.dayMaster.element").value("水"))
                .andExpect(jsonPath("$.pillarDetails.length()").value(4))
                .andExpect(jsonPath("$.pillarDetails[0].key").value("year"))
                .andExpect(jsonPath("$.pillarDetails[0].stem.tenGod").value("正财"))
                .andExpect(jsonPath("$.pillarDetails[0].stem.element").value("火"))
                .andExpect(jsonPath("$.pillarDetails[0].branch.element").value("木"))
                .andExpect(jsonPath("$.pillarDetails[0].hiddenStems[0].name").value("甲"))
                .andExpect(jsonPath("$.pillarDetails[0].hiddenStems[0].qi").value("本气"))
                .andExpect(jsonPath("$.pillarDetails[0].hiddenStems[0].tenGod").value("伤官"))
                .andExpect(jsonPath("$.pillarDetails[0].naYin").value("炉中火"))
                .andExpect(jsonPath("$.pillarDetails[2].stem.tenGod").value("比肩"))
                .andExpect(jsonPath("$.pillarDetails[2].hiddenStems.length()").value(1))
                .andExpect(jsonPath("$.elementCounts.木").value(1))
                .andExpect(jsonPath("$.elementCounts.火").value(2))
                .andExpect(jsonPath("$.elementCounts.土").value(2))
                .andExpect(jsonPath("$.elementCounts.金").value(1))
                .andExpect(jsonPath("$.elementCounts.水").value(2));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{}", "null", "{broken",
        "{\"gender\":\"MALE\",\"birthDate\":\"2001-02-29\",\"birthTime\":\"13:37\"}",
        "{\"gender\":\"MALE\",\"birthDate\":\"1986-05-29\",\"birthTime\":\"24:00\"}",
        "{\"gender\":\"MALE\",\"birthDate\":\"1899-05-29\",\"birthTime\":\"13:37\"}",
        "{\"gender\":\"MALE\",\"birthDate\":\"1986-05-29\",\"birthTime\":\"13:37:01\"}"
    })
    void rejectsInvalidBirthInput(String input) throws Exception {
        mvc.perform(post("/api/v1/bazi/charts").contentType(MediaType.APPLICATION_JSON).content(input))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void acceptsLunarLeapMonthAndReturnsBothCalendars() throws Exception {
        mvc.perform(post("/api/v1/bazi/charts").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"gender":"MALE","calendarType":"LUNAR","lunarDate":{"year":2023,"month":2,"day":1,"leapMonth":true},"birthTime":"13:37"}
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solarDate").value("2023-03-22"))
                .andExpect(jsonPath("$.lunarDate.year").value(2023))
                .andExpect(jsonPath("$.lunarDate.month").value(2))
                .andExpect(jsonPath("$.lunarDate.day").value(1))
                .andExpect(jsonPath("$.lunarDate.leapMonth").value(true))
                .andExpect(jsonPath("$.lunarDate.display").value("2023年闰二月初一"))
                .andExpect(jsonPath("$.inputCalendar").value("LUNAR"))
                .andExpect(jsonPath("$.dayMaster.name").value("己"))
                .andExpect(jsonPath("$.pillarDetails[0].naYin").value("金箔金"))
                .andExpect(jsonPath("$.elementCounts.火").value(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"gender\":\"MALE\",\"calendarType\":\"UNKNOWN\",\"birthTime\":\"13:37\"}",
        "{\"gender\":\"MALE\",\"calendarType\":\"LUNAR\",\"birthTime\":\"13:37\"}",
        "{\"gender\":\"MALE\",\"calendarType\":\"LUNAR\",\"lunarDate\":{\"year\":2023,\"month\":2,\"day\":1},\"birthTime\":\"13:37\"}",
        "{\"gender\":\"MALE\",\"calendarType\":\"LUNAR\",\"lunarDate\":{\"year\":2024,\"month\":2,\"day\":1,\"leapMonth\":true},\"birthTime\":\"13:37\"}",
        "{\"gender\":\"MALE\",\"calendarType\":\"SOLAR\",\"birthDate\":\"2024-02-10\",\"lunarDate\":{},\"birthTime\":\"13:37\"}"
    })
    void rejectsInvalidCalendarRequests(String input) throws Exception {
        mvc.perform(post("/api/v1/bazi/charts").contentType(MediaType.APPLICATION_JSON).content(input))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void enrichedFieldsFollowZiHourDayMasterRollover() throws Exception {
        mvc.perform(post("/api/v1/bazi/charts").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"gender":"MALE","birthDate":"1986-05-29","birthTime":"23:00"}
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dayMaster.name").value("甲"))
                .andExpect(jsonPath("$.pillarDetails[0].stem.tenGod").value("食神"))
                .andExpect(jsonPath("$.pillarDetails[2].ganZhi").value("甲戌"));
    }

    @ParameterizedTest
    @CsvSource({"MALE,FORWARD", "FEMALE,REVERSE"})
    void genderControlsFortuneDirection(String gender, String direction) throws Exception {
        mvc.perform(post("/api/v1/bazi/charts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"birthDate\":\"1986-05-29\",\"birthTime\":\"13:37\",\"gender\":\"" + gender + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gender").value(gender))
                .andExpect(jsonPath("$.fortune.direction").value(direction))
                .andExpect(jsonPath("$.fortune.decades.length()").value(10))
                .andExpect(jsonPath("$.fortune.decades[0].annualYears.length()").value(10))
                .andExpect(jsonPath("$.fortune.ageConvention").value("YEAR_DIFF_PLUS_ONE"));
    }

    @Test
    void knownFemaleStartFortuneAndAnnualYear() throws Exception {
        mvc.perform(post("/api/v1/bazi/charts").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"birthDate":"1983-02-15","birthTime":"20:00","gender":"FEMALE"}
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fortune.startTime").value("1989-05-04T18:24:00"))
                .andExpect(jsonPath("$.fortune.decades[0].ganZhi").value("乙卯"))
                .andExpect(jsonPath("$.fortune.decades[0].annualYears[0].ganZhi").value("己巳"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"birthDate\":\"1986-05-29\",\"birthTime\":\"13:37\"}",
        "{\"birthDate\":\"1986-05-29\",\"birthTime\":\"13:37\",\"gender\":null}",
        "{\"birthDate\":\"1986-05-29\",\"birthTime\":\"13:37\",\"gender\":\"UNKNOWN\"}",
        "{\"birthDate\":\"1986-05-29\",\"birthTime\":\"13:37\",\"gender\":0}"})
    void rejectsMissingOrInvalidGender(String input) throws Exception {
        mvc.perform(post("/api/v1/bazi/charts").contentType(MediaType.APPLICATION_JSON).content(input))
                .andExpect(status().isBadRequest());
    }
}
