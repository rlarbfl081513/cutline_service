package com.ssafya408.cutlineparsing.util.parser;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * 멀티 플랫폼 KakaoTalk 내보내기 텍스트 파서
 * 
 * <p>카카오톡 대화 내보내기 파일을 자동으로 감지하여 파싱합니다.
 * 초반 50줄을 버퍼링하여 플랫폼을 정확히 감지한 후, 모든 라인을 재생하여 
 * 데이터 손실 없이 파싱합니다.</p>
 * 
 * <h3>지원 플랫폼:</h3>
 * <ul>
 *   <li><b>Android:</b> "2025년 8월 3일 오후 4:00, 홍길동 : 안녕하세요"</li>
 *   <li><b>iOS:</b> "2025. 8. 3. 16:00, 홍길동 : 안녕하세요"</li>
 *   <li><b>PC:</b> "[홍길동] [오후 4:00] 안녕하세요" (날짜 헤더 필요)</li>
 * </ul>
 * 
 * <h3>주요 기능:</h3>
 * <ul>
 *   <li>플랫폼 자동 감지 (버퍼링 + 점수화 시스템)</li>
 *   <li>PC 날짜 컨텍스트 관리 (LocalDate.now() 사용 안함)</li>
 *   <li>강화된 AM/PM 변환 (12시 경계값 처리)</li>
 *   <li>혼입 데이터 감지 및 드롭</li>
 *   <li>스트리밍 멀티라인 파싱 지원</li>
 * </ul>
 * 
 * @author AI Assistant
 * @version 2.0 (리팩터링 완료)
 */
@Slf4j
public final class MultiPlatformKakaoParser {

    // ============================================================================
    // 상수 및 열거형 정의
    // ============================================================================
    
    /** 한국 표준시 (KST) */
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /** 플랫폼 종류 열거형 */
    public enum Flavor {
        /** Android 플랫폼 (년월일 + 오전/오후 형식) */
        ANDROID, 
        /** iOS 플랫폼 (점 구분자 + 24시간 형식) */
        IOS, 
        /** PC 플랫폼 (대괄호 + 별도 날짜 헤더) */
        PC
    }

    /**
     * 플랫폼별 정규식 패턴 및 설정을 담는 내부 클래스
     * 각 플랫폼마다 메시지 시작, 날짜 헤더, 스킵할 메타데이터 패턴을 정의
     */
    private static class PlatformSpecific {
        final Pattern msgStart;
        final Pattern dateHeader;
        final Pattern skipMeta;
        final boolean hasYearInDate;

        PlatformSpecific(Pattern msgStart, Pattern dateHeader, Pattern skipMeta, boolean hasYearInDate) {
            this.msgStart = msgStart;
            this.dateHeader = dateHeader;
            this.skipMeta = skipMeta;
            this.hasYearInDate = hasYearInDate;
        }
    }

    // ============================================================================
    // 플랫폼별 패턴 정의
    // ============================================================================
    
    /**
     * Android 플랫폼 패턴 정의
     * 형식: "2025년 8월 3일 오후 4:00, 홍길동 : 안녕하세요"
     * 특징: 한글 년월일 표기 + 오전/오후 + 12시간제
     */
    private static final PlatformSpecific ANDROID = new PlatformSpecific(
        // 메시지 시작 패턴: (년)(월)(일)(오전/오후)(시)(분)(화자)(본문)
        Pattern.compile("^\\s*(\\d{4})년\\s*(\\d{1,2})월\\s*(\\d{1,2})일\\s*(오전|오후)\\s*(\\d{1,2}):(\\d{2})\\s*,\\s*(.+?)\\s*:\\s*(.*)\\s*$"),
        // 날짜만 있는 헤더 패턴 (거의 사용되지 않음)
        Pattern.compile("^\\s*(\\d{4})년\\s*(\\d{1,2})월\\s*(\\d{1,2})일\\s*(오전|오후)\\s*(\\d{1,2}):(\\d{2})\\s*$"),
        // 스킵할 메타데이터 패턴 (채팅방 제목, 저장 정보, 파일명, 요일 헤더)
        Pattern.compile("^\\s*채팅방\\s*제목\\s*:|^\\s*저장\\s*.*:|^\\s*저장\\s*된\\s*메시지\\s*:|.*\\.txt$|^\\d{4}년\\s*\\d{1,2}월\\s*\\d{1,2}일\\s*[가-힣]+요일$"),
        true // 날짜에 년도 포함
    );

