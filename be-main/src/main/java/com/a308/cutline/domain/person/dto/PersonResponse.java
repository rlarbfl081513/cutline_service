package com.a308.cutline.domain.person.dto;

import com.a308.cutline.common.entity.Gender;
import com.a308.cutline.common.entity.Relation;
import com.a308.cutline.common.entity.Person;
import com.a308.cutline.common.entity.PersonStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PersonResponse {

    // DB: person_id, user_id ...
    private Long id;
    private Long userId;

    private String name;
    private LocalDate birth;
    private Gender gender;
    private PersonStatus status;

    // 새로 추가된 3개 전략 필드
    private String interestStrategy;
    private String uninterestStrategy;
    private String maintainStrategy;

    private Relation relation;
    private Integer duration;     // not null in DB지만 Integer로 둬도 무방
    private Integer totalGive;
    private Integer totalTake;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;  // nullable

    private Integer age;

    public static PersonResponse from(Person person) {
        return new PersonResponse(
                person.getId(),
                person.getUser() == null ? null : person.getUser().getId(),
                person.getName(),
                person.getBirth(),
                person.getGender(),
                person.getStatus(),
                person.getInterestStrategy(),
                person.getUninterestStrategy(),
                person.getMaintainStrategy(),
                person.getRelation(),
                person.getDuration(),
                person.getTotalGive(),
                person.getTotalTake(),
                person.getCreatedAt(),
                person.getUpdatedAt(),
                person.getDeletedAt(),
                person.getAge()
        );
    }
}
