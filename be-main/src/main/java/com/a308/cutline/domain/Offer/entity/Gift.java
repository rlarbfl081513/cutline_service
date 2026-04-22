package com.a308.cutline.domain.Offer.entity;

import com.a308.cutline.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "gift")
public class Gift extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gift_id")
    private Long id;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "price")
    private Integer price;

    @Column(name = "link")
    private String link;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "topic", length = 40)
    private String topic;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offer_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_gift_offer"))
    private Offer offer;

    // 이 생성자를 사용하도록 OfferService의 코드를 변경하는 것이 좋습니다.
    // 기존 코드의 혼란을 야기하는 생성자 대신, 아래와 같이 Builder 패턴을 사용하는 것을 권장합니다.
    // 하지만, 기존 생성자를 유지하려면 아래와 같이 수정하세요.
    public Gift(String name, Integer price, String imageUrl, String topic, Offer offer, String link){
        this.name = name;
        this.price = price;
        this.link = link;
        this.imageUrl = imageUrl;
        this.topic = topic;
        this.offer = offer;
    }

    // 아래의 비어있는 메서드는 제거해야 합니다.
    // public String getTitle() {}
    // public String getImage() {}
}