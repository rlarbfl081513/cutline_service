package com.a308.cutline.domain.familiy_event.dao;

import com.a308.cutline.domain.familiy_event.entity.FamilyEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FamilyEventRepository extends JpaRepository<FamilyEvent,Long> {
    
    @Query("SELECT fe FROM FamilyEvent fe WHERE fe.person.id = :personId ORDER BY fe.createdAt DESC LIMIT 1")
    Optional<FamilyEvent> findLatestByPersonId(@Param("personId") Long personId);

    List<FamilyEvent> findByPersonId(Long personId);
}
