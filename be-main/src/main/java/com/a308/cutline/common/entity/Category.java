package com.a308.cutline.common.entity;

import com.a308.cutline.domain.familiy_event.entity.FamilyEvent;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "category")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "title", length = 30)
    private Title title;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 3)
    private Type type;

    @OneToMany(mappedBy = "category")
    private List<FamilyEvent> familyEvents;


    public Category(Title title, Type type, List<FamilyEvent> familyEvents){
        this.title = title;
        this.type = type;
        this.familyEvents = familyEvents;
    }

}
