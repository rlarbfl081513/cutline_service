package com.ssafya408.cutlineparsing;

import com.ssafya408.cutlineparsing.util.Speaker;
import com.ssafya408.cutlineparsing.util.yearmonth.MonthRouter;
import com.ssafya408.cutlineparsing.util.yearmonth.Turn;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MonthRouterTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static ZonedDateTime dt(int y,int m,int d,int h,int min){
        return ZonedDateTime.of(y,m,d,h,min,0,0,KST);
    }

    @Test
    @DisplayName("월 경계에서 분리되고, 각 월 내부는 시간순이 보장된다")
    void splits_by_month_and_preserves_order() {
        var turns = List.of(
                new Turn(Speaker.F, dt(2025,8,1,0,1),  dt(2025,8,1,0,1),  "Aug-hi",   1),
                new Turn(Speaker.U, dt(2025,7,31,23,59), dt(2025,7,31,23,59), "Jul-bye", 1),
                new Turn(Speaker.U, dt(2025,8,1,12,0), dt(2025,8,1,12,0), "Aug-noon", 1)
        );

        Map<YearMonth, List<Turn>> byMonth = new MonthRouter().route(turns);

        assertEquals(Set.of(YearMonth.of(2025,7), YearMonth.of(2025,8)), byMonth.keySet());
        var july = byMonth.get(YearMonth.of(2025,7));
        var aug  = byMonth.get(YearMonth.of(2025,8));

        assertEquals(1, july.size());
        assertEquals("Jul-bye", july.get(0).text());

        assertEquals(2, aug.size());
        assertEquals("Aug-hi",   aug.get(0).text());
        assertEquals("Aug-noon", aug.get(1).text());
    }
}
