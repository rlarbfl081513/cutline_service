package com.ssafya408.cutlineparsing.util.parser;

import java.time.ZonedDateTime;

public record MessageRaw(
        ZonedDateTime dt,
        String speakerName,
        String text,
        boolean isSystem // 지금은 항상 false로만 사용(향후 시스템 메시지 구분 여지)
) {}