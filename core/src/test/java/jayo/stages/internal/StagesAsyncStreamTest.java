/*
 * Copyright (c) 2026-present, pull-vert and Jayo contributors.
 * Use of this source code is governed by the Apache 2.0 license.
 */

package jayo.stages.internal;

import jayo.result.Result;
import jayo.stages.Promesse;
import jayo.stages.StagesAsyncStream;
import jayo.stream.AsyncStream;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

public class StagesAsyncStreamTest {
    private final Executor baseExecutor = Executors.newFixedThreadPool(2);
    private final Executor otherExecutor = Executors.newFixedThreadPool(2);
    private final Promesse.Builder promesseBuilder = Promesse.builder(baseExecutor);

    @Test
    void asyncStreamIsAsync() { // Yes it is :)
        var timestamps = Collections.synchronizedList(new ArrayList<Long>());
        var start = System.currentTimeMillis();
        var asyncStream = buildAsyncStream();
        asyncStream.forEach(ignored -> timestamps.add(System.currentTimeMillis()));
        assertThat(timestamps).hasSize(2);
        assertThat(timestamps.get(0) - start).isCloseTo(100L, Offset.offset(75L));
        assertThat(timestamps.get(1) - start).isCloseTo(200L, Offset.offset(75L));
    }

    @Test
    void map() {
        var asyncStream = buildAsyncStream()
                .map(i -> i * 2);
        assertThatAsyncStreamContains(asyncStream, 2, 4);
    }

    @Test
    void mapAsync() {
        var asyncStream = buildAsyncStream()
                .mapAsync(i -> i * 2);
        assertThatAsyncStreamContains(asyncStream, 2, 4);
    }

    @Test
    void mapAsyncWithExecutor() {
        var asyncStream = buildAsyncStream()
                .mapAsync(otherExecutor, i -> i * 2);
        assertThatAsyncStreamContains(asyncStream, 2, 4);
    }

    @Test
    void mapCompose() {
        var asyncStream = buildAsyncStream()
                .mapCompose(i -> CompletableFuture.completedStage(i * 2));
        assertThatAsyncStreamContains(asyncStream, 2, 4);
    }

    @Test
    void mapComposeAsync() {
        var asyncStream = buildAsyncStream()
                .mapComposeAsync(i -> CompletableFuture.completedStage(i * 2));
        assertThatAsyncStreamContains(asyncStream, 2, 4);
    }

    @Test
    void mapComposeAsyncWithExecutor() {
        var asyncStream = buildAsyncStream()
                .mapComposeAsync(otherExecutor, i -> CompletableFuture.completedStage(i * 2));
        assertThatAsyncStreamContains(asyncStream, 2, 4);
    }

    @Test
    void recover() {
        var asyncStream = buildAsyncStream(true)
                .recover(ex -> {
                    if (ex instanceof CompletionException &&
                            ex.getCause() instanceof RuntimeException &&
                            "test".equals(ex.getCause().getMessage())) {
                        return 42;
                    } else {
                        return -1;
                    }
                });
        assertThatAsyncStreamContains(asyncStream, 1, 42);
    }

    @Test
    void recoverAsync() {
        var asyncStream = buildAsyncStream(true)
                .recoverAsync(ex -> {
                    if (ex instanceof CompletionException &&
                            ex.getCause() instanceof RuntimeException &&
                            "test".equals(ex.getCause().getMessage())) {
                        return 42;
                    } else {
                        return -1;
                    }
                });
        assertThatAsyncStreamContains(asyncStream, 1, 42);
    }

    @Test
    void recoverAsyncWithExecutor() {
        var asyncStream = buildAsyncStream(true)
                .recoverAsync(otherExecutor, ex -> {
                    if (ex instanceof CompletionException &&
                            ex.getCause() instanceof RuntimeException &&
                            "test".equals(ex.getCause().getMessage())) {
                        return 42;
                    } else {
                        return -1;
                    }
                });
        assertThatAsyncStreamContains(asyncStream, 1, 42);
    }

    @Test
    void recoverCompose() {
        var asyncStream = buildAsyncStream(true)
                .recoverCompose(ex -> {
                    if (ex instanceof CompletionException &&
                            ex.getCause() instanceof RuntimeException &&
                            "test".equals(ex.getCause().getMessage())) {
                        return CompletableFuture.completedStage(42);
                    } else {
                        return CompletableFuture.completedStage(-1);
                    }
                });
        assertThatAsyncStreamContains(asyncStream, 1, 42);
    }

