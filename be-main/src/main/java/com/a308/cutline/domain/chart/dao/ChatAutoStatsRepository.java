package com.a308.cutline.domain.chart.dao;

import com.a308.cutline.domain.chart.entity.ChatAutoStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChatAutoStatsRepository extends JpaRepository<ChatAutoStats, Long> {

    @Query(value = """
        SELECT s.*
        FROM chat_auto_stats s
        JOIN person_value pv ON pv.person_value_id = s.person_value_id
        WHERE pv.person_id = :personId
        ORDER BY s.created_at DESC
        LIMIT 1
    """, nativeQuery = true)
    Optional<ChatAutoStats> findLatestByPersonId(@Param("personId") Long personId);
}
