package com.ssafya408.cutlineparsing.util.accumulator;
import com.ssafya408.cutlineparsing.util.Speaker;
import com.ssafya408.cutlineparsing.util.yearmonth.Turn;

import java.time.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class MonthlyStatsAccumulator {
    private final YearMonth ym;
    private int fMsgCount = 0;
    private int fMsgChars = 0;
    private final Set<LocalDate> activeDays = new HashSet<>();

    public MonthlyStatsAccumulator(YearMonth ym) { this.ym = ym; }

    public void onMessage(Speaker spk, ZonedDateTime dt, String text) {
        if (spk == Speaker.F) {
            fMsgCount++;
            fMsgChars += (text == null ? 0 : text.codePointCount(0, text.length()));
        }
        activeDays.add(dt.toLocalDate());
    }

    public ReplyStats computeReplyStats(List<Turn> turns) {
        long minutesSum = 0;
        int pairCount = 0;
        for (int i = 1; i < turns.size(); i++) {
            var prev = turns.get(i - 1);
            var cur  = turns.get(i);
            if (prev.speaker() == Speaker.U && cur.speaker() == Speaker.F) {
                long minutes = Duration.between(prev.end(), cur.start()).toMinutes();
                if (minutes > 0) minutesSum += minutes;
                pairCount++;
            }
        }
        return new ReplyStats(minutesSum, pairCount);
    }

    private int computeMaxNoChatStreak() {
        var start = ym.atDay(1);
        var end   = ym.atEndOfMonth();
        int streak = 0, max = 0;
        for (var d = start; !d.isAfter(end); d = d.plusDays(1)) {
            if (activeDays.contains(d)) streak = 0;
            else { streak++; if (streak > max) max = streak; }
        }
        return max;
    }

    public MonthlyStatsResult finalizeMonth(ReplyStats replyStats) {
        long replyMinutesSum = replyStats == null ? 0 : replyStats.replyMinutesSum();
        int replyPairsCount = replyStats == null ? 0 : replyStats.replyPairsCount();
        return new MonthlyStatsResult(
                ym, fMsgCount, fMsgChars, replyMinutesSum, replyPairsCount,
                activeDays.size(), computeMaxNoChatStreak()
        );
    }

    public record ReplyStats(long replyMinutesSum, int replyPairsCount) {}
}


