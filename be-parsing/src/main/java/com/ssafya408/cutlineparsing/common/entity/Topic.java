package com.ssafya408.cutlineparsing.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "topic")
public class Topic extends BaseEntity {

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    public Topic(Person person, Integer count, String topic, Integer year, Integer month) {
        this.person = person;
        this.count = count;
        this.topic = topic;
        this.year = year;
        this.month = month;
    }

    public void update(Integer count, String topic, Integer year, Integer month) {
        this.count = count;
        this.topic = topic;
        this.year = year;
        this.month = month;
    }
}
