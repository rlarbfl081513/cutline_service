package com.a308.cutline.domain.Offer.dao;

import aj.org.objectweb.asm.commons.Remapper;
import com.a308.cutline.domain.Offer.entity.Gift;
import com.a308.cutline.domain.Offer.entity.Offer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long> {

    // @EntityGraph는 제거하는 것이 좋습니다. JOIN FETCH로 충분합니다.
    @EntityGraph(attributePaths = "gifts")

    Optional<Offer> findFirstByPersonIdOrderByCreatedAtDesc(@Param("personId") Long personId);


}