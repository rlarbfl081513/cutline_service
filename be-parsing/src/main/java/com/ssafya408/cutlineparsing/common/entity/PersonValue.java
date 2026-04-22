package com.ssafya408.cutlineparsing.common.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
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
    private Double changeRate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @OneToOne(mappedBy = "personValue", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private ChatManualStats manualStats;

    @OneToOne(mappedBy = "personValue", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private ChatAutoStats autoStats;

    public PersonValue(Person person, Integer value, Integer year, Integer month, String feedback, Double changeRate) {
        this.person = person;
        this.value = value;
        this.year = year;
        this.month = month;
        this.feedback = feedback;
        this.changeRate = changeRate;
    }

    public void update(Integer value, Integer year, Integer month, String feedback, Double changeRate) {
        this.value = value;
        this.year = year;
        this.month = month;
        this.feedback = feedback;
        this.changeRate = changeRate;
    }

    public void setManualStats(ChatManualStats manualStats) {
        this.manualStats = manualStats;
        if (manualStats != null) {
            manualStats.bindTo(this);
        }
    }

    public void setAutoStats(ChatAutoStats autoStats) {
        this.autoStats = autoStats;
        if (autoStats != null) {
            autoStats.bindTo(this);
        }
    }
}
