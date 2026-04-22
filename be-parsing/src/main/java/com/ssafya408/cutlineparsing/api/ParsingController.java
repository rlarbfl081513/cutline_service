package com.ssafya408.cutlineparsing.api;

import com.ssafya408.cutlineparsing.common.dto.ApiResponse;
import com.ssafya408.cutlineparsing.common.entity.User;
import com.ssafya408.cutlineparsing.common.security.AuthenticationUtils;
import com.ssafya408.cutlineparsing.dao.UserRepository;
import com.ssafya408.cutlineparsing.service.ParsingService;
import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * 카카오톡 파싱 API 컨트롤러
 * 
 * <p>카카오톡 대화 파일 업로드 및 파싱을 위한 REST API 엔드포인트를 제공합니다.
 * 모든 API는 JSON 형태의 표준화된 응답을 반환합니다.</p>
 * 
 * <h3>주요 엔드포인트:</h3>
 * <ul>
 *   <li><b>POST /{personId}:</b> 새로운 대화 파일 파싱 (AI 분석 포함)</li>
 *   <li><b>PUT /{personId}:</b> 기존 대화 업데이트 (AI 분석 포함)</li>
 * </ul>
 * 
 * <h3>인증:</h3>
 * <ul>
 *   <li>ACCESS_TOKEN 쿠키에 JWT 토큰 필요</li>
 *   <li>SecurityContext에서 사용자 정보 자동 추출</li>
 * </ul>
 * 
 * <h3>파일 지원:</h3>
 * <ul>
 *   <li>Android, iOS, PC 카카오톡 내보내기 파일 자동 감지</li>
 *   <li>UTF-8 인코딩 텍스트 파일</li>
 *   <li>최대 파일 크기: Spring Boot 기본값 적용</li>
 * </ul>
 * 
 * @author AI Assistant
 * @version 2.0 (MultiPlatform 파서 적용)
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class ParsingController {

    private final ParsingService parsingService;
    private final UserRepository userRepository;

    @PostMapping("/{personId}")
    public ResponseEntity<ApiResponse<Boolean>> parsingKakaoTalk(
            @PathVariable Long personId,
            @RequestParam("file") MultipartFile file) throws IOException {
        
        // SecurityContext에서 사용자 ID 추출
        Long userId = AuthenticationUtils.getCurrentUserId();
        
        // 사용자 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다: " + userId));
        String userDisplayName = user.getName();
        
        log.info("[ 파싱 시작 ] >>> 사용자: {} (ID: {}), 대상: {}, 파일: {} ({} bytes)", 
                userDisplayName, userId, personId, file.getOriginalFilename(), file.getSize());
        
        // 입력 파라미터 검증
        if (personId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "personId는 필수입니다");
        }
        
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효한 파일이 필요합니다");
        }

        try {
            parsingService.processWithBatchAutoAnalysis(personId, file, userDisplayName);
            log.info("[ 파싱 완료 ] >>> 사용자: {}, 대상: {}, 파일: {}", userDisplayName, personId, file.getOriginalFilename());
        } catch (IllegalArgumentException e) {
            log.error("[ 파싱 실패 ] >>> 잘못된 파라미터 - {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (EntityNotFoundException e) {
            log.error("[ 파싱 실패 ] >>> 엔티티 없음 - {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IOException e) {
            log.error("[ 파싱 실패 ] >>> IO 오류 - {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[ 파싱 실패 ] >>> 예상치 못한 오류 - {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다");
        }
        return ResponseEntity.ok(ApiResponse.success(Boolean.TRUE));
    }

    @PutMapping("/{personId}")
    public ResponseEntity<ApiResponse<Boolean>> updateKakaoTalk(
            @PathVariable Long personId,
            @RequestParam("file") MultipartFile file) throws IOException {
        
        // SecurityContext에서 사용자 ID 추출
        Long userId = AuthenticationUtils.getCurrentUserId();
        
        // 사용자 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다: " + userId));
        String userDisplayName = user.getName();
        
        log.info("[ 업데이트 시작 ] >>> 사용자: {} (ID: {}), 대상: {}, 파일: {} ({} bytes)", 
                userDisplayName, userId, personId, file.getOriginalFilename(), file.getSize());

        if (personId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "personId는 필수입니다");
        }

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효한 파일이 필요합니다");
        }

        try {
            parsingService.update(personId, file, userDisplayName);
            log.info("[ 업데이트 완료 ] >>> 사용자: {}, 대상: {}, 파일: {}", userDisplayName, personId, file.getOriginalFilename());
        } catch (IllegalArgumentException e) {
            log.error("[ 업데이트 실패 ] >>> 잘못된 파라미터 - {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (EntityNotFoundException e) {
            log.error("[ 업데이트 실패 ] >>> 엔티티 없음 - {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IOException e) {
            log.error("[ 업데이트 실패 ] >>> IO 오류 - {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[ 업데이트 실패 ] >>> 예상치 못한 오류 - {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다");
        }
        return ResponseEntity.ok(ApiResponse.success(Boolean.TRUE));
    }

}
