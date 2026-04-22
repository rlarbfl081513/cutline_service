package com.ssafya408.cutlineparsing.api;

import com.ssafya408.cutlineparsing.common.dto.ApiResponse;
import com.ssafya408.cutlineparsing.dto.TempDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/temp")
public class TempController {
    
    /**
     * 단일 TempDto 객체를 반환하는 예시
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getTempById(@PathVariable Long id) {
        try {
            // 실제로는 Service나 Repository에서 데이터를 가져올 것이지만, 
            // 예시를 위해 임시 데이터를 생성
            TempDto tempDto = TempDto.of(id, "Sample Item " + id, "This is a sample description", "SAMPLE");
            
            return ResponseEntity.ok(ApiResponse.success(tempDto));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.errorWithMessage("ID " + id + "에 해당하는 데이터를 찾을 수 없습니다."));
        }
    }
    
    /**
     * TempDto 리스트를 반환하는 예시
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TempDto>>> getAllTemp() {
        List<TempDto> tempList = Arrays.asList(
                TempDto.of(1L, "첫 번째 아이템", "첫 번째 설명", "CATEGORY_A"),
                TempDto.of(2L, "두 번째 아이템", "두 번째 설명", "CATEGORY_B"),
                TempDto.of(3L, "세 번째 아이템", "세 번째 설명", "CATEGORY_A")
        );
        
        return ResponseEntity.ok(ApiResponse.success(tempList));
    }
    
    /**
     * 새로운 TempDto를 생성하는 예시
     */
    @PostMapping
    public ResponseEntity<ApiResponse<?>> createTemp(@RequestBody TempDto tempDto) {
        try {
            // 유효성 검사
            if (tempDto.getName() == null || tempDto.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.errorWithMessage("이름은 필수 입력 항목입니다."));
            }
            
            // 임시로 ID를 설정 (실제로는 DB에서 자동 생성)
            tempDto.setId(System.currentTimeMillis());
            
            return ResponseEntity.ok(ApiResponse.success(tempDto));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.errorWithMessage("데이터 생성 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 기존 TempDto를 수정하는 예시
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> updateTemp(@PathVariable Long id, @RequestBody TempDto tempDto) {
        try {
            // ID 존재 여부 확인 (실제로는 DB에서 확인)
            if (id == null || id <= 0) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.errorWithMessage("유효하지 않은 ID입니다."));
            }
            
            // 업데이트된 데이터 설정
            tempDto.setId(id);
            
            return ResponseEntity.ok(ApiResponse.success(tempDto));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.errorWithMessage("데이터 수정 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * TempDto 삭제하는 예시
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteTemp(@PathVariable Long id) {
        try {
            // ID 존재 여부 확인 (실제로는 DB에서 확인)
            if (id == null || id <= 0) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.errorWithMessage("유효하지 않은 ID입니다."));
            }
            
            return ResponseEntity.ok(ApiResponse.success("ID " + id + "의 데이터가 성공적으로 삭제되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.errorWithMessage("데이터 삭제 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 카테고리별 조회 예시
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<List<TempDto>>> getTempByCategory(@PathVariable String category) {
        List<TempDto> filteredList = Arrays.asList(
                TempDto.of(1L, "카테고리 " + category + " 아이템 1", "설명 1", category),
                TempDto.of(2L, "카테고리 " + category + " 아이템 2", "설명 2", category)
        );
        
        return ResponseEntity.ok(ApiResponse.success(filteredList));
    }
    
    /**
     * 에러 케이스를 보여주는 예시
     */
    @GetMapping("/error")
    public ResponseEntity<ApiResponse<Object>> triggerError() {
        return ResponseEntity.internalServerError()
                .body(ApiResponse.error("의도적으로 발생시킨 에러입니다."));
    }
}
