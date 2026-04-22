package com.a308.cutline.domain.cashflow.service;

import com.a308.cutline.common.dao.CategoryRepository;
import com.a308.cutline.common.entity.Category;
import com.a308.cutline.common.entity.Person;
import com.a308.cutline.domain.cashflow.dao.CashflowRepository;
import com.a308.cutline.domain.cashflow.dao.projection.CategoryDirectionSum;
import com.a308.cutline.domain.cashflow.dto.CashflowCreateRequest;
import com.a308.cutline.domain.cashflow.dto.CashflowResponse;
import com.a308.cutline.domain.cashflow.dto.CashflowFullResponse;
import com.a308.cutline.domain.cashflow.dto.CategoryTotal;
import com.a308.cutline.domain.cashflow.entity.Cashflow;
import com.a308.cutline.domain.cashflow.entity.Direction;
import com.a308.cutline.domain.person.dao.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Map.entry;

@Service
@RequiredArgsConstructor
public class CashflowService {

    // ===============================
    // 0) 연차별 물가(%) 매핑 (기존 유지)
    // ===============================
    private static final NavigableMap<Integer, Double> INFLATION_BY_YEARS =
            new TreeMap<>(Map.ofEntries(
                    entry(1,  2.32), // 2024
                    entry(2,  3.60), // 2023
                    entry(3,  5.09), // 2022
                    entry(4,  2.50), // 2021
                    entry(5,  0.54), // 2020
                    entry(6,  0.38), // 2019
                    entry(7,  1.48), // 2018
                    entry(8,  1.94), // 2017
                    entry(9,  0.97), // 2016
                    entry(10, 0.71), // 2015
                    entry(11, 1.27), // 2014
                    entry(12, 1.30), // 2013
                    entry(13, 2.19), // 2012
                    entry(14, 4.03), // 2011
                    entry(15, 2.94), // 2010
                    entry(16, 2.76), // 2009
                    entry(17, 4.67), // 2008
                    entry(18, 2.53), // 2007
                    entry(19, 2.24), // 2006
                    entry(20, 2.75)  // 2005
            ));

    private static double resolveInflationByYears(int years) {
        if (years <= 0) return 0.0;
        var direct = INFLATION_BY_YEARS.get(years);
        if (direct != null) return direct;
        var floor = INFLATION_BY_YEARS.floorEntry(years);
        return (floor != null) ? floor.getValue() : 0.0;
    }

    /** price × (1 + rate%)^years → 반올림 정수 */
    private static Integer computeChangedPrice(Integer price, int years, double annualRatePercent) {
        if (price == null) return null;
        if (years <= 0 || annualRatePercent == 0.0) return price;

        BigDecimal p = BigDecimal.valueOf(price);
        BigDecimal r = BigDecimal.valueOf(annualRatePercent)
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        BigDecimal factor = BigDecimal.ONE.add(r).pow(years); // (1 + r)^years
        return p.multiply(factor).setScale(0, RoundingMode.HALF_UP).intValue();
    }

    // ===============================
    // 1) 의존성
    // ===============================
    private final CashflowRepository cashflowRepository;
    private final PersonRepository personRepository;
    private final CategoryRepository categoryRepository;

    // ===============================
    // 2) 생성 (기존 유지)
    // ===============================
    @Transactional
    public CashflowResponse create(Long personId, CashflowCreateRequest req) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 person"));

