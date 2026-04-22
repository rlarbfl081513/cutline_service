package com.ssafya408.cutlineparsing.service;


import com.ssafya408.cutlineparsing.util.Speaker;
import com.ssafya408.cutlineparsing.util.accumulator.MonthlyStatsAccumulator;
import com.ssafya408.cutlineparsing.util.accumulator.MonthlyStatsResult;
import com.ssafya408.cutlineparsing.util.dsl.DslBuilder;
import com.ssafya408.cutlineparsing.util.dsl.DslEvent;
import com.ssafya408.cutlineparsing.util.guard.GuardConfig;
import com.ssafya408.cutlineparsing.util.guard.TwoPersonGuard;
import com.ssafya408.cutlineparsing.util.parser.MultiPlatformKakaoParser;
import com.ssafya408.cutlineparsing.util.parser.MessageRaw;
import com.ssafya408.cutlineparsing.util.parser.SystemFilters;
import com.ssafya408.cutlineparsing.util.yearmonth.MonthRouter;
import com.ssafya408.cutlineparsing.util.yearmonth.Turn;
import com.ssafya408.cutlineparsing.util.yearmonth.TurnAccumulator;
import com.ssafya408.cutlineparsing.util.yearmonth.TwelveMonthWindow;

import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 원패스 코어 구현체 - 카카오톡 대화 텍스트를 한 번의 패스로 월별 DSL과 통계로 변환
 * 
 * <p>이 클래스는 전체 파싱 파이프라인을 조율하는 핵심 컴포넌트입니다.
 * 텍스트 입력부터 최종 월별 결과까지의 전 과정을 단일 패스로 처리합니다.</p>
 * 
 * <h3>처리 파이프라인:</h3>
 * <ol>
 *   <li><b>파싱:</b> MultiPlatformKakaoParser로 텍스트 → MessageRaw</li>
 *   <li><b>필터링:</b> 12개월 윈도우 + TwoPersonGuard + SystemFilters</li>
 *   <li><b>월별 통계:</b> MonthlyStatsAccumulator로 실시간 집계</li>
 *   <li><b>턴 생성:</b> TurnAccumulator로 연속 메시지 그룹화</li>
 *   <li><b>월별 라우팅:</b> MonthRouter로 월별 분리</li>
 *   <li><b>DSL 생성:</b> DslBuilder로 AI 분석용 형식 변환</li>
 * </ol>
 * 
 * @author AI Assistant
 * @version 2.0 (MultiPlatformKakaoParser 적용)
 */
@Service
@Slf4j
public final class OnePassCoreImpl implements OnePassCore {

