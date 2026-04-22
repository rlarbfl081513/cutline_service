package com.a308.cutline.domain.familiy_event.entity;

import com.a308.cutline.common.entity.BaseEntity;
import com.a308.cutline.common.entity.Category;
import com.a308.cutline.common.entity.Person;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "family_event")
public class FamilyEvent extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "family_event_id")
    private Long id;

    @Column(name = "cost")
    private Integer cost;

    @Column(name = "attendance")
    private Boolean attendance;

    @Column(name = "price")
    private Integer price;

    @Column(name = "content", length = 225)
    private String content;


    // 사람_id 외래키로 받아오는 코드
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "person_id",
            nullable = false
    )
    private Person person;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    public FamilyEvent(Integer cost, Boolean attendance, Integer price, String content, Person person, Category category){
        this.cost = cost;
        this.attendance = attendance;
        this.price = price;
        this.content = content;
        this.person = person;
        this.category = category;
    }

    public FamilyEvent(Integer cost, Person person, Category category) {
        super();
    }
}


