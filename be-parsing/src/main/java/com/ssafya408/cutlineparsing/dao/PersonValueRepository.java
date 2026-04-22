package com.ssafya408.cutlineparsing.dao;

import com.ssafya408.cutlineparsing.common.entity.PersonValue;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonValueRepository extends JpaRepository<PersonValue, Long> {

    @Query("select pv from PersonValue pv where pv.person.id = :personId " +
            "and (pv.year < :year or (pv.year = :year and pv.month < :month)) " +
            "order by pv.year desc, pv.month desc")
    List<PersonValue> findLatestBefore(Long personId, Integer year, Integer month, Pageable pageable);

    Optional<PersonValue> findTopByOrderByIdDesc();

    @Query("select pv from PersonValue pv where pv.person.id = :personId order by pv.year desc, pv.month desc")
    List<PersonValue> findLatestInternal(Long personId, Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("delete from PersonValue pv where pv.person.id = :personId and " +
            "(pv.year > :year or (pv.year = :year and pv.month >= :month))")
    void deleteFromMonth(Long personId, Integer year, Integer month);

    default Optional<PersonValue> findLatestBefore(Long personId, YearMonth yearMonth) {
        List<PersonValue> result = findLatestBefore(personId, yearMonth.getYear(), yearMonth.getMonthValue(), PageRequest.of(0, 1));
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    default Optional<PersonValue> findLatest(Long personId) {
        List<PersonValue> result = findLatestInternal(personId, PageRequest.of(0, 1));
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }
}
