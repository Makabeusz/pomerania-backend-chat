package com.sojka.pomeranian.chat.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;

public class TestUtils {

    public static Comparator<LocalDateTime> timestampComparator() {
        return (o1, o2) -> {
            if (o1 == null && o2 == null) {
                return 0;
            } else if (o1 == null && o2 != null) {
                return 1;
            } else if (o1 != null && o2 == null) {
                return -1;
            }
            long diffInSeconds = Math.abs(Duration.between(o1, o2).getSeconds());
            return diffInSeconds <= 3 ? 0 : o1.compareTo(o2);
        };
    }
}
