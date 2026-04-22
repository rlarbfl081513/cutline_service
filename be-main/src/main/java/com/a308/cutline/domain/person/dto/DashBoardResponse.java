package com.a308.cutline.domain.person.dto;

import com.a308.cutline.domain.personvalue.dto.PersonValueResponse;
import com.a308.cutline.domain.familiy_event.dto.FamilyEventResponse;
import com.a308.cutline.domain.Offer.dto.OfferResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class DashBoardResponse {

    private PersonResponse person;                           // Person 정보
    private List<PersonValueResponse> personValuesLast12;   // 최신 포함 최대 12개
    private FamilyEventResponse latestFamilyEvent;          // 가장 최신 1건 (nullable 가능)
    private OfferResponse latestOffer;                      // 가장 최신 1건 (nullable 가능)
}