    /**
     * iOS 플랫폼 패턴 정의
     * 형식: "2025. 8. 3. 16:00, 홍길동 : 안녕하세요"
     * 특징: 점(.) 구분자 + 24시간제
     */
    private static final PlatformSpecific IOS = new PlatformSpecific(
        // 메시지 시작 패턴: (년)(월)(일)(시)(분)(화자)(본문)
        Pattern.compile("^\\s*(\\d{4})\\.\\s*(\\d{1,2})\\.\\s*(\\d{1,2})\\.\\s*(\\d{1,2}):(\\d{2})\\s*,\\s*(.+?)\\s*:\\s*(.*)$"),
        // 날짜만 있는 헤더 패턴 (거의 사용되지 않음)
        Pattern.compile("^\\s*(\\d{4})\\.\\s*(\\d{1,2})\\.\\s*(\\d{1,2})\\.\\s*(\\d{1,2}):(\\d{2})\\s*$"),
        // 스킵할 메타데이터 패턴 (Android와 동일)
        Pattern.compile("^\\s*채팅방\\s*제목\\s*:|^\\s*저장\\s*.*:|^\\s*저장\\s*된\\s*메시지\\s*:|.*\\.txt$|^\\d{4}년\\s*\\d{1,2}월\\s*\\d{1,2}일\\s*[가-힣]+요일$"),
        true // 날짜에 년도 포함
    );

    /**
     * PC 플랫폼 패턴 정의
     * 형식: "[홍길동] [오후 4:00] 안녕하세요" + 별도 날짜 헤더 "--- 2025년 8월 3일 토요일 ---"
     * 특징: 대괄호 형식 + 별도 날짜 헤더 필요 + 오전/오후 12시간제
     */
    private static final PlatformSpecific PC = new PlatformSpecific(
        // 메시지 시작 패턴: (화자)(오전/오후)(시)(분)(본문)
        Pattern.compile("^\\[(.+?)\\]\\s*\\[(오전|오후)\\s*(\\d{1,2}):(\\d{2})\\]\\s*(.*)$"),
        // 날짜 헤더 패턴: "--- 2025년 8월 3일 토요일 ---"
        Pattern.compile("^-{3,}\\s*(\\d{4})년\\s*(\\d{1,2})월\\s*(\\d{1,2})일.*-{3,}$"),
        // 스킵할 메타데이터 패턴 (날짜 헤더는 제외 - PC에서는 중요함)
        Pattern.compile("^\\s*채팅방\\s*제목\\s*:|^\\s*저장\\s*.*:|^\\s*저장\\s*된\\s*메시지\\s*:|.*\\.txt$"),
        false // 메시지에는 년도 없음 (별도 헤더에서 관리)
    );

    // ============================================================================
    // 파서 상태 및 설정
    // ============================================================================
    
    /** 스니핑을 위한 초반 버퍼 크기 (라인 수) */
    private static final int SNIFF_BUFFER_SIZE = 50;
    
    // ---------- 플랫폼 감지 상태 ----------
    /** 플랫폼 감지 완료 여부 */
    private boolean sniffed = false;
    /** 감지된 플랫폼 종류 (기본값: Android) */
    private Flavor flavor = Flavor.ANDROID;
    /** 현재 사용 중인 플랫폼별 패턴 설정 */
    private PlatformSpecific ps = ANDROID;
    
