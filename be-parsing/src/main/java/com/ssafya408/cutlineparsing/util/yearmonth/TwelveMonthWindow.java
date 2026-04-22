package com.ssafya408.cutlineparsing.util.yearmonth;

import java.time.*;

public final class TwelveMonthWindow {
    private final Clock clock;
    private final YearMonth startYm; // inclusive
    private final YearMonth endYm;   // inclusive

    public TwelveMonthWindow(Clock clock) {
        this.clock = clock;
        YearMonth cur = YearMonth.from(LocalDate.now(clock));
        this.endYm   = cur;
        this.startYm = cur.minusMonths(11);
    }

    /** 해당 타임스탬프의 YearMonth가 12개월 윈도우에 들어오면 true */
    public boolean within(ZonedDateTime ts) {
        YearMonth ym = YearMonth.from(ts.withZoneSameInstant(clock.getZone()));
        return !ym.isBefore(startYm) && !ym.isAfter(endYm);
    }

    /** 테스트/로깅 용도로 범위를 문자열로 확인 */
    @Override public String toString() {
        return "TwelveMonthWindow[" + startYm + " .. " + endYm + "]";
    }
}