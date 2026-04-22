package com.ssafya408.cutlineparsing;

import com.ssafya408.cutlineparsing.util.guard.GuardConfig;
import com.ssafya408.cutlineparsing.util.Speaker;
import com.ssafya408.cutlineparsing.util.guard.TwoPersonGuard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TwoPersonGuardTest {

    @Test
    @DisplayName("내 이름과 상대 이름은 허용된다")
    void accept_user_and_friend_ok() {
        var cfg = GuardConfig.of("홍길동", "김형수");
        var guard = new TwoPersonGuard(cfg);

        assertDoesNotThrow(() -> guard.acceptSpeaker("홍길동"));
        assertDoesNotThrow(() -> guard.acceptSpeaker("김형수"));
    }

    @Test
    @DisplayName("제3자 화자는 거절된다")
    void reject_third_speaker() {
        var cfg = GuardConfig.of("홍길동", "김형수");
        var guard = new TwoPersonGuard(cfg);

        assertDoesNotThrow(() -> guard.acceptSpeaker("홍길동"));
        assertDoesNotThrow(() -> guard.acceptSpeaker("김형수"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> guard.acceptSpeaker("이순신"));
        assertTrue(ex.getMessage().contains("이순신"));
    }

    @Test
    @DisplayName("유저는 U로, 사람은 F로 매핑된다")
    void mapToUF_should_map_user_to_U_and_friend_to_F() {
        var cfg = GuardConfig.of("홍길동", "김형수");
        var guard = new TwoPersonGuard(cfg);

        assertEquals(Speaker.U, guard.mapToUF("홍길동"));
        assertEquals(Speaker.F, guard.mapToUF("김형수"));
        assertNull(guard.mapToUF("이순신")); // 제3자는 null 반환
    }
}
