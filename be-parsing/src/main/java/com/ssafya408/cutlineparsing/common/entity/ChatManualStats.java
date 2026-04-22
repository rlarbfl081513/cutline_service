package com.ssafya408.cutlineparsing.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "chat_manual_stats")
public class ChatManualStats extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_manual_stats_id")
    private Long id;

    @Column(name = "month_volume")
    private Integer monthVolume;

    @Column(name = "month_count")
    private Integer monthCount;

    @Column(name = "response_average")
    private Integer responseAverage;

    @Column(name = "chat_days")
    private Integer chatDays;

    @Column(name = "silent_days")
    private Integer silentDays;

    @Transient
    private Integer bonusChars;

    @Transient
    private Integer bonusMessages;

    @Transient
    private Integer bonusChatDays;

    @Transient
    private Integer penaltyResponse;

    @Transient
    private Integer penaltySilent;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_value_id", nullable = false)
    private PersonValue personValue;

    public ChatManualStats(Integer monthVolume, Integer monthCount, Integer responseAverage,
                          Integer chatDays, Integer silentDays,
                          Integer bonusChars, Integer bonusMessages, Integer bonusChatDays,
                          Integer penaltyResponse, Integer penaltySilent) {
        this.monthVolume = monthVolume;
        this.monthCount = monthCount;
        this.responseAverage = responseAverage;
        this.chatDays = chatDays;
        this.silentDays = silentDays;
        this.bonusChars = bonusChars;
        this.bonusMessages = bonusMessages;
        this.bonusChatDays = bonusChatDays;
        this.penaltyResponse = penaltyResponse;
        this.penaltySilent = penaltySilent;
    }

    void bindTo(PersonValue personValue) {
        this.personValue = personValue;
    }

    public void update(Integer monthVolume, Integer monthCount, Integer responseAverage,
                       Integer chatDays, Integer silentDays,
                       Integer bonusChars, Integer bonusMessages, Integer bonusChatDays,
                       Integer penaltyResponse, Integer penaltySilent) {
        this.monthVolume = monthVolume;
        this.monthCount = monthCount;
        this.responseAverage = responseAverage;
        this.chatDays = chatDays;
        this.silentDays = silentDays;
        this.bonusChars = bonusChars;
        this.bonusMessages = bonusMessages;
        this.bonusChatDays = bonusChatDays;
        this.penaltyResponse = penaltyResponse;
        this.penaltySilent = penaltySilent;
    }
}
