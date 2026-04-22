package com.a308.cutline.domain.person.dao;

import com.a308.cutline.common.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List; // Optional 대신 List를 사용합니다.

public interface TopicRepository extends JpaRepository<Topic, Long> {

    // 1. JPQL 문법 및 반환 타입 오류를 모두 수정한 메서드만 사용합니다.
    @Query(value = "SELECT t FROM Topic t " +
            "WHERE t.person.id = :personId AND t.year = :currentYear AND t.month = :currentMonth " +
            "ORDER BY t.count DESC LIMIT 3")
    List<Topic> findTop3TopicsByConditions( // 5개를 가져오므로 List<Topic>을 반환합니다.
            @Param("personId") Long personId,       // Person ID는 Long 타입
            @Param("currentYear") Integer currentYear,
            @Param("currentMonth") Integer currentMonth
    );


}