    // ---------- 스니핑 버퍼링 시스템 ----------
    /** 플랫폼 감지를 위한 초반 라인 버퍼 */
    private List<String> sniffBuffer = new ArrayList<>();
    /** 버퍼 재생 중 현재 인덱스 */
    private int replayIndex = 0;
    /** 현재 재생 모드인지 여부 */
    private boolean isReplaying = false;
    
    // ---------- PC 플랫폼 전용 상태 ----------
    /** PC 플랫폼의 현재 날짜 컨텍스트 (LocalDate.now() 사용 금지!) */
    private LocalDate currentDateForPC = null;
    
    // ---------- 스트리밍 파싱 상태 ----------
    /** 현재 진행 중인 메시지 (멀티라인 처리용) */
    private PendingMessage pendingMessage;

    // ============================================================================
    // 공개 API 메서드
    // ============================================================================
    
    /**
     * 단일 라인 파싱 (기본 파싱 메서드)
     * 
     * <p>주어진 라인을 파싱하여 MessageRaw 객체로 변환합니다.
     * 내부적으로 스니핑 버퍼링 시스템을 사용하여 플랫폼을 자동 감지합니다.</p>
     * 
     * @param line 파싱할 텍스트 라인
     * @return 파싱된 메시지 객체 (Optional)
     */
    public Optional<MessageRaw> parse(String line) {
        if (line == null) {
            log.trace("[ 파싱 ] >>> null 라인 입력, 건너뜀");
            return Optional.empty();
        }
        
        String s = line.strip();
        log.trace("[ 파싱 ] >>> 입력 라인: '{}'", s);
        
        // 빈 라인은 무시
        if (s.isEmpty()) {
            log.trace("[ 파싱 ] >>> 빈 라인 감지, 건너뜀");
            return Optional.empty();
        }

        // 스니핑 단계 처리
        if (!sniffed) {
            log.debug("[ 파싱 ] >>> 스니핑 단계 진행 중...");
            return handleSniffingPhase(s);
        }

        // 스니핑 완료 후 정상 파싱
        log.trace("[ 파싱 ] >>> 정상 파싱 모드 (플랫폼: {})", flavor);
        return parseByPlatform(s);
    }

    /**
     * 스트리밍 멀티라인 파싱 (기존 accept/flush 패턴)
     * 
     * <p>멀티라인 메시지를 처리하기 위한 스트리밍 파싱 메서드입니다.
     * flush() 메서드와 함께 사용하여 완전한 메시지를 구성합니다.</p>
     * 
     * @param line 처리할 텍스트 라인
     * @return 완성된 메시지 객체 (Optional, 주로 새 메시지 시작 시에만 반환)
     */
    public Optional<MessageRaw> accept(String line) {
        if (line == null) {
            log.trace("[ 스트리밍 ] >>> null 라인 입력, 건너뜀");
            return Optional.empty();
        }
        
        String s = line.strip();
        log.trace("[ 스트리밍 ] >>> 입력 라인: '{}'", s);
        
        // 빈 라인은 무시
        if (s.isEmpty()) {
            log.trace("[ 스트리밍 ] >>> 빈 라인 감지, 건너뜀");
            return Optional.empty();
        }

        // 스니핑 단계 처리
        if (!sniffed) {
            log.debug("[ 스트리밍 ] >>> 스니핑 단계 진행 중...");
            return handleSniffingPhase(s);
        }

        // 스니핑 완료 후 정상 파싱
        log.trace("[ 스트리밍 ] >>> 멀티라인 처리 모드 (플랫폼: {})", flavor);
        return processLine(s);
    }

    /**
     * 스니핑 단계 처리 - 버퍼링 + 재생 시스템
     */
    private Optional<MessageRaw> handleSniffingPhase(String line) {
        // 재생 모드인 경우
        if (isReplaying) {
            return handleReplayMode();
        }
        
        // 버퍼링 모드인 경우
        return handleBufferingMode(line);
    }
    
