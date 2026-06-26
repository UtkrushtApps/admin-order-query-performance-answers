package com.company.observability;

import org.springframework.stereotype.Component;

@Component
public class SqlStatementCounter {
    private final ThreadLocal<Integer> count = ThreadLocal.withInitial(() -> 0);

    public void increment() {
        count.set(count.get() + 1);
    }

    public int current() {
        return count.get();
    }

    public void reset() {
        count.set(0);
    }

    public void clear() {
        count.remove();
    }
}
