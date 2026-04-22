package com.a308.cutline.domain.user.dto;

import com.a308.cutline.common.entity.Gender;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class UserUpdateRequest {
    private LocalDate birth;
    private Gender gender;
}


