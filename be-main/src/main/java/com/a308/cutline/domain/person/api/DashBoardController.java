package com.a308.cutline.domain.person.api;

import com.a308.cutline.common.dto.ApiResponse;
import com.a308.cutline.domain.person.dto.DashBoardResponse;
import com.a308.cutline.domain.person.service.DashBoardService;
import com.a308.cutline.util.AuthenticationUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "대시보드 관련 API")
public class DashBoardController {

    private final DashBoardService dashBoardService;

    @Operation(
        summary = "인물 대시보드 조회", 
        description = "특정 인물의 대시보드 정보를 조회합니다. (PersonValue 최신 12개월, 최신 FamilyEvent, 최신 Offer 포함)"
    )
    @GetMapping("/people/{personId}/value")
    public ResponseEntity<ApiResponse<DashBoardResponse>> getDashBoard(
            @Parameter(description = "인물 ID", required = true) @PathVariable Long personId) {
        try {
            Long userId = AuthenticationUtils.getCurrentUserId();
            DashBoardResponse response = dashBoardService.getDashBoard(userId, personId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
}
