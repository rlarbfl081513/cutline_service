// src/main/java/com/a308/cutline/domain/cashflow/dto/CashflowResponse.java
package com.a308.cutline.domain.cashflow.dto;

import com.a308.cutline.domain.cashflow.entity.Cashflow;
import com.a308.cutline.domain.cashflow.entity.Direction;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CashflowResponse {

    private Long id;
    private Long personId;
    private Long categoryId;

    private Integer price;
    private String item;
    private Direction direction;   // Enum 그대로 반환 (문자열 원하면 String로 바꿔도 OK)
    private Double inflationRate;
    private Integer changedPrice;

    private LocalDate date;        // 엔티티가 LocalDate 기준일 때
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CashflowResponse from(Cashflow c) {
        return new CashflowResponse(
                c.getId(),
                c.getPerson().getId(),
                c.getCategory() != null ? c.getCategory().getId() : null,
                c.getPrice(),
                c.getItem(),
                c.getDirection(),
                c.getInflationRate(),
                c.getChangedPrice(),
                c.getDate(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}