    /** 한국 표준시 (KST) - 모든 시간 처리에 사용 */
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /**
     * 카카오톡 대화 텍스트를 월별 DSL과 통계로 변환하는 메인 메서드
     * 
     * <p>전체 파싱 파이프라인을 실행하여 입력 텍스트를 월별로 분석한 결과를 반환합니다.
     * 모든 처리는 단일 패스로 이루어지며, 메모리 효율성을 위해 스트리밍 방식을 사용합니다.</p>
     * 
     * @param text 파싱할 카카오톡 대화 텍스트 (전체 내보내기 파일 내용)
     * @param meDisplayName 사용자(나)의 표시 이름
     * @param friendDisplayName 상대방의 표시 이름
     * @return 월별 분석 결과 리스트 (DSL + 통계)
     * @throws IllegalArgumentException 입력값이 유효하지 않은 경우
     */
    @Override
    public List<MonthOutput> run(String text, String meDisplayName, String friendDisplayName) {
        log.info("[ OnePassCore 시작 ] >>> 텍스트 크기: {} bytes, 사용자: {}, 상대방: {}", 
                text != null ? text.length() : 0, meDisplayName, friendDisplayName);
        
        if (text == null || text.isBlank()) {
            log.warn("[ OnePassCore ] >>> 입력 텍스트가 비어있습니다.");
            return List.of();
        }

        // 월별 분석 시작 로깅
        log.info("[ 월별 분석 시작 ] >>> 사용자: {}, 상대방: {}", meDisplayName, friendDisplayName);

        // 1) 파서/가드/윈도우/누적기 준비
        final MultiPlatformKakaoParser parser = new MultiPlatformKakaoParser();
        log.info("[ 파서 설정 ] >>> 사용 중인 파서: MultiPlatformKakaoParser");
        
        final TwoPersonGuard guard = new TwoPersonGuard(new GuardConfig(meDisplayName, friendDisplayName));
        log.info("[ 가드 설정 ] >>> 사용자: {}, 상대방: {}", meDisplayName, friendDisplayName);
        
        final TurnAccumulator turnAcc = new TurnAccumulator();
        final TwelveMonthWindow win = new TwelveMonthWindow(Clock.system(KST));
        log.info("[ 윈도우 설정 ] >>> 12개월 윈도우 활성화");

        final Map<YearMonth, MonthlyStatsAccumulator> statsByMonth = new HashMap<>();

        // 공통 소비 로직: 윈도우 → 가드 → 월별 통계(onMessage) → 턴 누적
        final Consumer<MessageRaw> consume = msg -> {
            log.debug("[ 메시지 처리 ] >>> 시간: {}, 화자: {}, 본문길이: {}", 
                    msg.dt(), msg.speakerName(), msg.text().length());
            
            if (!win.within(msg.dt())) {
                log.debug("[ 윈도우 필터 ] >>> 메시지 제외 - 시간: {}", msg.dt());
                return;
            }
            
            var guardResult = guard.acceptSpeaker(msg.speakerName());
            if (!guardResult.ok()) {
                log.debug("[ 가드 필터 ] >>> 화자 제외 - 화자: {}, 결과: {}", msg.speakerName(), guardResult);
                return;
            }

            // ★ 시스템/삭제 본문 컷
            if (SystemFilters.isDrop(msg.text())) {
                log.debug("[ 시스템 필터 ] >>> 메시지 제외 - 본문: {}", msg.text());
                return;
            }

            final Speaker uf = guard.mapToUF(msg.speakerName());
            final YearMonth ym = YearMonth.from(msg.dt());
            log.debug("[ 메시지 수용 ] >>> 화자: {}, 월: {}, 본문: {}", uf, ym, msg.text());

            statsByMonth.computeIfAbsent(ym, MonthlyStatsAccumulator::new)
                    .onMessage(uf, msg.dt(), msg.text());

            turnAcc.ingest(uf, msg.dt(), msg.text());
        };

        // 2) 멀티라인 입력 스트리밍 파싱
        String[] lines = text.split("\\R");
        log.info("[ 파싱 시작 ] >>> 총 라인 수: {}", lines.length);
        
        long parseStartTime = System.currentTimeMillis();
        int processedLines = 0;
        int parsedMessages = 0;
        for (String line : lines) {
            processedLines++;
            
            // 진행률 로깅 (매 1000라인마다)
            if (processedLines % 1000 == 0) {
                log.info("[ 파싱 진행률 ] >>> 처리: {}/{} 라인 ({}%), 파싱된 메시지: {}", 
                        processedLines, lines.length, (processedLines * 100 / lines.length), parsedMessages);
            }
            
            var messageOpt = parser.accept(line);
            if (messageOpt.isPresent()) {
                parsedMessages++;
                log.trace("[ 메시지 파싱 ] >>> 성공 - 총 {}개", parsedMessages);
                consume.accept(messageOpt.get());
            }
        }
        var lastMessageOpt = parser.flush();
        if (lastMessageOpt.isPresent()) {
            parsedMessages++;
            log.debug("[ 마지막 메시지 ] >>> flush로 추가 메시지 파싱 완료");
            consume.accept(lastMessageOpt.get());
        }
        
        long parseElapsedTime = System.currentTimeMillis() - parseStartTime;
        log.info("[ 파싱 완료 ] >>> 처리된 라인 수: {}, 파싱된 메시지 수: {}, 소요시간: {}ms", 
                processedLines, parsedMessages, parseElapsedTime);
        log.info("[ 파싱 성능 ] >>> 처리 속도: {:.1f} 라인/초, {:.1f} 메시지/초", 
                processedLines * 1000.0 / parseElapsedTime, parsedMessages * 1000.0 / parseElapsedTime);

        // 3) 턴 → 월 라우팅
        final List<Turn> turns = turnAcc.flush();
        log.info("[ 턴 처리 ] >>> 총 턴 수: {}", turns.size());
        
        final Map<YearMonth, List<Turn>> byMonth = new MonthRouter().route(turns);
        log.info("[ 월별 라우팅 ] >>> 처리된 월 수: {}", byMonth.size());

        // 4) 월별 DSL + Stats 산출
        final List<MonthOutput> out = new ArrayList<>();
        final DslBuilder dslBuilder = new DslBuilder();
        log.info("[ DSL 생성 시작 ] >>> 월별 결과 생성");

        for (Map.Entry<YearMonth, List<Turn>> e : byMonth.entrySet()) {
            final YearMonth ym = e.getKey();
            final List<Turn> monthTurns = e.getValue();
            log.debug("[ DSL 생성 ] >>> 월: {}, 턴 수: {}", ym, monthTurns.size());

            // DSL: Turn → DslEvent
            final List<DslEvent> events = monthTurns.stream()
                    .map(t -> new DslEvent(t.speaker(), t.start(), t.text()))
                    .collect(Collectors.toList());

            final String dsl = dslBuilder.build(events);

            // Stats: 메시지 누적 + 턴 기반 U→F 응답합(분)
            final MonthlyStatsAccumulator acc =
                    statsByMonth.getOrDefault(ym, new MonthlyStatsAccumulator(ym));
            final MonthlyStatsAccumulator.ReplyStats replyStats = acc.computeReplyStats(monthTurns);
            final MonthlyStatsResult stats = acc.finalizeMonth(replyStats);

            out.add(new MonthOutput(ym, dsl, stats));
        }

        out.sort(Comparator.comparing(MonthOutput::ym));
        
        // 월별 분석 완료 로깅
        log.info("[ OnePassCore 완료 ] >>> 총 {} 개월 처리 완료", out.size());
        if (!out.isEmpty()) {
            log.info("[ OnePassCore 결과 ] >>> 처리 범위: {} ~ {}", 
                    out.get(0).ym(), out.get(out.size() - 1).ym());
        }
        
        return out;
    }
}
