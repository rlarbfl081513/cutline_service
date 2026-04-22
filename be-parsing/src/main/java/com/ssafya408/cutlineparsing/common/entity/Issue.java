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
@Table(name = "issue")
public class Issue extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "issue_id")
    private Long id;

    @Column(name = "content", length = 100)
    private String content;

    @Column(name = "year")
    private Integer year;

    @Column(name = "month")
    private Integer month;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    public Issue(Person person, String content, Integer year, Integer month) {
        this.person = person;
        this.content = content;
        this.year = year;
        this.month = month;
    }

    public void update(String content, Integer year, Integer month) {
        this.content = content;
        this.year = year;
        this.month = month;
    }
}
