package com.ssafya408.cutlineparsing.service;

import com.ssafya408.cutlineparsing.common.entity.Person;
import com.ssafya408.cutlineparsing.common.entity.PersonValue;
import com.ssafya408.cutlineparsing.dao.PersonRepository;
import com.ssafya408.cutlineparsing.dao.PersonValueRepository;
import com.ssafya408.cutlineparsing.service.dto.MonthlySummarySnapshot;
import com.ssafya408.cutlineparsing.service.dto.StrategyRecommendation;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PersonUpdateCoordinator {

    private final PersistenceCoordinator persistenceCoordinator;
    private final PersonValueRepository personValueRepository;
    private final RelationshipStrategyService relationshipStrategyService;
    private final PersonRepository personRepository;

    public void purgeFromMonth(Person person, YearMonth month) {
        Objects.requireNonNull(person, "person must not be null");
        if (month == null) {
            return;
        }

        log.info("[ 업데이트 정리 ] >>> personId: {}, 기준 월: {}", person.getId(), month);
        persistenceCoordinator.purgeFromMonth(person, month);
    }

    public void persistAndUpdate(Person person, List<MonthlyComputationContext> contexts) {
        Objects.requireNonNull(person, "person must not be null");
        if (contexts == null || contexts.isEmpty()) {
            throw new IllegalArgumentException("저장할 분석 결과가 존재하지 않습니다.");
        }

        log.info("[ 데이터 저장 ] >>> 시작 - 저장할 컨텍스트: {}개", contexts.size());
        List<MonthlySummarySnapshot> recentSummaries = persistenceCoordinator.persist(person, contexts);
        log.info("[ 데이터 저장 ] >>> 완료 - 생성된 스냅샷: {}개", recentSummaries != null ? recentSummaries.size() : 0);

        log.info("[ 저장 검증 ] >>> 시작");
        long personValueCount = personValueRepository.count();
        Optional<PersonValue> latestValueOpt = personValueRepository.findLatest(person.getId());
        latestValueOpt.ifPresent(value ->
                log.info("[ 저장 검증 ] >>> 최신 PersonValue - id: {}, personId: {}, year: {}, month: {}, value: {}, changeRate: {}",
                        value.getId(),
                        value.getPerson().getId(),
                        value.getYear(),
                        value.getMonth(),
                        value.getValue(),
                        value.getChangeRate())
        );
        int topicCount = person.getTopics() == null ? 0 : person.getTopics().size();
        int issueCount = person.getIssues() == null ? 0 : person.getIssues().size();

        log.info("[ 저장 통계 ] >>> 저장된 컨텍스트: {}개, 전체 PersonValue: {}개, 토픽: {}개, 이슈: {}개",
                contexts.size(), personValueCount, topicCount, issueCount);

        log.info("[ 전략 추천 ] >>> 시작 - personId: {}", person.getId());
        MonthlyComputationContext latestContext = contexts.get(contexts.size() - 1);
        StrategyRecommendation recommendation = relationshipStrategyService
                .recommend(person, recentSummaries, latestContext);
        log.info("[ 전략 추천 ] >>> 완료 - personId: {}", person.getId());

        log.info("[ Person 업데이트 ] >>> 시작 - personId: {}", person.getId());
        person.updateStrategies(
                recommendation.interestStrategy(),
                recommendation.uninterestStrategy(),
                recommendation.maintainStrategy()
        );

        personRepository.save(person);
        log.info("[ Person 업데이트 ] >>> 완료 - personId: {}", person.getId());
    }
}