        Category category = categoryRepository.findById(req.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 category"));

        Direction dir = req.direction(); // GIVE / TAKE
        LocalDate d = (req.date() == null) ? LocalDate.now() : req.date();

        // 경과 연수(과거→양수, 미래→0)
        int years = (int) Math.max(0, ChronoUnit.YEARS.between(d, LocalDate.now()));

        // 내부 계산
        Double inflation = resolveInflationByYears(years);
        Integer changed  = computeChangedPrice(req.price(), years, inflation);

        Cashflow entity = new Cashflow(
                person,
                category,
                req.price(),
                req.item(),
                dir,
                inflation,
                d,              // 엔티티가 LocalDateTime이라면: d.atStartOfDay()
                changed
        );

        // person 누적 업데이트 (원본 price 기준)
        int delta = (req.price() == null) ? 0 : req.price();
        // 만약 changed 기준으로 누적하려면 위 주석 교체:
        // int delta = (changed == null) ? 0 : changed;

        if (dir == Direction.GIVE) {
            person.addGive(delta);
        } else if (dir == Direction.TAKE) {
            person.addTake(delta);
        }

        return CashflowResponse.from(cashflowRepository.save(entity));
    }

    // ===============================
    // 3) 전체 히스토리 (비페이징) 최신순
    // ===============================
    @Transactional(readOnly = true)
    public List<CashflowResponse> listAll(Long personId) {
        // person 존재 검증
        personRepository.findById(personId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 person"));

        return cashflowRepository
                .findAllByPerson_IdAndDeletedAtIsNullOrderByDateDescIdDesc(personId)
                .stream()
                .map(CashflowResponse::from)
                .collect(Collectors.toList());
    }

    // ===============================
    // 4) 소프트 삭제 (기존 유지)
    // ===============================
    @Transactional
    public void softDelete(Long cashflowId) {
        Cashflow cf = cashflowRepository.findById(cashflowId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 cashflow"));
        cf.delete();
    }

    // ===============================
    // 5) 최신 changed_price 조회 (기존 유지)
    // ===============================
    @Transactional(readOnly = true)
    public Optional<Integer> getLatestChangedPrice(Long personId, Long categoryId) {
        return cashflowRepository
                .findTopByPerson_IdAndCategory_IdAndDirectionAndChangedPriceIsNotNullAndDeletedAtIsNullOrderByDateDescIdDesc(
                        personId, categoryId, Direction.TAKE)  // ★ TAKE만
                .map(Cashflow::getChangedPrice);
    }

    // ===============================
    // 6) 요약: 히스토리 + 카테고리별 합 + totalGive/totalTake
    // ===============================
    @Transactional(readOnly = true)
    public CashflowFullResponse getSummary(Long personId) {
        // person 존재 검증
        personRepository.findById(personId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 person"));

        // 6-1) 히스토리 (최신순)
        List<CashflowResponse> history = cashflowRepository
                .findAllByPerson_IdAndDeletedAtIsNullOrderByDateDescIdDesc(personId)
                .stream()
                .map(CashflowResponse::from)
                .collect(Collectors.toList());

        // 6-2) 카테고리 × 방향 합계 (이름 없이 ID만)
        List<CategoryDirectionSum> rows = cashflowRepository.sumByCategoryAndDirection(personId);

        Map<Long, CategoryTotal> acc = new LinkedHashMap<>();
        long totalGive = 0L;
        long totalTake = 0L;

        for (CategoryDirectionSum r : rows) {
            Long catId = r.getCategoryId();
            long amt = Optional.ofNullable(r.getAmount()).orElse(0L);

            CategoryTotal dto = acc.computeIfAbsent(catId, CategoryTotal::new);

            switch (r.getDirection()) {
                case GIVE -> {
                    dto.setGive(dto.getGive() + amt);
                    totalGive += amt;
                }
                case TAKE -> {
                    dto.setTake(dto.getTake() + amt);
                    totalTake += amt;
                }
            }
        }

        // 6-3) 순액 계산 및 정렬(총액 내림차순)
        List<CategoryTotal> categoryTotals = new ArrayList<>(acc.values());
        for (CategoryTotal c : categoryTotals) c.setNet(c.getTake() - c.getGive());
        categoryTotals.sort(Comparator.comparingLong(
                (CategoryTotal c) -> (c.getGive() + c.getTake())
        ).reversed());

        long net = totalTake - totalGive;

        return new CashflowFullResponse(history, categoryTotals, totalGive, totalTake, net);
    }
}
