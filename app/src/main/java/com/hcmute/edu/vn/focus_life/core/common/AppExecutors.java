package com.hcmute.edu.vn.focus_life.core.common;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AppExecutors {
    private final Executor diskIO = Executors.newSingleThreadExecutor();
    private final Executor networkIO = Executors.newFixedThreadPool(3);

    public Executor diskIO() {
        return diskIO;
    }

    public Executor networkIO() {
        return networkIO;
    }
}
