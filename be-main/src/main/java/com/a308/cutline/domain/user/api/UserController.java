package com.a308.cutline.domain.user.api;

import com.a308.cutline.common.dto.ApiResponse;
import com.a308.cutline.domain.user.dto.UserCreateRequest;
import com.a308.cutline.domain.user.dto.UserResponse;
import com.a308.cutline.domain.user.dto.UserUpdateRequest;
import com.a308.cutline.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;


import java.security.Principal;

@RestController
@RequestMapping("")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(Principal principal) {
        try {
            Long userId = Long.valueOf(principal.getName());
            UserResponse response = userService.findUser(userId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> createMe(
            Principal principal,
            @Valid @RequestBody UserCreateRequest request
    ) {
        try {
            Long userId = Long.valueOf(principal.getName()); // 토큰의 subject를 userId로 사용 중
            UserResponse response = userService.createMyProfile(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

}