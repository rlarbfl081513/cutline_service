package com.a308.cutline.domain.chart.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "chat_auto_stats")
public class ChatAutoStats {

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

    @Column(name = "person_value_id", nullable = false)
    private Long personValueId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