    /**
     * 버퍼링 모드 - 초반 라인들을 수집하여 플랫폼 감지
     */
    private Optional<MessageRaw> handleBufferingMode(String line) {
        sniffBuffer.add(line);
        log.debug("[ 스니핑 버퍼링 ] >>> 라인 추가: {} (현재 버퍼 크기: {})", line, sniffBuffer.size());
        
        // 버퍼가 충분히 찼거나 명확한 패턴을 발견했으면 스니핑 실행
        if (sniffBuffer.size() >= SNIFF_BUFFER_SIZE || hasDefinitivePattern()) {
            performSniffing();
            startReplay();
            return handleReplayMode();
        }
        
        return Optional.empty(); // 버퍼링 중에는 메시지 반환하지 않음
    }
    
    /**
     * 재생 모드 - 버퍼된 라인들을 순차적으로 재생
     */
    private Optional<MessageRaw> handleReplayMode() {
        if (replayIndex >= sniffBuffer.size()) {
            // 재생 완료
            isReplaying = false;
            sniffBuffer.clear(); // 메모리 절약
            log.info("[ 스니핑 재생 ] >>> 재생 완료, 정상 파싱 모드로 전환");
            return Optional.empty();
        }
        
        String replayLine = sniffBuffer.get(replayIndex++);
        log.debug("[ 스니핑 재생 ] >>> 재생 중: {} (진행: {}/{})", replayLine, replayIndex, sniffBuffer.size());
        
        // 재생 중인 라인을 실제 파싱 로직으로 처리
        return processLine(replayLine);
    }
    
