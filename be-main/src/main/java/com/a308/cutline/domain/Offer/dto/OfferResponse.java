package com.a308.cutline.domain.Offer.dto;

import com.a308.cutline.common.entity.Title;
import com.a308.cutline.common.entity.Type;
import com.a308.cutline.domain.Offer.entity.Offer;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class OfferResponse {
    
    private Long id;
    private Long personId;
    private Integer freeCash;
    private Integer price;
    private String content;
    private CategoryResponse category;
    private List<GiftResponse> gifts;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Getter
    @AllArgsConstructor
    public static class CategoryResponse {
        private Long id;
        private Title title;
        private Type type;
    }
    
    public static OfferResponse from(Offer offer) {
        CategoryResponse categoryResponse = null;
        if (offer.getCategory() != null) {
            categoryResponse = new CategoryResponse(
                offer.getCategory().getId(),
                offer.getCategory().getTitle(),
                offer.getCategory().getType()
            );
        }
        
        List<GiftResponse> giftResponses = offer.getGifts().stream()
            .map(GiftResponse::from)
            .collect(Collectors.toList());
        
        return new OfferResponse(
            offer.getId(),
            offer.getPerson().getId(),
            offer.getFreeCash(),
            offer.getPrice(),
            offer.getContent(),
            categoryResponse,
            giftResponses,
            offer.getCreatedAt(),
            offer.getUpdatedAt()
        );
    }
}