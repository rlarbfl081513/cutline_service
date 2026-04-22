package com.a308.cutline.domain.personvalue.dao;

import com.a308.cutline.domain.personvalue.entity.PersonValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonValueRepository extends JpaRepository<PersonValue, Long> {
    
//    이거 맞나
    @Query(value = """
    SELECT person_id       AS personId,
           "value"         AS value,
           "year"          AS year,
           "month"         AS month,
           change_rate     AS changeRate
    FROM (
        SELECT pv.*,
               ROW_NUMBER() OVER (
                   PARTITION BY pv.person_id
                   ORDER BY pv."year" DESC, pv."month" DESC, pv.created_at DESC
               ) AS rn
        FROM person_value pv
        WHERE pv.person_id IN (:personIds)
    ) t
    WHERE t.rn = 1
    """, nativeQuery = true)
    List<LatestPersonValueProjection> findLatestByPersonIds(@Param("personIds") List<Long> personIds);
    
    @Query("SELECT pv FROM PersonValue pv WHERE pv.person.id = :personId ORDER BY pv.year DESC, pv.month DESC LIMIT 12")
    List<PersonValue> findLast12ByPersonId(@Param("personId") Long personId);
}
