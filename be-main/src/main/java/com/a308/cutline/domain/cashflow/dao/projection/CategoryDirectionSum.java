package com.a308.cutline.domain.cashflow.dao.projection;

import com.a308.cutline.domain.cashflow.entity.Direction;

public interface CategoryDirectionSum {
    Long getCategoryId();
    Direction getDirection();
    Long getAmount();
}
