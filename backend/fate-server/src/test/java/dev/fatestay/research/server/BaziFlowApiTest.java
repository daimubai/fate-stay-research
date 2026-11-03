package dev.fatestay.research.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BaziFlowApiTest {
    @Autowired MockMvc mvc;

    @Test
    void publicFlowMonthsUsesBirthDayMasterAndExactJieBoundaries() throws Exception {
        mvc.perform(post("/api/v1/bazi/flow-months").contentType(MediaType.APPLICATION_JSON)
                .content("""
                  {"birthDate":"1986-05-29","birthTime":"13:37","year":2024}
                  """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.yearGanZhi").value("甲辰"))
                .andExpect(jsonPath("$.dayMaster.name").value("癸"))
                .andExpect(jsonPath("$.months.length()").value(12))
                .andExpect(jsonPath("$.months[0].ganZhi").value("丙寅"))
                .andExpect(jsonPath("$.months[0].stem.tenGod").value("正财"))
                .andExpect(jsonPath("$.months[0].startTime").value("2024-02-04T16:27:07"))
                .andExpect(jsonPath("$.months[11].term").value("小寒"));
    }

    @Test
    void publicFlowDaysIncludesClippedIntervalsAndLunarLabels() throws Exception {
        mvc.perform(post("/api/v1/bazi/flow-days").contentType(MediaType.APPLICATION_JSON)
                .content("""
                  {"birthDate":"1986-05-29","birthTime":"13:37","year":2024,"monthIndex":1}
                  """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthIndex").value(1))
                .andExpect(jsonPath("$.days[0].date").value("2024-02-04"))
                .andExpect(jsonPath("$.days[0].ganZhi").value("戊戌"))
                .andExpect(jsonPath("$.days[0].partial").value(true))
                .andExpect(jsonPath("$.days[0].endTime").value("2024-02-04T23:00:00"))
                .andExpect(jsonPath("$.days[1].date").value("2024-02-05"))
                .andExpect(jsonPath("$.days[1].startTime").value("2024-02-04T23:00:00"))
                .andExpect(jsonPath("$.days[-1].endTime").value("2024-03-05T10:22:45"))
                .andExpect(jsonPath("$.days[0].lunarDate.display").isNotEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "null", "{bad", "{\"birthDate\":\"1986-05-29\",\"birthTime\":\"13:37\"}",
        "{\"birthDate\":\"1986-05-29\",\"birthTime\":\"13:37\",\"year\":2211}",
        "{\"birthDate\":\"1986-05-29\",\"birthTime\":\"13:37\",\"year\":1899}",
        "{\"birthDate\":\"1899-05-29\",\"birthTime\":\"13:37\",\"year\":2024}",
        "{\"birthDate\":\"1986-05-29\",\"birthTime\":\"13:37:01\",\"year\":2024}"})
    void rejectsInvalidFlowMonthInput(String input) throws Exception {
        mvc.perform(post("/api/v1/bazi/flow-months").contentType(MediaType.APPLICATION_JSON).content(input))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "0", "13"})
    void rejectsMissingOrInvalidMonthIndex(String index) throws Exception {
        mvc.perform(post("/api/v1/bazi/flow-days").contentType(MediaType.APPLICATION_JSON)
                .content("{\"birthDate\":\"1986-05-29\",\"birthTime\":\"13:37\",\"year\":2024,\"monthIndex\":" + index + "}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void extraPathsAndGetAreNotPublic() throws Exception {
        mvc.perform(get("/api/v1/bazi/flow-months")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/bazi/flow-days/unsafe")).andExpect(status().isForbidden());
    }

    @Test
    void twelveFlowHoursArePublicAndUsePreviousNightZi() throws Exception {
        mvc.perform(post("/api/v1/bazi/flow-hours").contentType(MediaType.APPLICATION_JSON)
                .content("""
                  {"birthDate":"1986-05-29","birthTime":"13:37","year":2024,"monthIndex":1,"date":"2024-02-29"}
                  """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hours.length()").value(12))
                .andExpect(jsonPath("$.hours[0].name").value("子时"))
                .andExpect(jsonPath("$.hours[0].ganZhi").value("壬子"))
                .andExpect(jsonPath("$.hours[0].startTime").value("2024-02-28T23:00:00"))
                .andExpect(jsonPath("$.hours[0].endTime").value("2024-02-29T01:00:00"))
                .andExpect(jsonPath("$.hours[0].stem.tenGod").value("劫财"))
                .andExpect(jsonPath("$.hours[0].naYin").value("桑柘木"))
                .andExpect(jsonPath("$.hours[11].endTime").value("2024-02-29T23:00:00"));
    }

    @Test
    void clippedDayMarksOutsideAndPartialHoursWithoutFabricatedIntervals() throws Exception {
        mvc.perform(post("/api/v1/bazi/flow-hours").contentType(MediaType.APPLICATION_JSON)
                .content("""
                  {"birthDate":"1986-05-29","birthTime":"13:37","year":2024,"monthIndex":1,"date":"2024-02-04"}
                  """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hours[0].included").value(false))
                .andExpect(jsonPath("$.hours[0].startTime").isEmpty())
                .andExpect(jsonPath("$.hours[8].included").value(true))
                .andExpect(jsonPath("$.hours[8].partial").value(true))
                .andExpect(jsonPath("$.hours[8].startTime").value("2024-02-04T16:27:07"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "\"2024-01-01\"", "\"2024-02-30\""})
    void rejectsNullInvalidOrMismatchedFlowDate(String date) throws Exception {
        mvc.perform(post("/api/v1/bazi/flow-hours").contentType(MediaType.APPLICATION_JSON)
                .content("{\"birthDate\":\"1986-05-29\",\"birthTime\":\"13:37\",\"year\":2024,\"monthIndex\":1,\"date\":" + date + "}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "null",
        "{\"birthDate\":\"1986-05-29\",\"birthTime\":\"13:37\",\"year\":2024,\"monthIndex\":1}",
        "{\"birthDate\":\"1986-05-29\",\"birthTime\":\"13:37\",\"year\":2024,\"monthIndex\":13,\"date\":\"2024-02-29\"}"})
    void rejectsIncompleteHourRequestsAndInvalidMonth(String input) throws Exception {
        mvc.perform(post("/api/v1/bazi/flow-hours").contentType(MediaType.APPLICATION_JSON).content(input))
                .andExpect(status().isBadRequest());
    }

    @Test
    void hourGetAndUnrelatedPathsRemainDenied() throws Exception {
        mvc.perform(get("/api/v1/bazi/flow-hours")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/bazi/flow-hours/private")).andExpect(status().isForbidden());
    }
}