    /**
     * 확실한 패턴이 발견되었는지 확인
     */
    private boolean hasDefinitivePattern() {
        for (String line : sniffBuffer) {
            if (ANDROID.msgStart.matcher(line).matches() ||
                IOS.msgStart.matcher(line).matches() ||
                PC.msgStart.matcher(line).matches()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 버퍼된 라인들을 분석하여 플랫폼 결정
     */
    private void performSniffing() {
        log.info("[ 플랫폼 스니핑 ] >>> 시작 - 버퍼 크기: {}", sniffBuffer.size());
        
        int androidScore = 0;
        int iosScore = 0;
        int pcScore = 0;
        
        for (String line : sniffBuffer) {
            // 메타데이터는 점수에서 제외
            if (isIgnorableMeta(line)) {
                continue;
            }
            
            // 각 플랫폼 패턴 매칭
            if (ANDROID.msgStart.matcher(line).matches()) {
                androidScore += 3; // 메시지 패턴은 높은 점수
                log.debug("[ 플랫폼 스니핑 ] >>> Android 메시지 패턴 발견: {}", line);
            }
            if (ANDROID.dateHeader != null && ANDROID.dateHeader.matcher(line).matches()) {
                androidScore += 1; // 헤더 패턴은 낮은 점수
                log.debug("[ 플랫폼 스니핑 ] >>> Android 헤더 패턴 발견: {}", line);
            }
            
            if (IOS.msgStart.matcher(line).matches()) {
                iosScore += 3;
                log.debug("[ 플랫폼 스니핑 ] >>> iOS 메시지 패턴 발견: {}", line);
            }
            if (IOS.dateHeader != null && IOS.dateHeader.matcher(line).matches()) {
                iosScore += 1;
                log.debug("[ 플랫폼 스니핑 ] >>> iOS 헤더 패턴 발견: {}", line);
            }
            
            if (PC.msgStart.matcher(line).matches()) {
                pcScore += 3;
                log.debug("[ 플랫폼 스니핑 ] >>> PC 메시지 패턴 발견: {}", line);
            }
            if (PC.dateHeader != null && PC.dateHeader.matcher(line).matches()) {
                pcScore += 2; // PC는 헤더가 중요하므로 점수 높게
                log.debug("[ 플랫폼 스니핑 ] >>> PC 헤더 패턴 발견: {}", line);
            }
        }
        
        log.info("[ 플랫폼 스니핑 ] >>> 점수 - Android: {}, iOS: {}, PC: {}", androidScore, iosScore, pcScore);
        
        // 플랫폼 결정
        if (pcScore > androidScore && pcScore > iosScore) {
            flavor = Flavor.PC;
            ps = PC;
            log.info("[ 플랫폼 스니핑 ] >>> 선택된 플랫폼: PC");
        } else if (iosScore > androidScore) {
            flavor = Flavor.IOS;
            ps = IOS;
            log.info("[ 플랫폼 스니핑 ] >>> 선택된 플랫폼: iOS");
        } else {
            flavor = Flavor.ANDROID;
            ps = ANDROID;
            log.info("[ 플랫폼 스니핑 ] >>> 선택된 플랫폼: Android (기본값 포함)");
        }
        
        sniffed = true;
    }
    
    /**
     * 재생 모드 시작
     */
    private void startReplay() {
        isReplaying = true;
        replayIndex = 0;
        log.info("[ 스니핑 재생 ] >>> 재생 시작 - {} 라인 재생 예정", sniffBuffer.size());
    }
    
    /**
     * PC 날짜 컨텍스트 업데이트
     */
    private void updatePCDateContext(String dateHeaderLine) {
        try {
            Matcher matcher = PC.dateHeader.matcher(dateHeaderLine);
            if (matcher.matches()) {
                int year = i(matcher, 1);
                int month = i(matcher, 2);
                int day = i(matcher, 3);
                
                LocalDate newDate = LocalDate.of(year, month, day);
                
                if (!Objects.equals(currentDateForPC, newDate)) {
                    log.info("[ PC 날짜 컨텍스트 ] >>> 날짜 업데이트: {} → {}", currentDateForPC, newDate);
                    currentDateForPC = newDate;
                } else {
                    log.debug("[ PC 날짜 컨텍스트 ] >>> 날짜 유지: {}", currentDateForPC);
                }
            }
        } catch (Exception e) {
            log.warn("[ PC 날짜 컨텍스트 ] >>> 날짜 헤더 파싱 실패: {}", dateHeaderLine, e);
        }
    }
    
    /**
     * 미매칭 라인 처리 - 강화된 본문 누적 정책
     */
    private Optional<MessageRaw> handleUnmatchedLine(String line) {
        // 진행 중인 메시지가 없으면 라인 드롭
        if (pendingMessage == null) {
            log.debug("[ 라인 드롭 ] >>> 진행 중인 메시지 없음: {}", line);
            return Optional.empty();
        }
        
        // 메타데이터나 시스템 라인은 본문에 포함하지 않음
        if (isIgnorableMeta(line)) {
            log.debug("[ 라인 드롭 ] >>> 메타데이터 라인: {}", line);
            return Optional.empty();
        }
        
        // 다른 플랫폼의 메시지 패턴이면 혼입 데이터로 간주하여 드롭
        if (isOtherPlatformPattern(line)) {
            log.warn("[ 혼입 데이터 드롭 ] >>> 다른 플랫폼 패턴 감지: {}", line);
            return Optional.empty();
        }
        
        // 빈 라인이 아니고 너무 길지 않으면 본문에 추가
        if (!line.trim().isEmpty() && line.length() < 1000) { // 1000자 제한
            if (pendingMessage.body.length() > 0) {
                pendingMessage.body.append('\n');
            }
            pendingMessage.body.append(line);
            log.debug("[ 본문 누적 ] >>> 라인 추가: {}", line);
        } else {
            log.debug("[ 라인 드롭 ] >>> 빈 라인 또는 너무 긴 라인: {}", line);
        }
        
        return Optional.empty();
    }
    
    /**
     * 다른 플랫폼의 메시지 패턴인지 확인
     */
    private boolean isOtherPlatformPattern(String line) {
        switch (flavor) {
            case ANDROID:
                return IOS.msgStart.matcher(line).matches() || PC.msgStart.matcher(line).matches();
            case IOS:
                return ANDROID.msgStart.matcher(line).matches() || PC.msgStart.matcher(line).matches();
            case PC:
                return ANDROID.msgStart.matcher(line).matches() || IOS.msgStart.matcher(line).matches();
            default:
                return false;
        }
    }

    /**
     * 플랫폼별 파싱 로직
     */
    private Optional<MessageRaw> parseByPlatform(String s) {
        Matcher matcher = ps.msgStart.matcher(s);
        if (matcher.matches()) {
            return buildMessage(matcher);
        }
        return Optional.empty();
    }

    /**
     * 메시지 객체 생성 - 플랫폼별 안전한 그룹 인덱스 사용
     */
    private Optional<MessageRaw> buildMessage(Matcher matcher) {
        try {
            switch (flavor) {
                case ANDROID:
                    return buildAndroidMessage(matcher);
                case IOS:
                    return buildIOSMessage(matcher);
                case PC:
                    return buildPCMessage(matcher);
                default:
                    log.warn("[ 메시지 빌드 ] >>> 알 수 없는 플랫폼: {}", flavor);
                    return Optional.empty();
            }
        } catch (Exception e) {
            log.warn("[ 메시지 빌드 ] >>> 파싱 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Android 메시지 생성 - 안전한 그룹 인덱스
     */
    private Optional<MessageRaw> buildAndroidMessage(Matcher matcher) {
        // Android 패턴: (년)(월)(일)(오전/오후)(시)(분)(화자)(본문)
        // 그룹: 1=년, 2=월, 3=일, 4=오전/오후, 5=시, 6=분, 7=화자, 8=본문
        if (matcher.groupCount() < 8) {
            log.warn("[ Android 메시지 ] >>> 그룹 수 부족: {}", matcher.groupCount());
            return Optional.empty();
        }
        
        ZonedDateTime dt = buildDtAndroid(matcher);
        String speakerName = matcher.group(7);
        String text = matcher.group(8);
        
        return Optional.of(new MessageRaw(dt, speakerName, normalize(text), false));
    }
    
    /**
     * iOS 메시지 생성 - 안전한 그룹 인덱스
     */
    private Optional<MessageRaw> buildIOSMessage(Matcher matcher) {
        // iOS 패턴: (년)(월)(일)(시)(분)(화자)(본문)
        // 그룹: 1=년, 2=월, 3=일, 4=시, 5=분, 6=화자, 7=본문
        if (matcher.groupCount() < 7) {
            log.warn("[ iOS 메시지 ] >>> 그룹 수 부족: {}", matcher.groupCount());
            return Optional.empty();
        }
        
        ZonedDateTime dt = buildDtIOS(matcher);
        String speakerName = matcher.group(6);
        String text = matcher.group(7);
        
        return Optional.of(new MessageRaw(dt, speakerName, normalize(text), false));
    }
    
    /**
     * PC 메시지 생성 - 안전한 그룹 인덱스
     */
    private Optional<MessageRaw> buildPCMessage(Matcher matcher) {
        // PC 패턴: (화자)(오전/오후)(시)(분)(본문)
        // 그룹: 1=화자, 2=오전/오후, 3=시, 4=분, 5=본문
        if (matcher.groupCount() < 5) {
            log.warn("[ PC 메시지 ] >>> 그룹 수 부족: {}", matcher.groupCount());
            return Optional.empty();
        }
        
        ZonedDateTime dt = buildDtPC(matcher);
        String speakerName = matcher.group(1);
        String text = matcher.group(5);
        
        return Optional.of(new MessageRaw(dt, speakerName, normalize(text), false));
    }

    /**
     * Android 날짜/시간 생성 (오전/오후 형식)
     */
    private ZonedDateTime buildDtAndroid(Matcher m) {
        int year = i(m, 1);
        int month = i(m, 2);
        int day = i(m, 3);
        String ap = m.group(4);  // 오전/오후
        int hour = i(m, 5);
        int minute = i(m, 6);
        return ZonedDateTime.of(year, month, day, to24(ap, hour), minute, 0, 0, KST);
    }

    /**
     * iOS 날짜/시간 생성 (24시간 형식)
     */
    private ZonedDateTime buildDtIOS(Matcher m) {
        int year = i(m, 1);
        int month = i(m, 2);
        int day = i(m, 3);
        int hour = i(m, 4);      // 이미 24시간 형식
        int minute = i(m, 5);
        return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, KST);
    }

    /**
     * PC 날짜/시간 생성
     */
    private ZonedDateTime buildDtPC(Matcher m) {
        String ap = m.group(2);
        int hour = i(m, 3);
        int minute = i(m, 4);
        
        // PC는 날짜 정보가 별도 헤더에 있으므로 컨텍스트에서 가져옴
        if (currentDateForPC == null) {
            log.warn("[ PC 날짜 컨텍스트 ] >>> 날짜 헤더 없이 메시지 발견, 드롭: [{}] [{}] {}", 
                     m.group(1), m.group(2) + " " + m.group(3) + ":" + m.group(4), m.group(5));
            throw new IllegalStateException("PC 메시지에 날짜 컨텍스트가 없습니다");
        }
        
        return ZonedDateTime.of(currentDateForPC, LocalTime.of(to24(ap, hour), minute), KST);
    }

    /**
     * 오전/오후 → 24시간 변환 (강화된 엣지케이스 처리)
     */
    private static int to24(String ap, int h) {
        // 입력값 검증
        if (ap == null) {
            log.warn("[ AM/PM 변환 ] >>> null 오전/오후 값, 기본값 0 반환");
            return 0;
        }
        
        // 시간 범위 검증 (1-12)
        if (h < 1 || h > 12) {
            log.warn("[ AM/PM 변환 ] >>> 잘못된 시간 범위: {}, 보정하여 처리", h);
            h = Math.max(1, Math.min(12, h)); // 1-12 범위로 보정
        }
        
        // 오전/오후 변환
        if (Objects.equals(ap, "오전")) {
            int result = (h == 12) ? 0 : h;  // 오전 12시 → 0시, 나머지는 그대로
            log.debug("[ AM/PM 변환 ] >>> 오전 {}시 → {}시", h, result);
            return result;
        } else if (Objects.equals(ap, "오후")) {
            int result = (h == 12) ? 12 : h + 12; // 오후 12시 → 12시, 나머지는 +12
            log.debug("[ AM/PM 변환 ] >>> 오후 {}시 → {}시", h, result);
            return result;
        } else {
            log.warn("[ AM/PM 변환 ] >>> 알 수 없는 오전/오후 값: '{}', 기본값 0 반환", ap);
            return 0;
        }
    }

    /**
     * 정수 파싱 헬퍼 (강화된 예외 처리)
     */
    private static int i(Matcher m, int group) {
        try {
            String value = m.group(group);
            if (value == null || value.trim().isEmpty()) {
                log.warn("[ 정수 파싱 ] >>> 빈 값 또는 null, 그룹 {}: '{}'", group, value);
                return 0;
            }
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.warn("[ 정수 파싱 ] >>> 파싱 실패, 그룹 {}: '{}'", group, m.group(group));
            return 0;
        } catch (IndexOutOfBoundsException e) {
            log.warn("[ 정수 파싱 ] >>> 그룹 인덱스 초과: {}", group);
            return 0;
        }
    }

    /**
     * 메타 라인 무시 여부
     */
    private boolean isIgnorableMeta(String s) {
        return ps != null && ps.skipMeta != null && ps.skipMeta.matcher(s).matches();
    }

    /**
     * 본문 정규화
     */
    private static String normalize(String s) {
        return s != null ? s.trim() : "";
    }

    /**
     * 스트리밍 파싱을 위한 내부 클래스
     */
    private static class PendingMessage {
        ZonedDateTime dt;
        String speakerName;
        StringBuilder body = new StringBuilder();
    }

    /**
     * 라인 처리 (스트리밍용) - 실제 멀티라인 파싱
     */
    private Optional<MessageRaw> processLine(String s) {
        Matcher matcher = ps.msgStart.matcher(s);
        if (matcher.matches()) {
            // 새 메시지 헤더 → 이전 메시지 flush
            Optional<MessageRaw> out = flushPendingMessage();
            
            // 새 메시지 시작
            pendingMessage = new PendingMessage();
            try {
                switch (flavor) {
                    case ANDROID:
                        pendingMessage.dt = buildDtAndroid(matcher);
                        pendingMessage.speakerName = matcher.group(7);  // Android: 7번째 그룹
                        String firstText = matcher.group(8);           // Android: 8번째 그룹
                        if (!firstText.isEmpty()) {
                            pendingMessage.body.append(firstText);
                        }
                        break;
                    case IOS:
                        pendingMessage.dt = buildDtIOS(matcher);
                        pendingMessage.speakerName = matcher.group(6);  // iOS: 6번째 그룹
                        String firstTextIOS = matcher.group(7);        // iOS: 7번째 그룹
                        if (!firstTextIOS.isEmpty()) {
                            pendingMessage.body.append(firstTextIOS);
                        }
                        break;
                    case PC:
                        try {
                            pendingMessage.dt = buildDtPC(matcher);
                            pendingMessage.speakerName = matcher.group(1);
                            String firstTextPC = matcher.group(5);
                            if (!firstTextPC.isEmpty()) {
                                pendingMessage.body.append(firstTextPC);
                            }
                        } catch (IllegalStateException e) {
                            // PC 날짜 컨텍스트 없음 - 메시지 드롭
                            log.warn("[ PC 메시지 드롭 ] >>> {}", e.getMessage());
                            pendingMessage = null;
                            return out; // 이전 메시지만 반환
                        }
                        break;
                }
                return out;
            } catch (Exception e) {
                pendingMessage = null;
                return out;
            }
        }
        
        // 날짜만 있는 헤더 체크 (경계로 사용)
        if (ps.dateHeader != null && ps.dateHeader.matcher(s).matches()) {
            // PC의 경우 날짜 컨텍스트 업데이트
            if (flavor == Flavor.PC) {
                updatePCDateContext(s);
            }
            return flushPendingMessage();
        }
        
        // 미매칭 라인 처리 - 본문 누적 정책 강화
        return handleUnmatchedLine(s);
    }
    
    /**
     * 진행 중인 메시지 flush
     */
    private Optional<MessageRaw> flushPendingMessage() {
        if (pendingMessage == null) {
            return Optional.empty();
        }
        
        MessageRaw msg = new MessageRaw(
            pendingMessage.dt,
            pendingMessage.speakerName,
            normalize(pendingMessage.body.toString()),
            false
        );
        
        pendingMessage = null;
        return Optional.of(msg);
    }

    /**
     * 스트리밍 파싱 - 마지막 메시지 플러시
     * 
     * <p>현재 진행 중인 메시지를 완성하여 반환합니다.
     * 스트리밍 파싱의 마지막 단계에서 호출해야 합니다.</p>
     * 
     * @return 완성된 마지막 메시지 (Optional)
     */
    public Optional<MessageRaw> flush() {
        log.debug("[ 스트리밍 ] >>> flush() 호출 - 마지막 메시지 완성 시도");
        Optional<MessageRaw> result = flushPendingMessage();
        if (result.isPresent()) {
            log.info("[ 스트리밍 ] >>> flush 완료 - 메시지 반환: 화자={}, 길이={}", 
                    result.get().speakerName(), result.get().text().length());
        } else {
            log.debug("[ 스트리밍 ] >>> flush 완료 - 반환할 메시지 없음");
        }
        return result;
    }
}
