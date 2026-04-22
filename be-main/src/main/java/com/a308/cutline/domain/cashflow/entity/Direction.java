package com.a308.cutline.domain.cashflow.entity;

public enum Direction {
    GIVE,
    TAKE;

    public int sing() { return this == GIVE ? -1 : 1; }
}
