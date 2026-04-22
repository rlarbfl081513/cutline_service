package com.ssafya408.cutlineparsing;

import com.ssafya408.cutlineparsing.util.Speaker;
import com.ssafya408.cutlineparsing.util.dsl.DslBuilder;
import com.ssafya408.cutlineparsing.util.dsl.DslEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DslBuilderTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private static ZonedDateTime dt(int y,int m,int d,int h,int min){
        return ZonedDateTime.of(y,m,d,h,min,0,0,KST);
    }

    @Test
    @DisplayName("같은 화자 + 같은 분은 한 턴으로 병합되고, 다음 턴의 Δ분을 계산한다")
    void build_basic_lines_with_merge_and_delta() {
        var events = List.of(
                new DslEvent(Speaker.U, dt(2025,8,3,16,0), "A"),
                new DslEvent(Speaker.U, dt(2025,8,3,16,0), "B"),  // 같은 분 → 병합
                new DslEvent(Speaker.F, dt(2025,8,3,16,3), "C")   // 다른 화자, 3분 후
        );

        String out = new DslBuilder().build(events);

        String expected = String.join("\n",
                "U|+0|\"A\\nB\"",
                "F|+3|\"C\""
        );
        assertEquals(expected, out);
    }

    @Test
    @DisplayName("입력이 뒤섞여 있어도 시간순으로 정렬되고 Δ분이 정확하다")
    void build_sorts_and_computes_delta() {
        var events = List.of(
                new DslEvent(Speaker.F, dt(2025,8,3,16,3), "C"), // 나중 것
                new DslEvent(Speaker.U, dt(2025,8,3,16,0), "A"), // 먼저 것
                new DslEvent(Speaker.U, dt(2025,8,3,16,0), "B")  // 같은 분 병합
        );

        String out = new DslBuilder().build(events);

        String expected = String.join("\n",
                "U|+0|\"A\\nB\"",
                "F|+3|\"C\""
        );
        assertEquals(expected, out);
    }

    @Test
    @DisplayName("텍스트 이스케이프: 따옴표/역슬래시/개행을 DSL에 안전하게 변환한다")
    void escape_quotes_backslashes_and_newlines() {
        var events = List.of(
                new DslEvent(Speaker.U, dt(2025,8,3,16,0), "그가 말했다: \"hi\" 그리고 경로는 C:\\temp"),
                new DslEvent(Speaker.F, dt(2025,8,3,16,5), "첫줄\n둘째줄")
        );

        String out = new DslBuilder().build(events);

        String expected = String.join("\n",
                "U|+0|\"그가 말했다: \\\"hi\\\" 그리고 경로는 C:\\\\temp\"",
                "F|+5|\"첫줄\\n둘째줄\""
        );
        assertEquals(expected, out);
    }

    @DisplayName("URL이 여러 개여도 첫 URL만 <URL>로, 나머지는 제거한다")
    @Test
    void compress_urls_to_single_token_at_first_position() {
        var events = List.of(
                new DslEvent(Speaker.U, dt(2025,8,3,16,0),
                        "링크1 https://a.com/x 그리고 https://b.com/y 끝")
        );
        String out = new DslBuilder().build(events);
        assertEquals("U|+0|\"링크1 <URL> 그리고 끝\"", out);
    }

    @DisplayName("이모지가 여러 개여도 첫 이모지 위치에만 <EMOJI>를 남긴다")
    @Test
    void compress_emojis_to_single_token_at_first_position() {
        var events = List.of(
                new DslEvent(Speaker.F, dt(2025,8,3,16,5), "굿 😀😅 좋아요 👍")
        );
        String out = new DslBuilder().build(events);
        assertEquals("F|+0|\"굿 <EMOJI> 좋아요\"", out);
    }

    @DisplayName("본문이 정확히 [사진]/동영상/파일/음성메시지/이모티콘이면 태그로 치환한다")
    @Test
    void collapse_exact_media_and_sticker() {
        var ev = List.of(
                new DslEvent(Speaker.F, dt(2025,8,3,16,0), "[사진]"),
                new DslEvent(Speaker.F, dt(2025,8,3,16,1), "동영상"),
                new DslEvent(Speaker.F, dt(2025,8,3,16,2), "파일"),
                new DslEvent(Speaker.F, dt(2025,8,3,16,3), "음성메시지"),
                new DslEvent(Speaker.F, dt(2025,8,3,16,4), "이모티콘")
        );
        String expected = String.join("\n",
                "F|+0|\"<PHOTO>\"",
                "F|+1|\"<VIDEO>\"",
                "F|+1|\"<FILE>\"",
                "F|+1|\"<AUDIO>\"",
                "F|+1|\"<STICKER>\""
        );
        assertEquals(expected, new DslBuilder().build(ev));
    }

    @DisplayName("혼합: '사진 https://… 😀'는 사진은 그대로, URL은 <URL>, 이모지는 <EMOJI> 한 개만")
    @Test
    void mixed_line_keeps_text_and_single_tokens() {
        var ev = List.of(
                new DslEvent(Speaker.U, dt(2025,8,3,16,9), "사진 https://x.io 😀 또 https://y.io")
        );
        String out = new DslBuilder().build(ev);
        assertEquals("U|+0|\"사진 <URL> <EMOJI> 또\"", out);
    }

    @Test
    @DisplayName("대괄호 안/밖 공백이 있어도 미디어/스티커 정확 일치는 태그로 치환")
    void collapse_media_sticker_with_bracket_spaces() {
        var ev = List.of(
                new DslEvent(Speaker.U, dt(2025,8,3,10,0), "[  사진 ]"),
                new DslEvent(Speaker.U, dt(2025,8,3,10,1), "[ 이모티콘]"),
                new DslEvent(Speaker.U, dt(2025,8,3,10,2), "[동영상 ]")
        );
        String expected = String.join("\n",
                "U|+0|\"<PHOTO>\"",
                "U|+1|\"<STICKER>\"",
                "U|+1|\"<VIDEO>\""
        );
        assertEquals(expected, new DslBuilder().build(ev));
    }

    @Test
    @DisplayName("URL 뒤 문장부호는 토큰 밖에 남는다 (괄호/쉼표/마침표)")
    void url_trailing_punctuation_stays_outside_token() {
        var ev = List.of(
                new DslEvent(Speaker.U, dt(2025,8,3,11,0), "보기 https://a.com/x). 끝."),
                new DslEvent(Speaker.U, dt(2025,8,3,11,1), "두 개: https://a.com/x, 그리고 https://b.com/y.")
        );
        String out = new DslBuilder().build(ev);
        String expected = String.join("\n",
                "U|+0|\"보기 <URL>). 끝.\"",
                "U|+1|\"두 개: <URL>, 그리고 .\""
        );
        assertEquals(expected, out);
    }

    @Test
    @DisplayName("ㅋㅋ/ㅎㅎ 반복은 라인에서 첫 반복만 <LAUGH>로 남기고 나머지는 제거")
    void collapse_repeated_kh_to_single_laugh_token() {
        var ev = List.of(
                new DslEvent(Speaker.U, dt(2025,8,3,16,0), "ㅋㅋㅋㅋㅋㅋㅋㅋ"),
                new DslEvent(Speaker.U, dt(2025,8,3,16,1), "아 ㅋㅋㅋㅋ 진짜 ㅎㅎㅎㅎ")
        );
        String out = new DslBuilder().build(ev);
        String expected = String.join("\n",
                "U|+0|\"<LAUGH>\"",
                "U|+1|\"아 <LAUGH> 진짜\""
        );
        assertEquals(expected, out);
    }

    @Test
