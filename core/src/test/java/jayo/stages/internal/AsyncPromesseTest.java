/*
 * Copyright (c) 2026-present, pull-vert and Jayo contributors.
 * Use of this source code is governed by the Apache 2.0 license.
 */

package jayo.stages.internal;

import org.junit.jupiter.api.Test;
import jayo.stages.Promesse;

import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Fail.fail;

public class AsyncPromesseTest {
    private final Executor baseExecutor = Executors.newFixedThreadPool(2);
    private final Executor otherExecutor = Executors.newFixedThreadPool(2);
    private static final Exception EXCEPTION = new Exception("test");

    @Test
    public void completableAsyncSuccess() {
        var completablePromesse = Promesse.builder(baseExecutor).<Integer>buildCompletable();
        var result = new CompletableFuture<Integer>();
        baseExecutor.execute(() -> {
            sleep();
            completablePromesse.complete(2);
        });
        completablePromesse.thenApplyAsync(i -> i * 2)
                .thenApplyAsync(i -> i - 1)
                .thenAcceptAsync(result::complete);
        assertThat(result.join()).isEqualTo(3);
    }

    @Test
    public void callableAsyncSuccess() {
        var completablePromesse = Promesse.builder(baseExecutor).call(
                () -> {
                    sleep();
                    return 2;
                }, false
        );
        var result = new CompletableFuture<Integer>();
        completablePromesse.thenApplyAsync(i -> i * 2)
                .thenApplyAsync(i -> i - 1)
                .thenAcceptAsync(result::complete);
        assertThat(result.join()).isEqualTo(3);
    }

    @Test
    public void completableAsyncOtherExecutorSuccess() {
        var completablePromesse = Promesse.builder(baseExecutor).<Integer>buildCompletable();
        var result = new CompletableFuture<Integer>();
        baseExecutor.execute(() -> {
            sleep();
            completablePromesse.complete(2);
        });
        completablePromesse.thenApplyAsync(i -> i * 2, otherExecutor)
                .thenApplyAsync(i -> i - 1)
                .thenAcceptAsync(result::complete, otherExecutor);
        assertThat(result.join()).isEqualTo(3);
    }

    @Test
    public void callableAsyncOtherExecutorSuccess() {
        var completablePromesse = Promesse.builder(baseExecutor).call(
                () -> {
                    sleep();
                    return 2;
                }, false
        );
        var result = new CompletableFuture<Integer>();
        completablePromesse.thenApplyAsync(i -> i * 2, otherExecutor)
                .thenApplyAsync(i -> i - 1)
                .thenAcceptAsync(result::complete, otherExecutor);
        assertThat(result.join()).isEqualTo(3);
    }

    @Test
    public void completableAsyncFailure() {
        var completablePromesse = Promesse.builder(baseExecutor).<Integer>buildCompletable();
        var result = new CompletableFuture<Integer>();
        baseExecutor.execute(() -> {
            sleep();
            completablePromesse.completeExceptionally(EXCEPTION);
        });
        completablePromesse.thenApplyAsync(i -> i * 2)
                .thenApplyAsync(i -> i - 1)
                .exceptionallyAsync(ex -> {
                    result.completeExceptionally(ex);
                    return null;
                });
        assertThatThrownBy(result::join)
                .isInstanceOf(CompletionException.class)
                .cause().isSameAs(EXCEPTION);
    }

    @Test
    public void callableAsyncFailure() {
        var completablePromesse = Promesse.builder(baseExecutor).<Integer>call(
                () -> {
                    sleep();
                    throw EXCEPTION;
                }, false
        );
        var result = new CompletableFuture<Integer>();
        completablePromesse.thenApplyAsync(i -> i * 2)
                .thenApplyAsync(i -> i - 1)
                .exceptionallyAsync(ex -> {
                    result.completeExceptionally(ex);
                    return null;
                });
        assertThatThrownBy(result::join)
                .isInstanceOf(CompletionException.class)
                .cause().isSameAs(EXCEPTION);
    }

    @Test
    public void completableAsyncOtherExecutorFailure() {
        var completablePromesse = Promesse.builder(baseExecutor).<Integer>buildCompletable();
        var result = new CompletableFuture<Integer>();
        baseExecutor.execute(() -> {
            sleep();
            completablePromesse.completeExceptionally(EXCEPTION);
        });
        completablePromesse.thenApplyAsync(i -> i * 2, otherExecutor)
                .thenApplyAsync(i -> i - 1)
                .exceptionallyAsync(ex -> {
                    result.completeExceptionally(ex);
                    return null;
                }, otherExecutor);
        assertThatThrownBy(result::join)
                .isInstanceOf(CompletionException.class)
                .cause().isSameAs(EXCEPTION);
    }

