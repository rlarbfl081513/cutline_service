package com.a308.cutline.domain.personvalue.dao;

public interface LatestPersonValueProjection {
    Long getPersonId();
    Integer getValue();
    Integer getYear();
    Integer getMonth();
    Float getChangeRate();
}
