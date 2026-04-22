package com.a308.cutline.domain.Offer.dto;

import com.a308.cutline.domain.Offer.entity.Gift;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class GiftResponse {

    private Long id;
    private String title;
    private Integer price;
    private String link;
    private String image; // imageUrl -> image
    private String topic;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // GiftResponse.java (수정된 코드)

    public static GiftResponse from(Gift gift) {
        return new GiftResponse(
                gift.getId(),
                gift.getName(),     // getTitle() 대신 getName()으로 수정
                gift.getPrice(),
                gift.getLink(),
                gift.getImageUrl(), // getImage() 대신 getImageUrl()로 수정
                gift.getTopic(),
                gift.getCreatedAt(),
                gift.getUpdatedAt()
        );
    }
}