package com.a308.cutline.domain.chart.service;

import com.a308.cutline.domain.chart.dao.ChatAutoStatsRepository;
import com.a308.cutline.domain.chart.dao.ChatManualStatsRepository;
import com.a308.cutline.domain.chart.dao.IssueRepository;
import com.a308.cutline.domain.chart.dto.*;
import com.a308.cutline.domain.personvalue.dao.PersonValueRepository;
import com.a308.cutline.domain.personvalue.dto.PersonValueResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChartService {

    private final ChatAutoStatsRepository chatAutoStatsRepository;
    private final ChatManualStatsRepository chatManualStatsRepository;
    private final IssueRepository issueRepository;
    private final PersonValueRepository personValueRepository;

    @Transactional(readOnly = true)
    public ChartSummaryResponse getChart(Long personId) {

        // 1) 최신 ChatAutoStats 1건
        ChatAutoStatsResponse latestAuto = chatAutoStatsRepository.findLatestByPersonId(personId)
                .map(ChatAutoStatsResponse::from)
                .orElse(null);

        // 2) 최신 ChatManualStats 1건
        ChatManualStatsResponse latestManual = chatManualStatsRepository.findLatestByPersonId(personId)
                .map(ChatManualStatsResponse::from)
                .orElse(null);

        // 3) Issue 전체 (최신순)
        var issueResponses = issueRepository.findAllByPersonIdOrderByCreatedAtDesc(personId)
                .stream()
                .map(IssueResponse::from)
                .toList();

        // 4) PersonValue 최신 12개
        List<PersonValueResponse> personValueResponses = personValueRepository.findLast12ByPersonId(personId)
                .stream()
                .map(PersonValueResponse::from)
                .toList();

        return new ChartSummaryResponse(
                latestAuto,
                latestManual,
                issueResponses,
                personValueResponses
        );
    }
}
