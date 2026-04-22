package com.ssafya408.cutlineparsing.service;

import com.ssafya408.cutlineparsing.common.entity.Person;
import com.ssafya408.cutlineparsing.common.entity.PersonValue;
import com.ssafya408.cutlineparsing.dao.PersonRepository;
import com.ssafya408.cutlineparsing.dao.PersonValueRepository;
import com.ssafya408.cutlineparsing.service.dto.AutoAnalysisResult;
import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 카카오톡 파싱 서비스 - 파일 업로드부터 AI 분석까지의 전체 워크플로우 관리
 * 
 * <p>이 서비스는 카카오톡 대화 파일을 업로드받아 파싱하고, 
 * 가상 스레드를 활용한 배치 AI 분석을 통해 최종 결과를 생성합니다.</p>
 * 
 * <h3>주요 기능:</h3>
 * <ul>
 *   <li><b>파일 파싱:</b> MultiPlatformKakaoParser를 통한 자동 플랫폼 감지 및 파싱</li>
 *   <li><b>배치 AI 분석:</b> Virtual Thread를 활용한 병렬 GPT 분석 (배치 크기: 12)</li>
 *   <li><b>데이터 관리:</b> Person 엔티티 업데이트 및 PersonValue 저장</li>
 *   <li><b>트랜잭션 관리:</b> 전체 프로세스의 ACID 보장</li>
 * </ul>
 * 
 * <h3>Virtual Thread 활용:</h3>
 * <p>Java 21의 Virtual Thread를 사용하여 AI API 호출을 병렬화합니다.
 * 기존 Platform Thread 대비 메모리 효율성과 확장성이 크게 향상되었습니다.</p>
 * 
 * @author AI Assistant
 * @version 2.0 (Virtual Thread + MultiPlatform 파서 적용)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ParsingService {

    private static final String DEFAULT_USER_DISPLAY_NAME = "홍길동";

    private final OnePassCore onePassCore;
    private final PersonRepository personRepository;
    private final PersonValueRepository personValueRepository;
    private final AutoAnalysisService autoAnalysisService;
    private final MonthlyContextBuilder monthlyContextBuilder;
    private final PersonUpdateCoordinator personUpdateCoordinator;

    @Transactional
    public void process(Long personId, MultipartFile file, String userDisplayName) throws IOException {
        long perfStartNanos = System.nanoTime();
        try {
            log.info("[ 파싱 시작 ] >>> personId: {}, 파일명: {}", personId, file != null ? file.getOriginalFilename() : "null");

            validateFile(file);

            Person person = personRepository.findById(personId)
                    .orElseThrow(() -> {
                        log.error("[ Person 조회 ] >>> 실패 - personId: {}", personId);
                        return new EntityNotFoundException("요청한 상대 정보를 찾을 수 없습니다.");
                    });

            String source = new String(file.getBytes(), StandardCharsets.UTF_8);
            log.info("[ 파싱 ] >>> 파일 내용 미리보기 (처음 500자): {}", 
                    source.length() > 500 ? source.substring(0, 500) + "..." : source);
            log.info("[ 파싱 ] >>> 사용자명: {}, 상대방명: {}", userDisplayName, person.getName());
            
            List<MonthOutput> outputs = new ArrayList<>(onePassCore.run(source, userDisplayName, person.getName()));
            logCharCompression(source, outputs);

            if (outputs.isEmpty()) {
                log.error("[ OnePassCore ] >>> 결과 없음 - 유효한 대화 메시지 없음");
                log.error("[ 디버깅 ] >>> 파일 크기: {} bytes, 사용자명: {}, 상대방명: {}", 
                        source.length(), userDisplayName, person.getName());
                throw new IllegalArgumentException("유효한 대화 메시지를 찾을 수 없습니다. 대화 파일과 화자 이름을 확인해 주세요.");
            }

            outputs.sort(Comparator.comparing(MonthOutput::ym));

            YearMonth earliestMonth = outputs.get(0).ym();
            PersonValue previousValue = personValueRepository.findLatestBefore(personId, earliestMonth).orElse(null);

            List<MonthlyComputationContext> contexts = monthlyContextBuilder.buildSequential(
                    outputs,
                    person,
                    previousValue,
                    userDisplayName,
                    person.getName()
            );

            personUpdateCoordinator.persistAndUpdate(person, contexts);

            log.info("[ 파싱 완료 ] >>> personId: {} 처리 완료", personId);
        } finally {
            long perfDurationMillis = (System.nanoTime() - perfStartNanos) / 1_000_000;
            log.info("[ 성능 측정 ] >>> 파싱 소요시간: {} ms", perfDurationMillis);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.error("[ 파일 검증 ] >>> 실패 - file null: {}, empty: {}",
                    file == null, file != null && file.isEmpty());
            throw new IllegalArgumentException("업로드된 파일이 비어 있습니다.");
        }
        log.info("[ 파일 검증 ] >>> 성공 - 파일크기: {} bytes", file.getSize());
    }

    /**
     * 파싱 전 원문 글자수와 파싱 후 DSL 총 글자수, 감소율을 로그로 남긴다.
     */
    private void logCharCompression(String source, List<MonthOutput> outputs) {
        if (source == null) {
            return;
        }
        int rawChars = source.length();
        int dslChars = 0;
        if (outputs != null) {
            for (MonthOutput mo : outputs) {
                if (mo != null && mo.dsl() != null) {
                    dslChars += mo.dsl().length();
                }
            }
        }
        double ratio = rawChars == 0 ? 0.0 : (dslChars * 1.0 / rawChars);
        double reducePct = rawChars == 0 ? 0.0 : (1.0 - ratio) * 100.0;
        log.info("[ 문자 압축 ] >>> 원문: {} chars, DSL: {} chars, 감소율: {:.2f}%", rawChars, dslChars, reducePct);
    }

    private List<AutoAnalysisResult> analyzeAutoAnalysisInBatches(List<MonthOutput> outputs,
                                                                  String userDisplayName,
                                                                  String friendDisplayName) {
        log.info("[ AI 분석 시작 ] >>> 총 월 수: {}, 사용자: {}, 상대방: {}", outputs.size(), userDisplayName, friendDisplayName);
        
        if (outputs.isEmpty()) {
            log.warn("[ AI 분석 ] >>> 분석할 월 데이터가 없습니다.");
            return java.util.Collections.emptyList();
        }

        final int batchSize = 12;
        log.info("[ AI 분석 ] >>> 배치 크기: {}, 총 배치 수: {}", batchSize, (outputs.size() + batchSize - 1) / batchSize);
        
        List<AutoAnalysisResult> results = new ArrayList<>(java.util.Collections.nCopies(outputs.size(), AutoAnalysisResult.empty()));

        for (int start = 0; start < outputs.size(); start += batchSize) {
            int end = Math.min(start + batchSize, outputs.size());
            log.info("[ AI 분석 배치 ] >>> 처리 중: {}/{} ({}~{})", 
                    (start / batchSize) + 1, (outputs.size() + batchSize - 1) / batchSize, start, end - 1);

            List<Callable<AutoAnalysisResult>> tasks = new ArrayList<>();
            for (int index = start; index < end; index++) {
                MonthOutput output = outputs.get(index);
                log.debug("[ AI 분석 작업 ] >>> 월: {}, DSL 길이: {}", output.ym(), output.dsl().length());
                
                tasks.add(() -> {
                    try {
                        AutoAnalysisResult result = autoAnalysisService.analyze(output, output.stats(), userDisplayName, friendDisplayName);
                        log.debug("[ AI 분석 완료 ] >>> 월: {}, 결과: {}", output.ym(), result != null ? "성공" : "실패");
                        return result != null ? result : AutoAnalysisResult.empty();
                    } catch (Exception ex) {
                        log.error("[ AI 분석 오류 ] >>> 월: {}, 오류: {}", output.ym(), ex.getMessage());
                        return AutoAnalysisResult.empty();
                    }
                });
            }

            try (ExecutorService executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory())) {
                log.info("[ 가상 스레드 ] >>> 배치 작업 시작 - 작업 수: {}", tasks.size());
                List<Future<AutoAnalysisResult>> futures = executor.invokeAll(tasks);
                
                for (int offset = 0; offset < futures.size(); offset++) {
                    int resultIndex = start + offset;
                    try {
                        AutoAnalysisResult result = futures.get(offset).get();
                        results.set(resultIndex, result != null ? result : AutoAnalysisResult.empty());
                        log.debug("[ 배치 결과 ] >>> 인덱스: {}, 결과: {}", resultIndex, result != null ? "성공" : "실패");
                    } catch (ExecutionException ex) {
                        log.error("[ 배치 실행 오류 ] >>> 인덱스: {}, 오류: {}", resultIndex, ex.getMessage());
                        results.set(resultIndex, AutoAnalysisResult.empty());
                    }
                }
                log.info("[ 가상 스레드 ] >>> 배치 작업 완료");
            } catch (InterruptedException ex) {
                log.error("[ 가상 스레드 ] >>> 인터럽트 발생: {}", ex.getMessage());
                Thread.currentThread().interrupt();
                break;
            }
        }

        // 결과 검증 및 정리
        int successCount = 0;
        for (int index = 0; index < results.size(); index++) {
            if (results.get(index) == null) {
                results.set(index, AutoAnalysisResult.empty());
            } else {
                successCount++;
            }
        }
        
        log.info("[ AI 분석 완료 ] >>> 총 월: {}, 성공: {}, 실패: {}", 
                results.size(), successCount, results.size() - successCount);
        
        return results;
    }

    @Transactional
    public void processWithBatchAutoAnalysis(Long personId, MultipartFile file, String userDisplayName) throws IOException {
        long perfStartNanos = System.nanoTime();
        try {
            log.info("[ 배치 AI 분석 시작 ] >>> personId: {}, 파일명: {}", personId, file != null ? file.getOriginalFilename() : "null");
            
            validateFile(file);

            Person person = personRepository.findById(personId)
                    .orElseThrow(() -> {
                        log.error("[ 배치 AI 분석 Person 조회 ] >>> 실패 - personId: {}", personId);
                        return new EntityNotFoundException("요청한 상대 정보를 찾을 수 없습니다.");
                    });

            log.info("[ 배치 AI 분석 ] >>> 상대방: {}", person.getName());

            String source = new String(file.getBytes(), StandardCharsets.UTF_8);
            log.info("[ 배치 AI 분석 ] >>> 파일 크기: {} bytes", source.length());
            
            List<MonthOutput> outputs = new ArrayList<>(onePassCore.run(source, userDisplayName, person.getName()));
            logCharCompression(source, outputs);
            log.info("[ 배치 AI 분석 ] >>> OnePassCore 결과: {} 개월", outputs.size());

            if (outputs.isEmpty()) {
                log.error("[ 배치 AI 분석 ] >>> 유효한 대화 메시지 없음");
                throw new IllegalArgumentException("유효한 대화 메시지를 찾을 수 없습니다. 대화 파일과 화자 이름을 확인해 주세요.");
            }

            outputs.sort(Comparator.comparing(MonthOutput::ym));
            log.info("[ 배치 AI 분석 ] >>> 처리할 월 범위: {} ~ {}", 
                    outputs.get(0).ym(), outputs.get(outputs.size() - 1).ym());
            
            YearMonth earliestMonth = outputs.get(0).ym();
            PersonValue previousValue = personValueRepository.findLatestBefore(personId, earliestMonth).orElse(null);
            log.info("[ 배치 AI 분석 ] >>> 이전 값 존재: {}", previousValue != null);

            List<AutoAnalysisResult> autoAnalyses = analyzeAutoAnalysisInBatches(outputs, userDisplayName, person.getName());
            log.info("[ 배치 AI 분석 ] >>> AI 분석 결과: {} 개", autoAnalyses.size());
            
            List<MonthlyComputationContext> contexts = monthlyContextBuilder.buildWithAutoAnalyses(
                    outputs,
                    autoAnalyses,
                    person,
                    previousValue
            );
            log.info("[ 배치 AI 분석 ] >>> 월별 컨텍스트 생성: {} 개", contexts.size());

            if (contexts.isEmpty()) {
                log.error("[ 배치 AI 분석 ] >>> 월별 컨텍스트 생성 실패");
                throw new IllegalArgumentException("AI 분석이 월별 결과를 생성하지 못했습니다. 입력 파일을 확인해 주세요.");
            }

            personUpdateCoordinator.persistAndUpdate(person, contexts);
            log.info("[ 배치 AI 분석 완료 ] >>> personId: {}, 처리한 월: {}", personId, contexts.size());

        } finally {
            long perfDurationMillis = (System.nanoTime() - perfStartNanos) / 1_000_000;
            log.info("[ 성능 측정 ] >>> 배치 AI 분석 소요시간: {} ms", perfDurationMillis);
        }
    }

    @Transactional
    public void update(Long personId, MultipartFile file, String userDisplayName) throws IOException {
        long perfStartNanos = System.nanoTime();
        try {
            log.info("[ 업데이트 시작 ] >>> personId: {}, 파일명: {}", personId, file != null ? file.getOriginalFilename() : "null");

            validateFile(file);

            Person person = personRepository.findById(personId)
                    .orElseThrow(() -> {
                        log.error("[ 업데이트 Person 조회 ] >>> 실패 - personId: {}", personId);
                        return new EntityNotFoundException("요청한 상대 정보를 찾을 수 없습니다.");
                    });

            String source = new String(file.getBytes(), StandardCharsets.UTF_8);
            List<MonthOutput> outputs = new ArrayList<>(onePassCore.run(source, userDisplayName, person.getName()));
            logCharCompression(source, outputs);

            if (outputs.isEmpty()) {
                throw new IllegalArgumentException("유효한 대화 메시지를 찾을 수 없습니다. 대화 파일과 화자 이름을 확인해 주세요.");
            }

            outputs.sort(Comparator.comparing(MonthOutput::ym));

            Optional<PersonValue> latestValueOpt = personValueRepository.findLatest(personId);
            if (latestValueOpt.isEmpty()) {
                log.info("[ 업데이트 ] >>> 기존 데이터가 없어 신규 파싱으로 처리합니다.");
                YearMonth earliestMonth = outputs.get(0).ym();
                PersonValue previousValue = personValueRepository.findLatestBefore(personId, earliestMonth).orElse(null);
                List<MonthlyComputationContext> contexts = monthlyContextBuilder.buildSequential(
                        outputs,
                        person,
                        previousValue,
                        DEFAULT_USER_DISPLAY_NAME,
                        person.getName()
                );
                personUpdateCoordinator.persistAndUpdate(person, contexts);
                log.info("[ 업데이트 완료 ] >>> personId: {}, 처리한 월: {}", personId, contexts.size());
                return;
            }

            PersonValue latestValue = latestValueOpt.get();
            YearMonth startMonth = monthlyContextBuilder.toYearMonth(latestValue);
            if (startMonth == null) {
                throw new IllegalStateException("기존 데이터의 연월 정보를 확인할 수 없습니다.");
            }

            List<MonthOutput> filteredOutputs = outputs.stream()
                    .filter(output -> !output.ym().isBefore(startMonth))
                    .collect(Collectors.toList());

            if (filteredOutputs.isEmpty()) {
                throw new IllegalArgumentException("업로드된 파일에 최신 월 이후의 대화가 포함되어 있지 않습니다.");
            }

            PersonValue referencePrevious = personValueRepository.findLatestBefore(personId, startMonth).orElse(null);

            personUpdateCoordinator.purgeFromMonth(person, startMonth);

            List<MonthlyComputationContext> contexts = monthlyContextBuilder.buildSequential(
                    filteredOutputs,
                    person,
                    referencePrevious,
                    userDisplayName,
                    person.getName()
            );

            personUpdateCoordinator.persistAndUpdate(person, contexts);

            log.info("[ 업데이트 완료 ] >>> personId: {}, 처리한 월: {}", personId, contexts.size());
        } finally {
            long perfDurationMillis = (System.nanoTime() - perfStartNanos) / 1_000_000;
            log.info("[ 성능 측정 ] >>> 업데이트 소요시간: {} ms", perfDurationMillis);
        }
    }
}
