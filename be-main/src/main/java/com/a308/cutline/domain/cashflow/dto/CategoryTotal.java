package com.a308.cutline.domain.cashflow.dto;

/**
 * 카테고리별 합계 DTO (이름 제거 버전)
 * - give: 지출 합계 (Direction.GIVE)
 * - take: 수입 합계 (Direction.TAKE)
 * - net : 순액 = take - give
 */
public class CategoryTotal {

    private Long categoryId;
    private long give;
    private long take;
    private long net;

    public CategoryTotal() {}

    public CategoryTotal(Long categoryId) {
        this.categoryId = categoryId;
    }

    public CategoryTotal(Long categoryId, long give, long take, long net) {
        this.categoryId = categoryId;
        this.give = give;
        this.take = take;
        this.net = net;
    }

    // getters
    public Long getCategoryId() { return categoryId; }
    public long getGive() { return give; }
    public long getTake() { return take; }
    public long getNet()  { return net;  }

    // setters
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public void setGive(long give) { this.give = give; }
    public void setTake(long take) { this.take = take; }
    public void setNet(long net)   { this.net = net; }

    @Override
    public String toString() {
        return "CategoryTotalDto{" +
                "categoryId=" + categoryId +
                ", give=" + give +
                ", take=" + take +
                ", net=" + net +
                '}';
    }
}
