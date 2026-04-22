package com.ssafya408.cutlineparsing.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafya408.cutlineparsing.common.entity.Issue;
import com.ssafya408.cutlineparsing.common.entity.Person;
import com.ssafya408.cutlineparsing.common.entity.PersonValue;
import com.ssafya408.cutlineparsing.common.entity.Topic;
import com.ssafya408.cutlineparsing.dao.PersonValueRepository;
import com.ssafya408.cutlineparsing.service.dto.MonthlySummarySnapshot;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersistenceCoordinator {

    private final PersonValueRepository personValueRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public List<MonthlySummarySnapshot> persist(Person person, List<MonthlyComputationContext> contexts) {
        Objects.requireNonNull(person, "person must not be null");
        if (contexts == null || contexts.isEmpty()) {
            return List.of();
        }

        List<MonthlySummarySnapshot> summaries = new java.util.ArrayList<>();

        for (MonthlyComputationContext context : contexts) {
            PersonValue value = context.personValue();
            personValueRepository.save(value);
            person.addPersonValue(value);

            applyTopics(person, context.yearMonth(), context.autoAnalysis().topics());
            applyIssues(person, context.yearMonth(), context.autoAnalysis().issues());

            try {
                String topicsJson = objectMapper.writeValueAsString(context.autoAnalysis().topics());
                String issuesJson = objectMapper.writeValueAsString(context.autoAnalysis().issues());
                String autoStatsJson = objectMapper.writeValueAsString(context.autoAnalysis().autoStats());

                summaries.add(new MonthlySummarySnapshot(
                        context.yearMonth(),
                        context.autoAnalysis().feedback(),
                        topicsJson,
                        issuesJson,
                        autoStatsJson
                ));
            } catch (JsonProcessingException e) {
                log.warn("월별 요약 JSON 직렬화 실패: personId={}, ym={}", person.getId(), context.yearMonth());
            }
        }

        return summaries;
    }

    @Transactional
    public void purgeFromMonth(Person person, YearMonth month) {
        Objects.requireNonNull(person, "person must not be null");
        if (month == null) {
            return;
        }

        log.info("[ 데이터 정리 ] >>> personId: {}, 기준 월: {}", person.getId(), month);

        person.getTopics().removeIf(topic -> isSameOrAfter(topic.getYear(), topic.getMonth(), month));
        person.getIssues().removeIf(issue -> isSameOrAfter(issue.getYear(), issue.getMonth(), month));
        person.getPersonValues().removeIf(value -> isSameOrAfter(value.getYear(), value.getMonth(), month));

        personValueRepository.deleteFromMonth(person.getId(), month.getYear(), month.getMonthValue());
    }
    private void applyTopics(Person person, YearMonth ym, java.util.List<com.ssafya408.cutlineparsing.service.dto.TopicPayload> topics) {
        if (topics == null) {
            return;
        }

        person.getTopics().removeIf(topic -> sameMonth(topic.getYear(), topic.getMonth(), ym));

        for (var payload : topics) {
            if (payload == null) {
                continue;
            }
            String topicText = safeTruncate(payload.topic(), 40);
            int count = payload.count() == null ? 0 : Math.max(payload.count(), 0);
            Topic topic = new Topic(person, count, topicText, ym.getYear(), ym.getMonthValue());
            person.addTopic(topic);
        }
    }

    private void applyIssues(Person person, YearMonth ym, java.util.List<com.ssafya408.cutlineparsing.service.dto.IssuePayload> issues) {
        if (issues == null) {
            return;
        }

        person.getIssues().removeIf(issue -> sameMonth(issue.getYear(), issue.getMonth(), ym));

        for (var payload : issues) {
            if (payload == null) {
                continue;
            }
            String summary = payload.summary() == null ? "" : payload.summary();
            String prefix = payload.category() == null ? "" : payload.category().name() + ": ";
            String content = safeTruncate(prefix + summary, 100);
            Issue issue = new Issue(person, content, ym.getYear(), ym.getMonthValue());
            person.addIssue(issue);
        }
    }

    private boolean sameMonth(Integer year, Integer month, YearMonth ym) {
        if (year == null || month == null || ym == null) {
            return false;
        }
        return year.equals(ym.getYear()) && month.equals(ym.getMonthValue());
    }

    private boolean isSameOrAfter(Integer year, Integer month, YearMonth ym) {
        if (year == null || month == null || ym == null) {
            return false;
        }
        if (year > ym.getYear()) {
            return true;
        }
        if (year.equals(ym.getYear())) {
            return month >= ym.getMonthValue();
        }
        return false;
    }

    private String safeTruncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }
}
