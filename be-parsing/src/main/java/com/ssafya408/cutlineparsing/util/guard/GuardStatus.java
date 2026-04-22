package com.ssafya408.cutlineparsing.util.guard;

public record GuardStatus(boolean ok) {
    public static GuardStatus success()   { return new GuardStatus(true); }
    public static GuardStatus fail() { return new GuardStatus(false); }

    public boolean isOk() { return ok; }

    @Override public String toString() {
        return ok ? "GuardStatus{ok}" : "GuardStatus{fail}";
    }
}