    @Test
    public void callableAsyncOtherExecutorFailure() {
        var completablePromesse = Promesse.builder(baseExecutor).<Integer>call(
                () -> {
                    sleep();
                    throw EXCEPTION;
                }, false
        );
        var result = new CompletableFuture<Integer>();
        completablePromesse.thenApplyAsync(i -> i * 2, otherExecutor)
                .thenApplyAsync(i -> i - 1)
                .exceptionallyAsync(ex -> {
                    result.completeExceptionally(ex);
                    return null;
                }, otherExecutor);
        assertThatThrownBy(result::join)
                .isInstanceOf(CompletionException.class)
                .cause().isSameAs(EXCEPTION);
    }

    private static void sleep() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            fail(e);
        }
    }

    @Test
    public void completableAsyncCanceled() {
        var interrupted = new CompletableFuture<Boolean>();
        var cancelCallback = new CompletableFuture<Boolean>();
        var completablePromesse = Promesse.builder(baseExecutor)
                .onCancel(() -> cancelCallback.complete(true))
                .<Integer>buildCompletable();
        var result = new CompletableFuture<Integer>();
        baseExecutor.execute(() -> {
            try {
                Thread.sleep(200);
                interrupted.complete(false);
                completablePromesse.complete(2); // returns false because already canceled
            } catch (InterruptedException e) {
                interrupted.complete(true);
                throw new RuntimeException(e);
            }
        });
        completablePromesse.thenApplyAsync(i -> i * 2)
                .thenApplyAsync(i -> i - 1)
                .exceptionallyAsync(ex -> {
                    result.completeExceptionally(ex);
                    return null;
                });
        // cancel in the main thread before completion
        sleep();
        var canceled = completablePromesse.cancel();

        assertThat(canceled).isTrue();
        assertThat(interrupted.join()).isFalse();
        assertThat(cancelCallback.join()).isTrue();
        assertThatThrownBy(result::join)
                .isInstanceOf(CompletionException.class)
                .cause().isInstanceOf(CancellationException.class);
    }

    @Test
    public void callableAsyncCanceledNoInterrupt() {
        var interrupted = new CompletableFuture<Boolean>();
        var cancelCallback = new CompletableFuture<Boolean>();
        var completablePromesse = Promesse.builder(baseExecutor)
                .onCancel(() -> cancelCallback.complete(true))
                .call(
                        () -> {
                            try {
                                Thread.sleep(200);
                                interrupted.complete(false);
                                return 2;
                            } catch (InterruptedException e) {
                                interrupted.complete(true);
                                throw new RuntimeException(e);
                            }
                        }, false // do not interrupt on cancel
                );
        var result = new CompletableFuture<Integer>();
        completablePromesse.thenApplyAsync(i -> i * 2)
                .thenApplyAsync(i -> i - 1)
                .exceptionallyAsync(ex -> {
                    result.completeExceptionally(ex);
                    return null;
                });
        // cancel in the main thread before completion
        sleep();
        var canceled = completablePromesse.cancel();

        assertThat(canceled).isTrue();
        assertThat(interrupted.join()).isFalse();
        assertThat(cancelCallback.join()).isTrue();
        assertThatThrownBy(result::join)
                .isInstanceOf(CompletionException.class)
                .cause().isInstanceOf(CancellationException.class);
    }

    @Test
    public void callableAsyncCanceledInterrupt() {
        var interrupted = new CompletableFuture<Boolean>();
        var cancelCallback = new CompletableFuture<Boolean>();
        var completablePromesse = Promesse.builder(baseExecutor)
                .onCancel(() -> cancelCallback.complete(true))
                .call(
                        () -> {
                            try {
                                Thread.sleep(200);
                                interrupted.complete(false);
                                return 2;
                            } catch (InterruptedException e) {
                                interrupted.complete(true);
                                throw new RuntimeException(e);
                            }
                        }, true // interrupt on cancel
                );
        var result = new CompletableFuture<Integer>();
        completablePromesse.thenApplyAsync(i -> i * 2)
                .thenApplyAsync(i -> i - 1)
                .exceptionallyAsync(ex -> {
                    result.completeExceptionally(ex);
                    return null;
                });
        // cancel in the main thread before completion
        sleep();
        var canceled = completablePromesse.cancel();

        assertThat(canceled).isTrue();
        assertThat(interrupted.join()).isTrue();
        assertThat(cancelCallback.join()).isTrue();
        assertThatThrownBy(result::join)
                .isInstanceOf(CompletionException.class)
                .cause().isInstanceOf(CancellationException.class);
    }

    @Test
    public void completableAsyncOtherExecutorCanceled() {
        var interrupted = new CompletableFuture<Boolean>();
        var cancelCallback = new CompletableFuture<Boolean>();
        var completablePromesse = Promesse.builder(baseExecutor)
                .onCancel(() -> cancelCallback.complete(true))
                .<Integer>buildCompletable();
        var result = new CompletableFuture<Integer>();
        baseExecutor.execute(() -> {
            try {
                Thread.sleep(200);
                interrupted.complete(false);
                completablePromesse.complete(2); // returns false because already canceled
            } catch (InterruptedException e) {
                interrupted.complete(true);
                throw new RuntimeException(e);
            }
        });
        completablePromesse.thenApplyAsync(i -> i * 2, otherExecutor)
                .thenApplyAsync(i -> i - 1)
                .exceptionallyAsync(ex -> {
                    result.completeExceptionally(ex);
                    return null;
                }, otherExecutor);
        // cancel in the main thread before completion
        sleep();
        var canceled = completablePromesse.cancel();

        assertThat(canceled).isTrue();
        assertThat(interrupted.join()).isFalse();
        assertThat(cancelCallback.join()).isTrue();
        assertThatThrownBy(result::join)
                .isInstanceOf(CompletionException.class)
                .cause().isInstanceOf(CancellationException.class);
    }

    @Test
    public void callableAsyncOtherExecutorCanceledNoInterrupt() {
        var interrupted = new CompletableFuture<Boolean>();
        var cancelCallback = new CompletableFuture<Boolean>();
        var completablePromesse = Promesse.builder(baseExecutor)
                .onCancel(() -> cancelCallback.complete(true))
                .call(
                        () -> {
                            try {
                                Thread.sleep(200);
                                interrupted.complete(false);
                                return 2;
                            } catch (InterruptedException e) {
                                interrupted.complete(true);
                                throw new RuntimeException(e);
                            }
                        }, false // do not interrupt on cancel
                );
        var result = new CompletableFuture<Integer>();
        completablePromesse.thenApplyAsync(i -> i * 2, otherExecutor)
                .thenApplyAsync(i -> i - 1)
                .exceptionallyAsync(ex -> {
                    result.completeExceptionally(ex);
                    return null;
                }, otherExecutor);
        // cancel in the main thread before completion
        sleep();
        var canceled = completablePromesse.cancel();

        assertThat(canceled).isTrue();
        assertThat(interrupted.join()).isFalse();
        assertThat(cancelCallback.join()).isTrue();
        assertThatThrownBy(result::join)
                .isInstanceOf(CompletionException.class)
                .cause().isInstanceOf(CancellationException.class);
    }

    @Test
    public void callableAsyncOtherExecutorCanceledInterrupt() {
        var interrupted = new CompletableFuture<Boolean>();
        var cancelCallback = new CompletableFuture<Boolean>();
        var completablePromesse = Promesse.builder(baseExecutor)
                .onCancel(() -> cancelCallback.complete(true))
                .call(
                        () -> {
                            try {
                                Thread.sleep(200);
                                interrupted.complete(false);
                                return 2;
                            } catch (InterruptedException e) {
                                interrupted.complete(true);
                                throw new RuntimeException(e);
                            }
                        }, true // interrupt on cancel
                );
        var result = new CompletableFuture<Integer>();
        completablePromesse.thenApplyAsync(i -> i * 2, otherExecutor)
                .thenApplyAsync(i -> i - 1)
                .exceptionallyAsync(ex -> {
                    result.completeExceptionally(ex);
                    return null;
                }, otherExecutor);
        // cancel in the main thread before completion
        sleep();
        var canceled = completablePromesse.cancel();

        assertThat(canceled).isTrue();
        assertThat(interrupted.join()).isTrue();
        assertThat(cancelCallback.join()).isTrue();
        assertThatThrownBy(result::join)
                .isInstanceOf(CompletionException.class)
                .cause().isInstanceOf(CancellationException.class);
    }
}
