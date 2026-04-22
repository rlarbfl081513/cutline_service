package com.ssafya408.cutlineparsing.service.dto;

import java.util.List;

public record AutoAnalysisResult(
        AutoStatsPayload autoStats,
        List<TopicPayload> topics,
        List<IssuePayload> issues,
        String feedback
) {
    public static AutoAnalysisResult empty() {
        return new AutoAnalysisResult(
                AutoStatsPayload.empty(),
                List.of(),
                List.of(),
                ""
        );
    }
}
