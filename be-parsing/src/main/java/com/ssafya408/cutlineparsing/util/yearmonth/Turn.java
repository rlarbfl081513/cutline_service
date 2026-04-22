package com.ssafya408.cutlineparsing.util.yearmonth;

import com.ssafya408.cutlineparsing.util.Speaker;

import java.time.ZonedDateTime;

public record Turn(
        Speaker speaker,
        ZonedDateTime start,   // 턴 첫 메시지 시각(분)
        ZonedDateTime end,     // 턴 마지막 메시지 시각(분)
        String text,           // 병합된 본문
        int msgCount           // 이 턴에 포함된 메시지 수
) {}