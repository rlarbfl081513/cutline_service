package com.ssafya408.cutlineparsing.util.guard;

import com.ssafya408.cutlineparsing.util.Speaker;

import java.util.LinkedHashSet;
import java.util.Optional;

public final class TwoPersonGuard {
    private final GuardConfig cfg;
    private final LinkedHashSet<String> seen = new LinkedHashSet<>();

    public TwoPersonGuard(GuardConfig cfg) { this.cfg = cfg; }

    public GuardStatus acceptSpeaker(String speakerName) {
        if (speakerName == null || speakerName.isBlank()) {
            throw new IllegalArgumentException("화자 이름이 비어 있습니다.");
        }

        if (isUser(speakerName) || isFriend(speakerName)) {
            seen.add(speakerName);
            if (seen.size() > 2) {
                throw new IllegalArgumentException(String.format(
                        "지원되지 않는 다인 대화가 감지되었습니다. 허용된 화자: '%s', '%s'",
                        cfg.userName(), cfg.friendName()));
            }
            return GuardStatus.success();
        }

        throw new IllegalArgumentException(String.format(
                "등록되지 않은 화자 '%s' 가 포함된 대화는 지원하지 않습니다.",
                speakerName));
    }

    public Speaker mapToUF(String speakerName) {
        if (isUser(speakerName))   return Speaker.U;
        if (isFriend(speakerName)) return Speaker.F;
        return null;
    }

    private boolean isUser(String s)   { return cfg.userName().equals(s); }
    private boolean isFriend(String s) { return cfg.friendName().equals(s); }
}
