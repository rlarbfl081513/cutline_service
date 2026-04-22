package com.ssafya408.cutlineparsing.util.parser;

import java.util.regex.Pattern;

public final class SystemFilters {
    private SystemFilters() {}

    private static final Pattern DELETED = Pattern.compile(
            "^\\s*(메시지가 삭제되었습니다|삭제된 메시지입니다)[.!?]*\\s*$"
    );

    /** 드랍해야 하는 시스템류/무의미 본문이면 true */
    public static boolean isDrop(String text) {
        if (text == null) return true;            // 안전
        String s = text.strip();
        if (s.isEmpty()) return true;             // 완전 빈 본문
        if (DELETED.matcher(s).matches()) return true;
        return false;
    }
}