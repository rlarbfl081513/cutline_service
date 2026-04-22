package com.a308.cutline.domain.Offer.api;

import com.a308.cutline.common.dto.ApiResponse;
import com.a308.cutline.domain.Offer.dto.GiftResponse;
import com.a308.cutline.domain.Offer.dto.OfferRequest;
import com.a308.cutline.domain.Offer.dto.OfferResponse;
import com.a308.cutline.domain.Offer.entity.Gift;
import com.a308.cutline.domain.Offer.entity.Offer;
import com.a308.cutline.domain.Offer.service.OfferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/people/{personId}/offer")
@RequiredArgsConstructor
public class OfferController {

    private final OfferService offerService;

    // 유저가 여유금액과 이벤트 카테고리를 입력
    @PostMapping
    public ResponseEntity<ApiResponse<OfferResponse>> createOfferAndGiftList(@Valid @RequestBody OfferRequest request) {
        try {
            OfferResponse response = OfferResponse.from(offerService.createOfferAndGiftList(request));
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }


    @GetMapping
    public ResponseEntity<OfferResponse> getLatestOfferWithGifts(@PathVariable Long personId) {
        Offer latestOffer = offerService.getLatestOfferWithGiftsByPersonId(personId);
        return ResponseEntity.ok(OfferResponse.from(latestOffer));
    }
}