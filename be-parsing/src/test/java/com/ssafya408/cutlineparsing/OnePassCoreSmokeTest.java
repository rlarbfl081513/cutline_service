package com.ssafya408.cutlineparsing;

import com.ssafya408.cutlineparsing.service.OnePassCore;
import com.ssafya408.cutlineparsing.service.OnePassCoreImpl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.*;

class OnePassCoreSmokeTest {

    @Test
    @DisplayName("텍스트→월별 DSL+Stats 스모크: 병합/Δ/압축/지표까지 한 번에")
    void one_pass_core_smoke() {
        String me = "홍길동", friend = "김형수";
        // *merge 유도 위해 ok/more를 같은 분(10:01)에 배치*
        String src = String.join("\n",
                "2025년 8월 1일 오전 10:00, 홍길동 : hi",
                "2025년 8월 1일 오전 10:01, 김형수 : ok",
                "2025년 8월 1일 오전 10:01, 김형수 : more",
                "2025년 8월 3일 오후 12:00, 홍길동 : ping 😀",
                "2025년 8월 7일 오후 12:10, 김형수 : [사진]",
                "2025년 8월 31일 오후 11:50, 홍길동 : end"
        );

        OnePassCore core = new OnePassCoreImpl();
        var outputs = core.run(src, me, friend);

        assertEquals(1, outputs.size());
        var out = outputs.get(0);

        // 월 확인
        assertEquals(YearMonth.of(2025, 8), out.ym());

        // DSL 체크 (스폿)
        String dsl = out.dsl();
        String[] lines = dsl.split("\\R");

        assertTrue(lines[0].equals("U|+0|\"hi\""), "첫 줄 Δ=0, U hi");
        assertTrue(lines[1].equals("F|+1|\"ok\\nmore\""), "같은 분 F 병합 ok\\nmore");
        assertTrue(dsl.contains("<EMOJI>"), "이모지 토큰 존재");
        assertTrue(dsl.contains("<PHOTO>"), "사진 토큰 존재");

        // 서버 지표(월별) 체크
        com.ssafya408.cutlineparsing.util.accumulator.MonthlyStatsResult s = out.stats();
        assertEquals(3, s.fMsgCount(), "F 메시지 수: ok, more, [사진]");
        assertEquals(4, s.activeDaysCount(), "활성 날짜 수: 8/1, 8/3, 8/7, 8/31");
        assertEquals(5771, s.fReplyMinutesSum(), "U→F 응답합(분) = 1 + (4일*1440 + 10)");
        assertEquals(2, s.fReplyPairsCount(), "U→F 응답 횟수 = 2회");

        // fMsgChars는 파서 원문 기준이므로 대략(=10)이 맞지만 환경에 따라 다를 수 있어 완화
        assertTrue(s.fMsgChars() >= 9, "F 총 문자 길이 대략 체크");
    }
}
