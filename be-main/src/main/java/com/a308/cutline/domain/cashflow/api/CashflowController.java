// src/main/java/com/a308/cutline/domain/cashflow/api/CashflowController.java
package com.a308.cutline.domain.cashflow.api;

import com.a308.cutline.common.dto.ApiResponse;
import com.a308.cutline.domain.cashflow.dto.CashflowCreateRequest;
import com.a308.cutline.domain.cashflow.dto.CashflowFullResponse;
import com.a308.cutline.domain.cashflow.dto.CashflowResponse;
import com.a308.cutline.domain.cashflow.service.CashflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/people")
public class CashflowController {

    private final CashflowService cashflowService;

    /** 생성 */
    @PostMapping("/{personId}/cashflows")
    public ResponseEntity<ApiResponse<CashflowResponse>> create(
            @PathVariable Long personId,
            @RequestBody CashflowCreateRequest request
    ) {
        try {
            // (권한 체크는 Service에서 personId 기반으로 처리 가정)
            CashflowResponse res = cashflowService.create(personId, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(res));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /** 조회: 인물별 **/
    @GetMapping("/{personId}/cashflows/list")
    public ResponseEntity<ApiResponse<List<CashflowResponse>>> list(
            @PathVariable Long personId
    ) {
        try {
            List<CashflowResponse> res = cashflowService.listAll(personId);
            return ResponseEntity.ok(ApiResponse.success(res)); // ✅ 제네릭 일치
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{personId}/cashflows")
    public ResponseEntity<ApiResponse<CashflowFullResponse>> summary(
            @PathVariable Long personId
    ) {
        try {
            CashflowFullResponse res = cashflowService.getSummary(personId);
            return ResponseEntity.ok(ApiResponse.success(res));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }


    /** 소프트 삭제: deleted_at=now() */
    @PatchMapping("/{personId}/cashflows/{cashflowId}/delete")
    public ResponseEntity<ApiResponse<Void>> softDelete(
            @PathVariable Long personId,
            @PathVariable Long cashflowId
    ) {
        try {
            cashflowService.softDelete(cashflowId);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{personId}/cashflows/latest")
    public ResponseEntity<?> latestOne(
            @PathVariable Long personId,
            @RequestParam Long categoryId) {
        return cashflowService.getLatestChangedPrice(personId, categoryId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
