package com.ssafya408.cutlineparsing.util.guard;

import java.util.Objects;

public record GuardConfig(String userName, String friendName) {
    public GuardConfig {
        Objects.requireNonNull(userName,  "userName");
        Objects.requireNonNull(friendName,"friendName");
    }
    public static GuardConfig of(String user, String friend) {
        return new GuardConfig(user, friend);
    }
}