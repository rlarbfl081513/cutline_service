// src/main/java/com/a308/cutline/domain/cashflow/dto/CashflowCreateRequest.java
package com.a308.cutline.domain.cashflow.dto;

import com.a308.cutline.domain.cashflow.entity.Direction;

import java.time.LocalDate;

public record CashflowCreateRequest(
        Long categoryId,
        Integer price,
        String item,
        Direction direction,
        LocalDate date     // yyyy-MM-dd
) {}
