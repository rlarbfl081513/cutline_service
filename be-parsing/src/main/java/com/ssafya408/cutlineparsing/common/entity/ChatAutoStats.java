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
@Table(name = "chat_auto_stats")
public class ChatAutoStats extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_auto_stats_id")
    private Long id;

    @Column(name = "start_chat")
    private Integer startChat;

    @Column(name = "question")
    private Integer question;

    @Column(name = "private_story")
    private Integer privateStory;

    @Column(name = "positive_reaction")
    private Integer positiveReaction;

    @Column(name = "get_help")
    private Integer getHelp;

    @Column(name = "meeting_success")
    private Integer meetingSuccess;

    @Column(name = "no_response")
    private Integer noResponse;

    @Column(name = "give_help")
    private Integer giveHelp;

    @Column(name = "attack")
    private Integer attack;

    @Column(name = "meeting_rejection")
    private Integer meetingRejection;

    @Transient
    private Integer scoreStartChat;

    @Transient
    private Integer scoreQuestion;

    @Transient
    private Integer scorePrivateStory;

    @Transient
    private Integer scorePositiveReaction;

    @Transient
    private Integer scoreMeetingSuccess;

    @Transient
    private Integer scoreGiveHelp;

    @Transient
    private Integer scoreNoResponse;

    @Transient
    private Integer scoreMeetingRejection;

    @Transient
    private Integer scoreAttack;

    @Transient
    private Integer scoreGetHelp;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_value_id", nullable = false)
    private PersonValue personValue;

    public ChatAutoStats(Integer startChat, Integer question, Integer privateStory, Integer positiveReaction,
                         Integer getHelp, Integer meetingSuccess, Integer noResponse, Integer giveHelp,
                         Integer attack, Integer meetingRejection,
                         Integer scoreStartChat, Integer scoreQuestion, Integer scorePrivateStory,
                         Integer scorePositiveReaction, Integer scoreMeetingSuccess, Integer scoreGiveHelp,
                         Integer scoreNoResponse, Integer scoreMeetingRejection, Integer scoreAttack, Integer scoreGetHelp) {
        this.startChat = startChat;
        this.question = question;
        this.privateStory = privateStory;
        this.positiveReaction = positiveReaction;
        this.getHelp = getHelp;
        this.meetingSuccess = meetingSuccess;
        this.noResponse = noResponse;
        this.giveHelp = giveHelp;
        this.attack = attack;
        this.meetingRejection = meetingRejection;
        this.scoreStartChat = scoreStartChat;
        this.scoreQuestion = scoreQuestion;
        this.scorePrivateStory = scorePrivateStory;
        this.scorePositiveReaction = scorePositiveReaction;
        this.scoreMeetingSuccess = scoreMeetingSuccess;
        this.scoreGiveHelp = scoreGiveHelp;
        this.scoreNoResponse = scoreNoResponse;
        this.scoreMeetingRejection = scoreMeetingRejection;
        this.scoreAttack = scoreAttack;
        this.scoreGetHelp = scoreGetHelp;
    }

    void bindTo(PersonValue personValue) {
        this.personValue = personValue;
    }

    public void update(Integer startChat, Integer question, Integer privateStory, Integer positiveReaction,
                       Integer getHelp, Integer meetingSuccess, Integer noResponse, Integer giveHelp,
                       Integer attack, Integer meetingRejection,
                       Integer scoreStartChat, Integer scoreQuestion, Integer scorePrivateStory,
                       Integer scorePositiveReaction, Integer scoreMeetingSuccess, Integer scoreGiveHelp,
                       Integer scoreNoResponse, Integer scoreMeetingRejection, Integer scoreAttack, Integer scoreGetHelp) {
        this.startChat = startChat;
        this.question = question;
        this.privateStory = privateStory;
        this.positiveReaction = positiveReaction;
        this.getHelp = getHelp;
        this.meetingSuccess = meetingSuccess;
        this.noResponse = noResponse;
        this.giveHelp = giveHelp;
        this.attack = attack;
        this.meetingRejection = meetingRejection;
        this.scoreStartChat = scoreStartChat;
        this.scoreQuestion = scoreQuestion;
        this.scorePrivateStory = scorePrivateStory;
        this.scorePositiveReaction = scorePositiveReaction;
        this.scoreMeetingSuccess = scoreMeetingSuccess;
        this.scoreGiveHelp = scoreGiveHelp;
        this.scoreNoResponse = scoreNoResponse;
        this.scoreMeetingRejection = scoreMeetingRejection;
        this.scoreAttack = scoreAttack;
        this.scoreGetHelp = scoreGetHelp;
    }
}
