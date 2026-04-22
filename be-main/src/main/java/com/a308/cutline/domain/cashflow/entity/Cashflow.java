package com.a308.cutline.domain.cashflow.entity;

import com.a308.cutline.common.entity.BaseEntity;
import com.a308.cutline.common.entity.Category;
import com.a308.cutline.common.entity.Person;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "cash_flow")
public class Cashflow extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cash_flow_id")
    private Long id;

    /** FK: 사람 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    /** FK: 카테고리 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /** 당시 결제 금액(원) */
    @Column(name = "price")
    private Integer price;

    /** 품목/메모 */
    @Column(name = "item", length = 100)
    private String item;

    /** Give=내가 지출 / Take=내가 수령 (enum 별도 파일) */
    @Enumerated(EnumType.STRING)
    @Column(name = "direction", length = 8)
    private Direction direction;

    /** 물가상승률 반영 현재가치(원) */
    @Column(name = "changed_price")
    private Integer changedPrice;

    /** 연 기준 물가상승률(예: 0.0325) */
    @Column(name = "inflation_rate", nullable = true)
    private Double inflationRate;

    /** 거래 날짜(프론트 일 단위 입력). DB가 TIMESTAMP면 LocalDateTime으로 변경 */
    @Column(name = "date", nullable = false)
    private LocalDate date;

    /** 소프트 삭제 */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ===== 생성자 =====
    public Cashflow(Person person,
                    Category category,
                    Integer price,
                    String item,
                    Direction direction,
                    Double inflationRate,
                    LocalDate  date,
                    Integer changedPrice) {
        this.person = person;
        this.category = category;
        this.price = price;
        this.item = item;
        this.direction = direction;
        this.inflationRate = inflationRate;
        this.date = date;
        this.changedPrice = changedPrice;
    }

    // ===== 부분 업데이트(서비스에서 null 체크 후 넘기는 패턴) =====
    public Cashflow update(Category category,
                           Integer price,
                           String item,
                           Direction direction,
                           Double inflationRate,
                           LocalDate  date,
                           Integer changedPrice) {
        if (category != null) this.category = category;
        if (price != null) this.price = price;
        if (item != null) this.item = item;
        if (direction != null) this.direction = direction;
        if (inflationRate != null) this.inflationRate = inflationRate;
        if (date != null) this.date = date;
        if (changedPrice != null) this.changedPrice = changedPrice;
        return this;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}
