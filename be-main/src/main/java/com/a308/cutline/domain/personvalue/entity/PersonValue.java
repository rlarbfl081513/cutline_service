package com.a308.cutline.domain.personvalue.entity;

import com.a308.cutline.common.entity.BaseEntity;
import com.a308.cutline.common.entity.Person;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "person_value")
public class PersonValue extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "person_value_id")
    private Long id;
    
    @Column(name = "value")
    private Integer value;
    
    @Column(name = "year")
    private Integer year;
    
    @Column(name = "month")
    private Integer month;
    
    @Column(name = "feedback", length = 300)
    private String feedback;
    
    @Column(name = "change_rate", nullable = false)
    private Float changeRate;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "person_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "FK_person_value_person")
    )
    private Person person;
    
    public PersonValue(Integer value, Integer year, Integer month, String feedback, 
                      Float changeRate, Person person) {
        this.value = value;
        this.year = year;
        this.month = month;
        this.feedback = feedback;
        this.changeRate = changeRate;
        this.person = person;
    }
}
