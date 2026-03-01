package ru.vladmikhayl.task_management.entity;

import java.util.LinkedHashSet;
import java.util.Set;

public final class WeekdaysMask {
    public static int toMask(Set<Integer> weekdays) {
        int mask = 0;
        for (int d : weekdays) {
            if (d < 0 || d > 6) throw new IllegalArgumentException("weekday должен быть в диапазоне 0..6");
            mask |= (1 << d);
        }
        return mask;
    }

    public static Set<Integer> toSet(int mask) {
        Set<Integer> days = new LinkedHashSet<>();
        for (int d = 0; d <= 6; d++) {
            if ((mask & (1 << d)) != 0) {
                days.add(d);
            }
        }
        return days;
    }
}
