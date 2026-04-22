package com.ssafya408.cutlineparsing.service.dto;

public record IssuePayload(
        IssueCategory category,
        String summary
) {
    public static IssuePayload of(IssueCategory category, String summary) {
        return new IssuePayload(category, summary);
    }
}
