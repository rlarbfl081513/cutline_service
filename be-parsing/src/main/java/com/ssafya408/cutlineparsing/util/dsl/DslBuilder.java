package com.ssafya408.cutlineparsing.util.dsl;

import com.ssafya408.cutlineparsing.util.Speaker;
import java.util.List;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

public final class DslBuilder {

    // ★ 여기 추가: 항상-최대 압축 컴포저
    private final DslCompressor compressor = new DslCompressor();

    public String build(List<DslEvent> events) {
        if (events == null || events.isEmpty()) return "";

        // 시간 정렬(안전장치)
        List<DslEvent> sorted = new ArrayList<>(events);
        sorted.sort(Comparator.comparing(DslEvent::dt));

        List<String> lines = new ArrayList<>();

        Speaker curSpk = null;
        ZonedDateTime curStart = null; // 현재 턴 시작(분)
        ZonedDateTime curEnd   = null; // 현재 턴 끝(분)
        StringBuilder curText  = null;

        ZonedDateTime prevEnd = null;  // 직전 턴 end(분)

        for (DslEvent e : sorted) {
            ZonedDateTime minute = e.dt().withSecond(0).withNano(0);

            if (curSpk == null) {
                // 첫 이벤트로 새 턴 시작
                curSpk  = e.speaker();
                curStart= minute;
                curEnd  = minute;
                curText = new StringBuilder(e.text());
                continue;
            }

            boolean sameSpeaker = e.speaker() == curSpk;
            boolean sameMinute  = minute.equals(curStart); // "같은 분" 기준

            if (sameSpeaker && sameMinute) {
                // 같은 턴 병합
                if (curText.length() > 0) curText.append('\n');
                curText.append(e.text());
                curEnd = minute;
            } else {
                // 현재 턴 flush
                long delta = (prevEnd == null) ? 0 : Duration.between(prevEnd, curStart).toMinutes();
                lines.add(formatLine(curSpk, delta, curText.toString()));
                prevEnd = curEnd;

                // 새 턴 시작
                curSpk  = e.speaker();
                curStart= minute;
                curEnd  = minute;
                curText = new StringBuilder(e.text());
            }
        }

        // 마지막 턴 flush
        if (curSpk != null) {
            long delta = (prevEnd == null) ? 0 : Duration.between(prevEnd, curStart).toMinutes();
            lines.add(formatLine(curSpk, delta, curText.toString()));
        }

        return lines.stream().collect(Collectors.joining("\n"));
    }

    /** 한 라인을 U|+Δ|"텍스트" 로 만든다. */
    private String formatLine(Speaker spk, long delta, String text) {
        String uf = (spk == Speaker.U) ? "U" : "F";

        // ★ 압축은 escape 전에 수행 (토큰 치환 후에 따옴표/역슬래시/개행 이스케이프)
        String compressed = compressor.compress(text);

        return uf + "|+"
                + delta
                + "|\""
                + escape(compressed)
                + "\"";
    }

    /** DSL용 텍스트 이스케이프: 역슬래시, 큰따옴표, 개행 */
    private String escape(String s) {
        if (s == null) return "";
        StringBuilder out = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '\"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> { /* drop CR */ }
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}