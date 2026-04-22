package com.ssafya408.cutlineparsing.util.dsl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DSL 직전 텍스트를 항상 "최대 압축"으로 변환한다.
 *
 * 순서(고정):
 *  1) 본문이 정확히 [사진]/동영상/파일/음성메시지/이모티콘 이면 즉시 태그 반환
 *  2) URL: 라인에서 "첫 URL"만 <URL>로 대체, 나머지 URL은 제거
 *  3) 텍스트 반복 이모티브:
 *     - ㅋㅋ/ㅎㅎ (2+ 연속) -> 첫 반복만 <LAUGH>, 나머지는 제거 (단일 'ㅋ','ㅎ'은 유지)
 *     - ㅠㅠ/ㅜㅜ (2+ 연속) -> 첫 반복만 <CRY>,   나머지는 제거 (단일 'ㅠ','ㅜ'은 유지)
 *  4) 이모지: 라인에서 "첫 이모지 클러스터"만 <EMOJI>, 나머지는 제거
 *  5) 공백 정리: 줄바꿈은 보존, 각 줄 내부의 다중 공백을 1칸으로, 앞뒤 trim
 */
final class DslCompressor {

    // 보수적 URL 매처(공백/닫는 괄호/일반 문장부호에서 종료)
    private static final Pattern URL = Pattern.compile("(?:(?:https?)://)[^\\s)\\]}>,]*");

    String compress(String raw) {
        String s = (raw == null) ? "" : raw;

        // 1) 미디어/스티커: 정확 일치 또는 [토큰]
        String token = stripOuterBrackets(s.strip());
        switch (token) {
            case "사진":       return "<PHOTO>";
            case "동영상":     return "<VIDEO>";
            case "파일":       return "<FILE>";
            case "음성메시지": return "<AUDIO>";
            case "이모티콘":   return "<STICKER>";
            default: /* 계속 진행 */
        }

        // 2) URL: 첫 URL만 <URL>, 나머지는 제거
        s = replaceFirstUrlAndDropRest(s);

        // 3) 반복 이모티브
        s = replaceFirstLaughAndDropRest(s); // ㅋㅋ/ㅎㅎ
        s = replaceFirstCryAndDropRest(s);   // ㅠㅠ/ㅜㅜ

        // 4) 이모지: 첫 클러스터만 <EMOJI>, 나머지는 제거
        s = replaceFirstEmojiAndDropRest(s);

        // 5) 공백 정리(줄바꿈 보존)
        s = normalizeSpaces(s);
        return s;
    }

    // ------------------------- helpers --------------------------

    /** "[ xxx ]" 처럼 바깥 대괄호 제거 + 내부 trim */
    private static String stripOuterBrackets(String x) {
        String t = (x == null) ? "" : x.trim();
        if (t.length() >= 2 && t.charAt(0) == '[' && t.charAt(t.length() - 1) == ']') {
            t = t.substring(1, t.length() - 1).trim();
        }
        return t;
    }

    /** URL: 첫 URL만 <URL>로 남기고, 그 외 URL은 제거한다(사이 텍스트/문장부호는 보존). */
    private static String replaceFirstUrlAndDropRest(String s) {
        Matcher m = URL.matcher(s);
        if (!m.find()) return s;

        // 첫 URL의 말미 문장부호는 URL 밖에 남기도록 end 조정
        int firstStart = m.start();
        int firstEnd = trimTrailingPunctForUrl(s, m.start(), m.end());

        StringBuilder out = new StringBuilder();
        out.append(s, 0, firstStart).append("<URL>");

        int lastIdx = firstEnd;

        // 이후 URL들은 제거(그 앞의 일반 텍스트는 보존)
        while (m.find()) {
            int curStart = m.start();
            int curEnd   = trimTrailingPunctForUrl(s, m.start(), m.end());
            out.append(s, lastIdx, curStart); // URL 전 일반 텍스트
            lastIdx = curEnd;                 // URL 자체는 드랍
        }

        out.append(s, lastIdx, s.length()); // 꼬리
        return out.toString();
    }

    /** URL 매치 우측의 일반 문장부호를 URL 밖으로 남기기 위해 end를 줄인다. */
    private static int trimTrailingPunctForUrl(String s, int start, int end) {
        while (end > start) {
            char ch = s.charAt(end - 1);
            if (ch == '.' || ch == ',' || ch == '!' || ch == '?' ||
                    ch == ':' || ch == ';' || ch == ')' || ch == ']' || ch == '}' || ch == '>') {
                end--;
            } else break;
        }
        return end;
    }

    /** ㅋㅋ/ㅎㅎ (2+ 연속): 첫 run만 <LAUGH>, 나머지 run은 제거. 단일 'ㅋ','ㅎ'은 그대로 둔다. */
    private static String replaceFirstLaughAndDropRest(String s) {
        StringBuilder out = new StringBuilder(s.length() + 8);
        boolean inserted = false;
        int i = 0, n = s.length();

        while (i < n) {
            int cp = s.codePointAt(i);
            int len = Character.charCount(cp);

            if (isKh(cp)) {
                int j = i, run = 0;
                while (j < n) {
                    int c2 = s.codePointAt(j);
                    if (!isKh(c2)) break;
                    j += Character.charCount(c2);
                    run++;
                }
                if (run >= 2) {
                    if (!inserted) { out.append("<LAUGH>"); inserted = true; }
                    i = j; // 이 run 스킵
                    // 이어지는 또 다른 ㅋ/ㅎ run도 모두 스킵
                    while (i < n) {
                        int c3 = s.codePointAt(i);
                        if (!isKh(c3)) break;
                        i += Character.charCount(c3);
                    }
                    continue;
                } else { // run==1
                    out.appendCodePoint(cp); i += len; continue;
                }
            } else {
                out.appendCodePoint(cp); i += len;
            }
        }
        return out.toString();
    }