    @Test
    void recoverComposeAsync() {
        var asyncStream = buildAsyncStream(true)
                .recoverComposeAsync(ex -> {
                    if (ex instanceof CompletionException &&
                            ex.getCause() instanceof RuntimeException &&
                            "test".equals(ex.getCause().getMessage())) {
                        return CompletableFuture.completedStage(42);
                    } else {
                        return CompletableFuture.completedStage(-1);
                    }
                });
        assertThatAsyncStreamContains(asyncStream, 1, 42);
    }

    @Test
    void recoverComposeAsyncWithExecutor() {
        var asyncStream = buildAsyncStream(true)
                .recoverComposeAsync(otherExecutor, ex -> {
                    if (ex instanceof CompletionException &&
                            ex.getCause() instanceof RuntimeException &&
                            "test".equals(ex.getCause().getMessage())) {
                        return CompletableFuture.completedStage(42);
                    } else {
                        return CompletableFuture.completedStage(-1);
                    }
                });
        assertThatAsyncStreamContains(asyncStream, 1, 42);
    }

    @Test
    void whenEachCompletes() {
        var asyncStream = buildAsyncStream(true);
        var resultFuture = new CompletableFuture<Integer>();
        var exceptionFuture = new CompletableFuture<Throwable>();
        asyncStream.whenEachCompletes((result, ex) -> {
            if (ex != null) {
                exceptionFuture.complete(ex);
            } else {
                resultFuture.complete(result);
            }
        });
        assertThat(resultFuture.join()).isEqualTo(1);
        assertThat(exceptionFuture.join()).isInstanceOf(CompletionException.class)
                .cause().isInstanceOf(RuntimeException.class)
                .hasMessage("test");
    }

    @Test
    void whenEachCompletesAsync() {
        var asyncStream = buildAsyncStream(true);
        var resultFuture = new CompletableFuture<Integer>();
        var exceptionFuture = new CompletableFuture<Throwable>();
        asyncStream.whenEachCompletesAsync((result, ex) -> {
            if (ex != null) {
                exceptionFuture.complete(ex);
            } else {
                resultFuture.complete(result);
            }
        });
        assertThat(resultFuture.join()).isEqualTo(1);
        assertThat(exceptionFuture.join()).isInstanceOf(CompletionException.class)
                .cause().isInstanceOf(RuntimeException.class)
                .hasMessage("test");
    }

    @Test
    void whenEachCompletesAsyncWithExecutor() {
        var asyncStream = buildAsyncStream(true);
        var resultFuture = new CompletableFuture<Integer>();
        var exceptionFuture = new CompletableFuture<Throwable>();
        asyncStream.whenEachCompletesAsync(otherExecutor, (result, ex) -> {
            if (ex != null) {
                exceptionFuture.complete(ex);
            } else {
                resultFuture.complete(result);
            }
        });
        assertThat(resultFuture.join()).isEqualTo(1);
        assertThat(exceptionFuture.join()).isInstanceOf(CompletionException.class)
                .cause().isInstanceOf(RuntimeException.class)
                .hasMessage("test");
    }

    @Test
    void asJavaStreamContains() {
        var asyncStream = buildAsyncStream();
        var javaStream = asyncStream.stream();
        assertThat(javaStream).containsExactly(Result.success(1), Result.success(2));
    }

    @Test
    void asJavaStreamIsAsync() {
        var timestamps = Collections.synchronizedList(new ArrayList<Long>());
        var start = System.currentTimeMillis();
        var asyncStream = buildAsyncStream();
        asyncStream.stream()
                .forEach(ignored -> timestamps.add(System.currentTimeMillis()));
        assertThat(timestamps).hasSize(2);
        assertThat(timestamps.get(0) - start).isCloseTo(100L, Offset.offset(75L));
        assertThat(timestamps.get(1) - start).isCloseTo(200L, Offset.offset(75L));
    }

    private StagesAsyncStream<Integer> buildAsyncStream() {
        return buildAsyncStream(false);
    }

    private StagesAsyncStream<Integer> buildAsyncStream(boolean willThrow) {
        var promesse1 = promesseBuilder.call(() -> {
            Thread.sleep(200L);
            if (willThrow) {
                throw new RuntimeException("test");
            }
            return 2;
        }, false);
        var promesse2 = promesseBuilder.call(() -> {
            Thread.sleep(100L);
            return 1;
        }, false);
        return StagesAsyncStream.of(List.of(promesse1, promesse2));
    }

    private void assertThatAsyncStreamContains(AsyncStream<Integer> asyncStream,
                                               int value1,
                                               int value2) {
        assertThat(asyncStream).containsExactly(Result.success(value1), Result.success(value2));
    }
}
