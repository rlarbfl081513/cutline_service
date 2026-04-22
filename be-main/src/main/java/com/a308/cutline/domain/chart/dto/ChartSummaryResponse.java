package com.a308.cutline.domain.chart.dto;

import com.a308.cutline.domain.personvalue.dto.PersonValueResponse;
import java.util.List;

public class ChartSummaryResponse {
    private ChatAutoStatsResponse latestAuto;       // 최신 1건
    private ChatManualStatsResponse latestManual;   // 최신 1건
    private List<IssueResponse> issues;             // 전체
    private List<PersonValueResponse> personValuesLast12; // 최신 12

    public ChartSummaryResponse() {}

    public ChartSummaryResponse(
            ChatAutoStatsResponse latestAuto,
            ChatManualStatsResponse latestManual,
            List<IssueResponse> issues,
            List<PersonValueResponse> personValuesLast12
    ) {
        this.latestAuto = latestAuto;
        this.latestManual = latestManual;
        this.issues = issues;
        this.personValuesLast12 = personValuesLast12;
    }

    public ChatAutoStatsResponse getLatestAuto() { return latestAuto; }
    public ChatManualStatsResponse getLatestManual() { return latestManual; }
    public List<IssueResponse> getIssues() { return issues; }
    public List<PersonValueResponse> getPersonValuesLast12() { return personValuesLast12; }

    public void setLatestAuto(ChatAutoStatsResponse latestAuto) { this.latestAuto = latestAuto; }
    public void setLatestManual(ChatManualStatsResponse latestManual) { this.latestManual = latestManual; }
    public void setIssues(List<IssueResponse> issues) { this.issues = issues; }
    public void setPersonValuesLast12(List<PersonValueResponse> personValuesLast12) { this.personValuesLast12 = personValuesLast12; }
}
