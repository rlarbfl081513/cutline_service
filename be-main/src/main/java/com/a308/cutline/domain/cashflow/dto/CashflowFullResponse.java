package com.a308.cutline.domain.cashflow.dto;

import java.util.List;

/**
 * 현금흐름 요약 응답 DTO (일반 클래스 버전)
 * - history        : 개별 Cashflow 응답 목록
 * - categoryTotals : 카테고리별 집계
 * - totalGive      : 총 지출
 * - totalTake      : 총 수입
 * - net            : 순액 (totalTake - totalGive)
 */

public class CashflowFullResponse {

    private List<CashflowResponse> history;
    private List<CategoryTotal> categoryTotals;
    private long totalGive;
    private long totalTake;
    private long net;

    public CashflowFullResponse() {
    }

    public CashflowFullResponse(List<CashflowResponse> history,
                                   List<CategoryTotal> categoryTotals,
                                   long totalGive,
                                   long totalTake,
                                   long net) {
        this.history = history;
        this.categoryTotals = categoryTotals;
        this.totalGive = totalGive;
        this.totalTake = totalTake;
        this.net = net;
    }

    // ---- getters ----
    public List<CashflowResponse> getHistory() {
        return history;
    }

    public List<CategoryTotal> getCategoryTotals() {
        return categoryTotals;
    }

    public long getTotalGive() {
        return totalGive;
    }

    public long getTotalTake() {
        return totalTake;
    }

    public long getNet() {
        return net;
    }

    // ---- setters ----
    public void setHistory(List<CashflowResponse> history) {
        this.history = history;
    }

    public void setCategoryTotals(List<CategoryTotal> categoryTotals) {
        this.categoryTotals = categoryTotals;
    }

    public void setTotalGive(long totalGive) {
        this.totalGive = totalGive;
    }

    public void setTotalTake(long totalTake) {
        this.totalTake = totalTake;
    }

    public void setNet(long net) {
        this.net = net;
    }

    @Override
    public String toString() {
        return "CashflowSummaryResponse{" +
                "history=" + (history == null ? 0 : history.size()) +
                ", categoryTotals=" + (categoryTotals == null ? 0 : categoryTotals.size()) +
                ", totalGive=" + totalGive +
                ", totalTake=" + totalTake +
                ", net=" + net +
                '}';
    }
}
