package com.a308.cutline.domain.Offer.entity;

import com.a308.cutline.common.entity.BaseEntity;
import com.a308.cutline.common.entity.Category;
import com.a308.cutline.common.entity.Person;
import jakarta.persistence.*;
import lombok.*;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Builder
@Table(name = "offer")
public class Offer extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "offer_id")
    private Long id;

    @Column(name = "free_cash")
    private Integer freeCash;

    @Column(name = "price")
    private Integer price;

    @Column(name = "content", length = 225)
    private String content;

    // 사람_id 외래키로 받아오는 코드
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "person_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_offer_person")
    )
    private Person person;

    // 카테고리 정보
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_offer_category"))
    private Category category;

    // 선물 리스트에 대한 것
    @OneToMany(mappedBy = "offer", cascade = CascadeType.ALL,orphanRemoval = true)
    @Builder.Default
    private List<Gift> gifts = new ArrayList<>();

    // 편의 메서드 (양쪽 동식 세팅)
    public void addGift(Gift gift){
        if (this.gifts == null) {
            this.gifts = new ArrayList<>();
        }
        this.gifts.add(gift);
        gift.setOffer(this);
    }

    public Offer ( Integer freeCash,Integer price, String content, Person person, Category category){
        this.freeCash = freeCash;
        this.price = price;
        this.content = content;
        this.person = person;
        this.category = category;
        this.gifts = new ArrayList<>();
    }

}
