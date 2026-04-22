package com.a308.cutline.domain.chart.api;

import com.a308.cutline.domain.chart.dto.ChartSummaryResponse;
import com.a308.cutline.domain.chart.service.ChartService;
import com.a308.cutline.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/persons/{personId}/chart")
public class ChartController {

    private final ChartService chartService;

    @GetMapping
    public ResponseEntity<ApiResponse<ChartSummaryResponse>> getChart(
            @PathVariable Long personId
    ) {
        ChartSummaryResponse res = chartService.getChart(personId);
        return ResponseEntity.ok(ApiResponse.success(res));
    }
}
