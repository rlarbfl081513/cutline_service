package com.a308.cutline.domain.user.dto;

import com.a308.cutline.common.entity.Gender;
import com.a308.cutline.common.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class UserResponse {
    
    private Long id;
    private String email;
    private String name;
    private LocalDate birth;
    private Gender gender;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getBirth(),
            user.getGender(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
}