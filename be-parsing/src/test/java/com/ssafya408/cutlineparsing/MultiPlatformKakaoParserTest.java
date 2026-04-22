package com.ssafya408.cutlineparsing;

import com.ssafya408.cutlineparsing.util.parser.MultiPlatformKakaoParser;
import com.ssafya408.cutlineparsing.util.parser.MessageRaw;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class MultiPlatformKakaoParserTest {
    
    private final ZoneId KST = ZoneId.of("Asia/Seoul");

    /**
     * 테스트 헬퍼: 단일 라인을 파싱하여 결과 반환
     */
    private MessageRaw parseTestLine(String line) {
        MultiPlatformKakaoParser testParser = new MultiPlatformKakaoParser();
        
        // 스니핑을 위해 여러 번 같은 라인 입력
        MessageRaw result = null;
        for (int i = 0; i < 60; i++) {
            var parsed = testParser.parse(line);
            if (parsed.isPresent()) {
                result = parsed.get();
                break;
            }
        }
        
        assertNotNull(result, "파싱 결과가 없습니다: " + line);
        return result;
    }

    /**
     * 테스트 헬퍼: PC 메시지 파싱 (날짜 헤더 포함)
     */
    private MessageRaw parsePCTestLine(String dateHeader, String messageLine) {
        MultiPlatformKakaoParser testParser = new MultiPlatformKakaoParser();
        
        // 날짜 헤더 먼저 처리
        for (int i = 0; i < 30; i++) {
            testParser.parse(dateHeader);
        }
        
        // 메시지 라인 처리
        MessageRaw result = null;
        for (int i = 0; i < 60; i++) {
            var parsed = testParser.parse(messageLine);
            if (parsed.isPresent()) {
                result = parsed.get();
                break;
            }
        }
        
        assertNotNull(result, "PC 파싱 결과가 없습니다: " + messageLine);
        return result;
    }

    @Test
    @DisplayName("Android 플랫폼 - 기본 PM 라인 파싱")
    void parse_android_pm_line() {
        String line = "2025년 8월 3일 오후 4:00, 홍길동 : 응 프론트 안해~";
        MessageRaw result = parseTestLine(line);
        
        assertEquals(ZonedDateTime.of(2025, 8, 3, 16, 0, 0, 0, KST), result.dt());
        assertEquals("홍길동", result.speakerName());
        assertEquals("응 프론트 안해~", result.text());
        assertFalse(result.isSystem());
    }

    @Test
    @DisplayName("Android 플랫폼 - 오전 시간 파싱")
    void parse_android_am_line() {
        String line = "2025년 8월 3일 오전 9:30, 홍길동 : 안녕하세요";
        MessageRaw result = parseTestLine(line);
        
        assertEquals(ZonedDateTime.of(2025, 8, 3, 9, 30, 0, 0, KST), result.dt());
        assertEquals("홍길동", result.speakerName());
        assertEquals("안녕하세요", result.text());
    }

    @Test
    @DisplayName("iOS 플랫폼 - 점 형식 날짜 파싱")
    void parse_ios_dot_format() {
        String line = "2025. 8. 3. 16:00, 홍길동 : iOS에서 보내는 메시지";
        MessageRaw result = parseTestLine(line);
        
        assertEquals(ZonedDateTime.of(2025, 8, 3, 16, 0, 0, 0, KST), result.dt());
        assertEquals("홍길동", result.speakerName());
        assertEquals("iOS에서 보내는 메시지", result.text());
    }

    @Test
    @DisplayName("PC 플랫폼 - 대괄호 형식 파싱")
    void parse_pc_bracket_format() {
        String dateHeader = "--- 2025년 8월 3일 토요일 ---";
        String messageLine = "[홍길동] [오후 4:00] PC에서 보내는 메시지";
        
        MessageRaw result = parsePCTestLine(dateHeader, messageLine);
        
        assertEquals(ZonedDateTime.of(2025, 8, 3, 16, 0, 0, 0, KST), result.dt());
        assertEquals("홍길동", result.speakerName());
        assertEquals("PC에서 보내는 메시지", result.text());
        assertFalse(result.isSystem());
    }

    @Test
    @DisplayName("오전/오후 경계값 테스트 - 오전 12시")
    void parse_am_midnight_edge() {
        String line = "2025년 8월 4일 오전 12:02, 홍길동 : 맞아";
        MessageRaw result = parseTestLine(line);
        
        assertEquals(0, result.dt().getHour());
        assertEquals(2, result.dt().getMinute());
    }

    @Test
    @DisplayName("오전/오후 경계값 테스트 - 오후 12시")
    void parse_pm_noon_edge() {
        String line = "2025년 8월 4일 오후 12:23, 홍길동 : 점심?";
        MessageRaw result = parseTestLine(line);
        
        assertEquals(12, result.dt().getHour());
        assertEquals(23, result.dt().getMinute());
    }

    @Test
    @DisplayName("빈 줄/공백 줄은 파싱하지 않음")
    void ignore_blank_lines() {
        MultiPlatformKakaoParser testParser = new MultiPlatformKakaoParser();
        assertTrue(testParser.parse("").isEmpty());
        assertTrue(testParser.parse("   ").isEmpty());
        assertTrue(testParser.parse("\t  \u00A0").isEmpty());
    }

    @Test
    @DisplayName("메타 라인 무시")
    void ignore_meta_lines() {
        MultiPlatformKakaoParser testParser = new MultiPlatformKakaoParser();
        assertTrue(testParser.parse("채팅방 제목: 테스트방").isEmpty());
        assertTrue(testParser.parse("저장 일시: 2025-08-03").isEmpty());
        assertTrue(testParser.parse("저장된 메시지: 100개").isEmpty());
    }

    @Test
    @DisplayName("잘못된 형식은 파싱 실패")
    void parse_invalid_format() {
        MultiPlatformKakaoParser testParser = new MultiPlatformKakaoParser();
        
        // 잘못된 형식들을 여러 번 시도해도 파싱되지 않음
        for (int i = 0; i < 60; i++) {
            assertTrue(testParser.parse("잘못된 형식의 메시지").isEmpty());
            assertTrue(testParser.parse("2025년 8월 3일").isEmpty());
            assertTrue(testParser.parse("홍길동: 메시지").isEmpty());
        }
    }

    @Test
    @DisplayName("플랫폼별 호환성 테스트 - Android 형식")
    void compatibility_android_format() {
        String[] androidLines = {
            "2025년 8월 3일 오후 4:00, 홍길동 : 안녕하세요",
            "2025년 12월 31일 오전 11:59, 홍길동 : 연말인사",
            "2025년 1월 1일 오전 12:00, 홍길동 : 새해복많이"
        };

        for (String line : androidLines) {
            MessageRaw result = parseTestLine(line);
            assertNotNull(result, "Android 형식 파싱 실패: " + line);
        }
    }

    @Test
    @DisplayName("플랫폼별 호환성 테스트 - iOS 형식")
    void compatibility_ios_format() {
        String[] iosLines = {
            "2025. 8. 3. 16:00, 홍길동 : 안녕하세요",
            "2025. 12. 31. 23:59, 홍길동 : 연말인사",
            "2025. 1. 1. 0:00, 홍길동 : 새해복많이"
        };

        for (String line : iosLines) {
            MessageRaw result = parseTestLine(line);
            assertNotNull(result, "iOS 형식 파싱 실패: " + line);
        }
    }

    @Test
    @DisplayName("플랫폼별 호환성 테스트 - PC 형식")
    void compatibility_pc_format() {
        String dateHeader = "--- 2025년 8월 3일 토요일 ---";
        String[] pcLines = {
            "[홍길동] [오후 4:00] 안녕하세요",
            "[홍길동] [오후 11:59] 연말인사",
            "[홍길동] [오전 12:00] 새해복많이"
        };

        for (String line : pcLines) {
            MessageRaw result = parsePCTestLine(dateHeader, line);
            assertNotNull(result, "PC 형식 파싱 실패: " + line);
        }
    }

    @Test
    @DisplayName("스트리밍 파싱 테스트")
    void streaming_parsing_test() {
        MultiPlatformKakaoParser testParser = new MultiPlatformKakaoParser();
        String[] lines = {
            "2025년 8월 3일 오후 4:00, 홍길동 : 첫 번째 메시지",
            "2025년 8월 3일 오후 4:01, 홍길동 : 두 번째 메시지"
        };

        List<MessageRaw> messages = new ArrayList<>();
        
        // 각 라인 처리 (스니핑 포함)
        for (String line : lines) {
            for (int i = 0; i < 30; i++) {
                var result = testParser.accept(line);
                if (result.isPresent()) {
                    messages.add(result.get());
                }
            }
        }
        
        // 마지막 메시지 flush
        var lastResult = testParser.flush();
        if (lastResult.isPresent()) {
            messages.add(lastResult.get());
        }
        
        // 결과 검증
        assertTrue(messages.size() >= 2, "파싱된 메시지 수가 부족합니다: " + messages.size());
        
        // 첫 번째와 마지막 메시지 검증
        MessageRaw firstMsg = messages.get(0);
        MessageRaw lastMsg = messages.get(messages.size() - 1);
        
        assertEquals("홍길동", firstMsg.speakerName());
        assertEquals("홍길동", lastMsg.speakerName());
    }
}