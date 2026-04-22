package com.a308.cutline.domain.Offer.dao;

import com.a308.cutline.domain.Offer.entity.Gift;
import com.a308.cutline.domain.Offer.entity.Offer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GiftRepository extends JpaRepository<Gift, Long> {

    @Query(value = "SELECT g FROM Gift g " +
            "JOIN g.offer o " +
            "WHERE o.person.id = :personId " +
            "ORDER BY o.createdAt DESC " +
            "LIMIT 15", nativeQuery = false)
    List<Gift> findTop15ByOfferPersonIdOrderByOfferCreatedAtDesc(@Param("personId") Long personId);
}