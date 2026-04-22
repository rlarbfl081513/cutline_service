package com.ssafya408.cutlineparsing.util.dsl;

import com.ssafya408.cutlineparsing.util.Speaker;
import java.time.ZonedDateTime;

/** DSL 빌더 입력 이벤트: 화자(U/F), 시각(분까지), 원문 텍스트 */
public record DslEvent(
        Speaker speaker,
        ZonedDateTime dt,
        String text
) {}

