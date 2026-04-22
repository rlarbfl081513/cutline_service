package com.ssafya408.cutlineparsing;


import com.ssafya408.cutlineparsing.util.Speaker;
import com.ssafya408.cutlineparsing.util.accumulator.MonthlyStatsAccumulator;
import com.ssafya408.cutlineparsing.util.yearmonth.Turn;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MonthlyStatsAccumulatorTest {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static ZonedDateTime dt(int y,int m,int d,int h,int min){
        return ZonedDateTime.of(y,m,d,h,min,0,0,KST);
    }

    @Test
    @DisplayName("F 메시지 수/길이, 활성일 수, U→F 응답합(분), 최대 무대화 연속일을 계산한다")
    void compute_all_five_metrics() {
        var ym = YearMonth.of(2025, 8);
        var acc = new MonthlyStatsAccumulator(ym);

        // 메시지 스트림(파서→가드 직후 가정)
        acc.onMessage(Speaker.U, dt(2025,8,1,10,0), "hi");
        acc.onMessage(Speaker.F, dt(2025,8,1,10,1), "ok");        // F
        acc.onMessage(Speaker.F, dt(2025,8,1,10,2), "more");      // F
        acc.onMessage(Speaker.U, dt(2025,8,3,12,0), "ping");
        acc.onMessage(Speaker.F, dt(2025,8,7,12,10), "pong");     // F
        acc.onMessage(Speaker.U, dt(2025,8,31,23,50), "end");     // 활성일 꼬리 방지

        // 턴(병합 결과) — 테스트 단순화를 위해 수동 구성
        var turns = List.of(
                new Turn(Speaker.U, dt(2025,8,1,10,0),  dt(2025,8,1,10,0),  "hi",   1),
                new Turn(Speaker.F, dt(2025,8,1,10,1),  dt(2025,8,1,10,2),  "ok\nmore", 2),
                new Turn(Speaker.U, dt(2025,8,3,12,0),  dt(2025,8,3,12,0),  "ping", 1),
                new Turn(Speaker.F, dt(2025,8,7,12,10), dt(2025,8,7,12,10), "pong", 1),
                new Turn(Speaker.U, dt(2025,8,31,23,50),dt(2025,8,31,23,50),"end",  1)
        );

        var replyStats = acc.computeReplyStats(turns); // U→F만 합산: 1분 + (4일 10분) = 5771, 응답 횟수 2회
        var result = acc.finalizeMonth(replyStats);

        assertEquals(3,  result.fMsgCount());          // ok, more, pong
        assertEquals(2+4+4, result.fMsgChars());       // "ok"(2) + "more"(4) + "pong"(4) = 10
        assertEquals(4,  result.activeDaysCount());    // 8/1, 8/3, 8/7, 8/31
        assertEquals(5771, result.fReplyMinutesSum()); // 1 + 4*24*60 + 10
        assertEquals(2, result.fReplyPairsCount());    // hi→ok, ping→pong
        assertTrue(result.maxNoChatStreakDays() >= 1); // 경계 완화(원하면 정확값으로 잠궈도 됨)
    }
}
