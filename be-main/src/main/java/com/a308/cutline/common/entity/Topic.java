package com.a308.cutline.common.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "topic")
public class Topic extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "theme_id")
    private Long id;

    @Column(name = "count")
    private Integer count;

    @Column(name = "topic", length = 40)
    private String topic;

    @Column(name = "year")
    private Integer year;

    @Column(name = "month")
    private Integer month;

    /** FK: 사람 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    public Topic(Integer count, String topic, Integer year, Integer month, Person person){
        this.count = count;
        this.topic = topic;
        this.year = year;
        this.month = month;
        this.person = person;
    }
}
