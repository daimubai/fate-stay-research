package dev.fatestay.research.chart;

import dev.fatestay.research.bazi.BaziCalculator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bazi")
public class BaziController {
    private final BaziCalculator calculator = new BaziCalculator();

    @PostMapping("/charts")
    public BaziCalculator.Chart calculate(@Valid @RequestBody BirthRequest input) {
        BaziCalculator.BirthGender gender;
        try {
            gender = BaziCalculator.BirthGender.valueOf(input.gender());
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("性别仅支持 MALE（男）或 FEMALE（女）");
        }
        CalendarType type = input.calendarType() == null ? CalendarType.SOLAR : input.calendarType();
        if (type == CalendarType.LUNAR) {
            LunarInput lunar = input.lunarDate();
            if (input.birthDate() != null || lunar == null || lunar.year() == null
                    || lunar.month() == null || lunar.day() == null || lunar.leapMonth() == null) {
                throw new IllegalArgumentException("农历模式请仅提供完整的农历年月日和闰月标记");
            }
            return calculator.calculateLunar(lunar.year(), lunar.month(), lunar.day(),
                    lunar.leapMonth(), input.birthTime(), gender);
        }
        if (input.birthDate() == null || input.lunarDate() != null) {
            throw new IllegalArgumentException("公历模式请仅提供有效的公历出生日期");
        }
        return calculator.calculate(input.birthDate().atTime(input.birthTime()), gender);
    }

    public enum CalendarType { SOLAR, LUNAR }
    public record LunarInput(Integer year, Integer month, Integer day, Boolean leapMonth) {}
    public record BirthRequest(CalendarType calendarType, LocalDate birthDate,
                               LunarInput lunarDate, @NotNull LocalTime birthTime, @NotNull String gender) {}
    public record InputError(String message) {}

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    InputError invalidInput(IllegalArgumentException error) {
        return new InputError(error.getMessage());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentNotValidException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    InputError invalidRequest() {
        return new InputError("请提供有效的性别、历法、出生日期和时间；公历格式为 YYYY-MM-DD，时间为 HH:mm");
    }
}
