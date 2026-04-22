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
@Table(name = "chat_manual_stats")
public class ChatManualStats {

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

    @Column(name = "person_value_id", nullable = false)
    private Long personValueId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
