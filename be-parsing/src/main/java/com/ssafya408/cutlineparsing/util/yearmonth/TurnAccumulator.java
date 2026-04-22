package com.ssafya408.cutlineparsing.util.yearmonth;

import com.ssafya408.cutlineparsing.util.Speaker;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TurnAccumulator {
    private Speaker curSpk;
    private ZonedDateTime curStart; // 분 단위
    private ZonedDateTime curEnd;   // 분 단위
    private StringBuilder buf;
    private int msgCount;
    private final List<Turn> out = new ArrayList<>();

    public void ingest(Speaker spk, ZonedDateTime dt, String text) {
        Objects.requireNonNull(spk, "speaker must not be null");
        Objects.requireNonNull(dt,  "datetime must not be null");

        final String payload = (text == null) ? "" : text;
        final ZonedDateTime minute = floorToMinute(dt);

        if (curSpk == null) {
            // 첫 메시지로 새 턴 시작
            startNewTurn(spk, minute, payload);
            return;
        }

        final boolean sameSpeaker = (spk == curSpk);
        final boolean sameMinute  = minute.equals(curStart);

        if (sameSpeaker && sameMinute) {
            // 같은 턴: 텍스트 병합 + end 갱신 + 카운트 증가
            if (buf.length() > 0) buf.append('\n');
            buf.append(payload);
            curEnd = minute;
            msgCount++;
        } else {
            // 턴 경계: 기존 턴 flush 후 새 턴 시작
            closeTurn();
            startNewTurn(spk, minute, payload);
        }
    }

    public List<Turn> flush() {
        closeTurn();
        if (out.isEmpty()) return List.of();
        List<Turn> res = new ArrayList<>(out);
        out.clear();
        return res;
    }

    // -------------------- 내부 유틸 --------------------

    private static ZonedDateTime floorToMinute(ZonedDateTime z) {
        return z.withSecond(0).withNano(0);
    }

    private void startNewTurn(Speaker spk, ZonedDateTime minute, String payload) {
        this.curSpk   = spk;
        this.curStart = minute;
        this.curEnd   = minute;
        this.buf      = new StringBuilder(payload);
        this.msgCount = 1;
    }

    private void closeTurn() {
        if (curSpk == null) return;
        out.add(new Turn(curSpk, curStart, curEnd, buf.toString(), msgCount));
        // 상태 초기화
        curSpk = null;
        curStart = null;
        curEnd = null;
        buf = null;
        msgCount = 0;
    }
}
