package com.a308.cutline.domain.person.dto;

import com.a308.cutline.common.entity.Gender;
import com.a308.cutline.common.entity.Relation;
import com.a308.cutline.common.entity.PersonStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class PersonUpdateRequest {
    
    private String name;
    private Integer age;
    private LocalDate birth;
    private Gender gender;
    private PersonStatus status;
    private Relation relation;
    private Integer duration;

}