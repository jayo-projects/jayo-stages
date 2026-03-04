/*
 * Copyright (c) 2026-present, pull-vert and Jayo contributors.
 * Use of this source code is governed by the Apache 2.0 license.
 */

package jayo.stages.internal;

import jayo.stages.JayoStages;
import jayo.stages.Promesse;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

public class FirstSuccessfulOrThrowTest {
    private final Executor baseExecutor = Executors.newFixedThreadPool(2);
    private Promesse<Integer> promesse1;
    private Promesse<Integer> promesse2;

    @Test
    void allSuccesses() throws InterruptedException {
        var timestamp = new AtomicReference<Long>();
        var start = System.currentTimeMillis();
        var result = buildFirstSuccessfulOrThrow(0);
        result.thenRun(() -> timestamp.set(System.currentTimeMillis()));
        Thread.sleep(200L);
        assertThat(timestamp.get() - start).isCloseTo(100L, Offset.offset(75L));
    }

    @Test
    void oneFailure() throws InterruptedException {
        var timestamp = new AtomicReference<Long>();
        var start = System.currentTimeMillis();
        var result = buildFirstSuccessfulOrThrow(1);
        result.thenRun(() -> timestamp.set(System.currentTimeMillis()));
        Thread.sleep(300L);
        assertThat(timestamp.get() - start).isCloseTo(200L, Offset.offset(75L));
    }

    @Test
    void allFailures() throws InterruptedException {
        var exception = new AtomicReference<Throwable>();
        var result = buildFirstSuccessfulOrThrow(2);
        result.exceptionally(ex -> {
            exception.set(ex);
            return null;
        });
        Thread.sleep(300L);
        assertThat(exception.get()).isInstanceOf(CompletionException.class)
                .cause().isInstanceOf(RuntimeException.class)
                .hasMessage("test2");
    }

    @Test
    void cancel() throws InterruptedException {
        var exception = new AtomicReference<Throwable>();
        var exception1 = new AtomicReference<Throwable>();
        var exception2 = new AtomicReference<Throwable>();
        var result = buildFirstSuccessfulOrThrow(0);
        result.exceptionally(ex -> {
            exception.set(ex);
            return null;
        });
        promesse1.exceptionally(ex -> {
            exception1.set(ex);
            return null;
        });
        promesse2.exceptionally(ex -> {
            exception2.set(ex);
            return null;
        });
        result.cancel();
        Thread.sleep(100L);
        assertThat(exception.get()).isInstanceOf(CancellationException.class);
        assertThat(exception1.get()).isInstanceOf(CancellationException.class);
        assertThat(exception2.get()).isInstanceOf(CancellationException.class);
    }

    private Promesse<Integer> buildFirstSuccessfulOrThrow(int failures) {
        var promesseBuilder = Promesse.builder(baseExecutor);
        promesse1 = promesseBuilder.call(() -> {
            Thread.sleep(200L);
            if (failures >= 2) {
                throw new RuntimeException("test2");
            }
            return 2;
        }, false);
        promesse2 = promesseBuilder.call(() -> {
            Thread.sleep(100L);
            if (failures >= 1) {
                throw new RuntimeException("test1");
            }
            return 1;
        }, false);
        return JayoStages.firstSuccessfulOrThrow(baseExecutor, List.of(promesse1, promesse2));
    }
}
