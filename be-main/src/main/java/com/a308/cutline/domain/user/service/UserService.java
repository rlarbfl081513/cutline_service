package com.a308.cutline.domain.user.service;

import com.a308.cutline.common.entity.User;
import com.a308.cutline.domain.user.dto.UserCreateRequest;
import com.a308.cutline.domain.user.dto.UserResponse;
import com.a308.cutline.domain.user.dto.UserUpdateRequest;
import com.a308.cutline.domain.user.dao.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    
    private final UserRepository userRepository;
    
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }
        
        User user = new User(
            request.getEmail(),
            request.getName(),
            request.getBirth(),
            request.getGender()
        );
        
        User savedUser = userRepository.save(user);
        return UserResponse.from(savedUser);
    }
    
    public UserResponse findUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateUser(Long userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        if (request.getBirth() != null) {
            user.setBirth(request.getBirth());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse createMyProfile(Long userId, UserCreateRequest request) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 이메일은 카카오 동의로 이미 등록되어 있을 가능성이 높아 setter가 없다고 가정
        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getBirth() != null) {
            user.setBirth(request.getBirth());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }

        // JPA 변경감지로 저장됨
        return UserResponse.from(user);
    }
}