@DisplayName("ㅠㅠ/ㅜㅜ 반복은 라인에서 첫 반복만 <CRY>로 남기고 나머지는 제거")
void collapse_repeated_cry_to_single_token() {
    var ev = List.of(
        new DslEvent(Speaker.F, dt(2025,8,3,17,0), "ㅠㅠㅠㅠㅠ"),
        new DslEvent(Speaker.F, dt(2025,8,3,17,1), "허걱 ㅜㅜ 진짜")
    );
    String out = new DslBuilder().build(ev);
    String expected = String.join("\n",
        "F|+0|\"<CRY>\"",
        "F|+1|\"허걱 <CRY> 진짜\""
    );
    assertEquals(expected, out);
}

@Test
@DisplayName("웃음/울음/이모지/URL이 섞여도 라인당 각 카테고리 첫 토큰만 남는다")
void mixed_kinds_leave_single_tokens_each() {
    var ev = List.of(
        new DslEvent(Speaker.U, dt(2025,8,3,18,0), "ㅋㅋㅋㅋ https://a.com 😀 ㅠㅠ 또 https://b.com ㅎㅎ")
    );
    String out = new DslBuilder().build(ev);
    // 정책: URL/EMOJI/LAUGH/CRY는 각 1개만, 첫 위치를 유지. 나머지 제거.
    assertEquals("U|+0|\"<LAUGH> <URL> <EMOJI> <CRY> 또\"", out);
}
}
