// src/main/java/com/a308/cutline/domain/cashflow/dao/CashflowRepository.java
package com.a308.cutline.domain.cashflow.dao;

import com.a308.cutline.domain.cashflow.entity.Cashflow;
import com.a308.cutline.domain.cashflow.entity.Direction;
import com.a308.cutline.domain.cashflow.dao.projection.CategoryDirectionSum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CashflowRepository extends JpaRepository<Cashflow, Long> {

    // 전체 히스토리(미삭제만), date DESC, id DESC
    List<Cashflow> findAllByPerson_IdAndDeletedAtIsNullOrderByDateDescIdDesc(Long personId);

    @Query("""
        select
            c.category.id as categoryId,
            c.direction   as direction,
            sum(coalesce(c.changedPrice, c.price, 0)) as amount
        from Cashflow c
        where c.person.id = :personId
          and c.deletedAt is null
        group by c.category.id, c.direction
    """)
    List<CategoryDirectionSum> sumByCategoryAndDirection(@Param("personId") Long personId);

    Optional<Cashflow>
    findTopByPerson_IdAndCategory_IdAndDirectionAndChangedPriceIsNotNullAndDeletedAtIsNullOrderByDateDescIdDesc(
            Long personId, Long categoryId, Direction direction
    );


}