    private static boolean isKh(int cp) { return cp == 'ㅋ' || cp == 'ㅎ'; }

    /** ㅠㅠ/ㅜㅜ (2+ 연속): 첫 run만 <CRY>, 나머지 run은 제거. 단일 'ㅠ','ㅜ'은 그대로 둔다. */
    private static String replaceFirstCryAndDropRest(String s) {
        StringBuilder out = new StringBuilder(s.length() + 8);
        boolean inserted = false;
        int i = 0, n = s.length();

        while (i < n) {
            int cp = s.codePointAt(i);
            int len = Character.charCount(cp);

            if (isCry(cp)) {
                int j = i, run = 0;
                while (j < n) {
                    int c2 = s.codePointAt(j);
                    if (!isCry(c2)) break;
                    j += Character.charCount(c2);
                    run++;
                }
                if (run >= 2) {
                    if (!inserted) { out.append("<CRY>"); inserted = true; }
                    i = j; // 이 run 스킵
                    // 이어지는 또 다른 ㅠ/ㅜ run도 모두 스킵
                    while (i < n) {
                        int c3 = s.codePointAt(i);
                        if (!isCry(c3)) break;
                        i += Character.charCount(c3);
                    }
                    continue;
                } else { // run==1
                    out.appendCodePoint(cp); i += len; continue;
                }
            } else {
                out.appendCodePoint(cp); i += len;
            }
        }
        return out.toString();
    }

    private static boolean isCry(int cp) { return cp == 'ㅠ' || cp == 'ㅜ'; }

    /** 이모지: 첫 "이모지 클러스터"만 <EMOJI>, 나머지 이모지는 모두 제거. */
    private static String replaceFirstEmojiAndDropRest(String s) {
        if (!containsEmojiBase(s)) return s;

        StringBuilder out = new StringBuilder(s.length() + 8);
        boolean inserted = false;

        int i = 0, n = s.length();
        while (i < n) {
            int cp = s.codePointAt(i);
            int len = Character.charCount(cp);

            if (isEmojiLike(cp)) {
                if (!inserted) { out.append("<EMOJI>"); inserted = true; }
                // 이모지 클러스터(베이스+결합요소) 전부 스킵
                i += len;
                while (i < n) {
                    int cp2 = s.codePointAt(i);
                    if (!isEmojiLike(cp2)) break;
                    i += Character.charCount(cp2);
                }
                // 이후의 다른 이모지들도 발견되면 전부 스킵(토큰 추가 없음)
                while (i < n) {
                    int look = s.codePointAt(i);
                    if (!isEmojiLike(look)) break;
                    i += Character.charCount(look);
                }
            } else {
                out.appendCodePoint(cp);
                i += len;
            }
        }
        return out.toString();
    }

    /** 줄바꿈은 보존하고, 각 줄 내부의 공백을 1칸으로 축약 + 앞뒤 trim */
    private static String normalizeSpaces(String s) {
        String[] lines = s.split("\\r?\\n", -1); // 빈 줄도 보존
        for (int i = 0; i < lines.length; i++) {
            // 유니코드 공백/탭(NBSP 포함)을 1칸으로
            lines[i] = lines[i].replaceAll("[\\p{Zs}\\t\\u00A0]+", " ").trim();
        }
        String joined = String.join("\n", lines);
        return joined.strip();
    }

    // ----------------------- Emoji 판정 유틸 ---------------------

    /** 이모지 "베이스"가 하나라도 포함되어 있는지 빠른 검사 */
    private static boolean containsEmojiBase(String s) {
        return s.codePoints().anyMatch(DslCompressor::isEmojiBase);
    }

    /** 이모지 베이스(표시 문자) 여부 */
    private static boolean isEmojiBase(int cp) {
        // 대부분의 이모지는 So(OTHER_SYMBOL)
        if (Character.getType(cp) == Character.OTHER_SYMBOL) return true;
        // 국기(Regional Indicator)
        if (cp >= 0x1F1E6 && cp <= 0x1F1FF) return true;
        return false;
    }

    /** 이모지 결합 요소(시퀀스 구성 요소) */
    private static boolean isEmojiCombiner(int cp) {
        // ZWJ
        if (cp == 0x200D) return true;
        // Variation Selector / Keycap
        if ((cp >= 0xFE00 && cp <= 0xFE0F) || cp == 0x20E3) return true;
        // Tag sequences (subdivision flags 등)
        if (cp >= 0xE0020 && cp <= 0xE007F) return true;
        // Skin tone modifiers
        if (cp >= 0x1F3FB && cp <= 0x1F3FF) return true;
        return false;
    }

    /** 이모지 시퀀스에 속하는 코드포인트인지(베이스 또는 결합 요소) */
    private static boolean isEmojiLike(int cp) {
        return isEmojiBase(cp) || isEmojiCombiner(cp);
    }
}