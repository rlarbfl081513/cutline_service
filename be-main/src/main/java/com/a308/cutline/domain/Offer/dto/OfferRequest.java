package com.a308.cutline.domain.Offer.dto;

import com.a308.cutline.common.entity.Title;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OfferRequest {

    private Integer freeCash;
    private Long personId;
    private Long categoryId;

}
