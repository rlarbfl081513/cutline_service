package com.ssafya408.cutlineparsing.util.yearmonth;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MonthRouter {

    /**
     * Turn.start 기준으로 정렬한 뒤 YearMonth 버킷으로 묶는다.
     * 결과는 월(YearMonth) 순서가 입력 시계열을 따르도록 LinkedHashMap으로 유지.
     */
    public Map<YearMonth, List<Turn>> route(List<Turn> turns) {
        Map<YearMonth, List<Turn>> out = new LinkedHashMap<>();
        if (turns == null || turns.isEmpty()) return out;

        List<Turn> sorted = new ArrayList<>(turns);
        sorted.sort(java.util.Comparator.comparing(Turn::start).thenComparing(Turn::end));

        for (Turn t : sorted) {
            YearMonth ym = YearMonth.from(t.start().toLocalDate());
            out.computeIfAbsent(ym, __ -> new ArrayList<>()).add(t);
        }
        return out;
    }
}
