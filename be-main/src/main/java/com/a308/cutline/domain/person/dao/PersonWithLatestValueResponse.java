package com.a308.cutline.domain.person.dao;

import com.a308.cutline.domain.person.dto.PersonResponse;

// 최신 person_value만 덧붙인 응답 (이 API 전용)
public record PersonWithLatestValueResponse(
        PersonResponse person,   // 기존 DTO 그대로
        Integer latestValue,     // nullable
        Integer latestYear,      // nullable
        Integer latestMonth,      // nullable
        Float latestChangeRate
) {}

