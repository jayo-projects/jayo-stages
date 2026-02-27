/*
 * Copyright (c) 2026-present, pull-vert and Jayo contributors.
 * Use of this source code is governed by the Apache 2.0 license.
 *
 * Forked from the Lukáš Křečan CompletionStage implementation (https://github.com/lukas-krecan/completion-stage),
 * original copyright is below
 *
 * Copyright 2009-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jayo.stages.internal;

import jayo.stages.AsyncExecution;
import jayo.stages.Promesse;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static jayo.stages.internal.Callbacks.EmptyCallbacks;

public class RealPromesse<T> implements Promesse.Completable<T> {
    private final @NonNull Executor executor;
    private final @Nullable Runnable onCancel;
    private final boolean useInitialExecutor;

    @SuppressWarnings("FieldMayBeFinal")
    private volatile @NonNull Callbacks<T> callbacks = EmptyCallbacks.instance();
    private static final @NonNull VarHandle CALLBACKS_HANDLE;

    static {
        try {
            final var lookup = MethodHandles.lookup();
            CALLBACKS_HANDLE = lookup.findVarHandle(RealPromesse.class, "callbacks", Callbacks.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    RealPromesse(final @NonNull Executor executor,
                 final @Nullable Runnable onCancel,
                 final boolean useInitialExecutor) {
        assert executor != null;

        this.executor = executor;
        this.onCancel = onCancel;
        this.useInitialExecutor = useInitialExecutor;
    }

    @Override
    public final boolean complete(final T result) {
        while (true) {
            final var currentCallbacks = callbacks;

            // The stage is already completed, return false.
            if (currentCallbacks.isCompleted()) {
                return false;
            }

            // If we successfully switched to the success terminal step, we're done.
            final var success = currentCallbacks.complete(result);
            if (CALLBACKS_HANDLE.compareAndSet(this, currentCallbacks, success)) {
                currentCallbacks.callSuccessCallbacks(result);
                return true;
            }

            // We lost the race to mutate the callback stack. Try again!
        }
    }

    @Override
    public final boolean completeExceptionally(final @NonNull Throwable ex) {
        Objects.requireNonNull(ex);

        while (true) {
            final var currentCallbacks = callbacks;

            // The stage is already completed, return false.
            if (currentCallbacks.isCompleted()) {
                return false;
            }

            // If we successfully switched to the failure terminal step, we're done.
            final var failure = currentCallbacks.completeExceptionally(ex);
            if (CALLBACKS_HANDLE.compareAndSet(this, currentCallbacks, failure)) {
                // synchronously execute the onCancel callback if the stage was cancelled
                if (ex instanceof CancellationException && onCancel != null) {
                    try {
                        onCancel.run();
                    } catch (Throwable ex2) {
                        ex.addSuppressed(ex2);
                    }
                }
                currentCallbacks.callFailureCallbacks(ex);
                return true;
            }

            // We lost the race to mutate the callback stack. Try again!
        }
    }

    @Override
    public boolean cancel() {
        return completeExceptionally(new CancellationException());
    }

    @Override
    public final <U> @NonNull Promesse<U> thenApplyAsync(final @NonNull Function<? super T, ? extends U> fn,
                                                         final @NonNull Executor executor) {
        Objects.requireNonNull(fn);
        Objects.requireNonNull(executor);

        final RealPromesse<U> nextStage = newPromesse(executor);
        addCallbacks(
                result -> {
                    try {
                        nextStage.complete(fn.apply(result));
                    } catch (Throwable e) {
                        handleFailure(nextStage, e);
                    }
                },
                handleFailure(nextStage),
                executor
        );
        return nextStage;
    }

    @Override
    public final @NonNull Promesse<@Nullable Void> thenAcceptAsync(final @NonNull Consumer<? super T> action,
                                                                   final @NonNull Executor executor) {
        Objects.requireNonNull(action);
        Objects.requireNonNull(executor);

        return thenApplyAsync(convertConsumerToFunction(action), executor);
    }

    @Override
    public final @NonNull Promesse<@Nullable Void> thenRunAsync(final @NonNull Runnable action,
                                                                final @NonNull Executor executor) {
        Objects.requireNonNull(action);
        Objects.requireNonNull(executor);

        return thenApplyAsync(convertRunnableToFunction(action), executor);
    }

    @Override
    public final <U, V> @NonNull Promesse<V> thenCombineAsync(
            final @NonNull CompletionStage<? extends U> other,
            final @NonNull BiFunction<? super T, ? super U, ? extends V> fn,
            final @NonNull Executor executor
    ) {
        Objects.requireNonNull(other);
        Objects.requireNonNull(fn);
        Objects.requireNonNull(executor);

        return thenCompose(result1 ->
                other.thenApplyAsync(result2 -> fn.apply(result1, result2), executor));
    }

    @Override
    public final <U> @NonNull Promesse<U> applyToEitherAsync(final @NonNull CompletionStage<? extends T> other,
                                                             final @NonNull Function<? super T, U> fn,
                                                             final @NonNull Executor executor) {
        Objects.requireNonNull(other);
        Objects.requireNonNull(fn);
        Objects.requireNonNull(executor);

        return doApplyToEitherAsync(this, other, fn, executor);
    }


    @Override
    public final @NonNull Promesse<@Nullable Void> acceptEitherAsync(final @NonNull CompletionStage<? extends T> other,
                                                                     final @NonNull Consumer<? super T> action,
                                                                     final @NonNull Executor executor) {
        Objects.requireNonNull(other);
        Objects.requireNonNull(action);
        Objects.requireNonNull(executor);

        return applyToEitherAsync(other, convertConsumerToFunction(action), executor);
    }

    @Override
    public final @NonNull Promesse<@Nullable Void> runAfterEitherAsync(final @NonNull CompletionStage<?> other,
                                                                       final @NonNull Runnable action,
                                                                       final @NonNull Executor executor) {
        Objects.requireNonNull(other);
        Objects.requireNonNull(action);
        Objects.requireNonNull(executor);

        return doApplyToEitherAsync(this, other, convertRunnableToFunction(action), executor);
    }

    /**
     * This method exists just to reconcile generics when called from {@link #runAfterEitherAsync} which has an
     * unexpected type of parameter "other". The alternative is to ignore the compiler warning.
     */
    private <U, V> Promesse<V> doApplyToEitherAsync(final @NonNull CompletionStage<? extends U> first,
                                                    final @NonNull CompletionStage<? extends U> second,
                                                    final @NonNull Function<? super U, V> fn,
                                                    final @NonNull Executor executor) {
        assert first != null;
        assert second != null;
        assert fn != null;

        final RealPromesse<U> nextStage = newPromesse(executor);

        // the completion stage accepts only the first result, the other one is ignored
        BiConsumer<U, @Nullable Throwable> action = completeHandler(nextStage);
        first.whenComplete(action);
        second.whenComplete(action);

        return nextStage.thenApplyAsync(fn, executor);
    }

    @Override
    public final <U> @NonNull Promesse<U> thenComposeAsync(
            final @NonNull Function<? super T, ? extends @NonNull CompletionStage<U>> fn,
            final @NonNull Executor executor
    ) {
        Objects.requireNonNull(fn);
        Objects.requireNonNull(executor);

        final RealPromesse<U> nextStage = newPromesse(executor);
        addCallbacks(
                result -> {
                    try {
                        fn.apply(result).whenComplete(completeHandler(nextStage));
                    } catch (Throwable e) {
                        handleFailure(nextStage, e);
                    }
                },
                handleFailure(nextStage),
                executor
        );
        return nextStage;
    }

    @Override
    public final @NonNull Promesse<T> exceptionallyAsync(final @NonNull Function<@NonNull Throwable, ? extends T> fn,
                                                         final @NonNull Executor executor) {
        Objects.requireNonNull(fn);
        Objects.requireNonNull(executor);

        final RealPromesse<T> nextStage = newPromesse(executor);
        addCallbacks(
                nextStage::complete,
                ex -> {
                    try {
                        nextStage.complete(fn.apply(ex));
                    } catch (Throwable e) {
                        handleFailure(nextStage, e);
                    }
                },
                executor
        );
        return nextStage;
    }

    @Override
    public final @NonNull Promesse<T> exceptionallyComposeAsync(
            final @NonNull Function<@NonNull Throwable, ? extends @NonNull CompletionStage<T>> fn,
            final @NonNull Executor executor
    ) {
        Objects.requireNonNull(fn);
        Objects.requireNonNull(executor);

        final RealPromesse<T> nextStage = newPromesse(executor);
        addCallbacks(
                nextStage::complete,
                e1 -> {
                    try {
                        fn.apply(e1).whenComplete(completeHandler(nextStage));
                    } catch (Throwable e2) {
                        handleFailure(nextStage, e2);
                    }
                },
                executor
        );
        return nextStage;
    }

    @Override
    public final @NonNull Promesse<T> whenCompleteAsync(
            final @NonNull BiConsumer<? super T, ? super @Nullable Throwable> action,
            final @NonNull Executor executor
    ) {
        Objects.requireNonNull(action);
        Objects.requireNonNull(executor);

        final RealPromesse<T> nextStage = newPromesse(executor);
        addCallbacks(
                result -> {
                    try {
                        action.accept(result, null);
                        nextStage.complete(result);
                    } catch (Throwable ex) {
                        handleFailure(nextStage, ex);
                    }
                },
                ex1 -> {
                    try {
                        action.accept(null, ex1);
                        handleFailure(nextStage, ex1);
                    } catch (Throwable ex2) {
                        handleFailure(nextStage, ex2);
                    }
                },
                executor
        );
        return nextStage;
    }

    @Override
    public final <U> @NonNull Promesse<U> handleAsync(
            final @NonNull BiFunction<? super T, @Nullable Throwable, ? extends U> fn,
            final @NonNull Executor executor
    ) {
        Objects.requireNonNull(fn);
        Objects.requireNonNull(executor);

        final RealPromesse<U> nextStage = newPromesse(executor);
        addCallbacks(
                result -> {
                    try {
                        nextStage.complete(fn.apply(result, null));
                    } catch (Throwable e) {
                        handleFailure(nextStage, e);
                    }
                },
                // exceptions are treated as success
                ex -> {
                    try {
                        nextStage.complete(fn.apply(null, ex));
                    } catch (Throwable e) {
                        handleFailure(nextStage, e);
                    }
                },
                executor
        );
        return nextStage;
    }

    @Override
    public final @NonNull CompletableFuture<T> toCompletableFuture() {
        CompletableFuture<T> completableFuture = new CompletableFuture<>();
        addCallbacks(
                completableFuture::complete,
                completableFuture::completeExceptionally,
                SAME_THREAD_EXECUTOR
        );
        return completableFuture;
    }

    private <U> RealPromesse<U> newPromesse(final @NonNull Executor newExecutor) {
        assert newExecutor != null;

        return new RealPromesse<>(
                (useInitialExecutor || newExecutor == SAME_THREAD_EXECUTOR) ? executor : newExecutor,
                onCancel,
                useInitialExecutor);
    }

    private void addCallbacks(final @NonNull Consumer<? super T> successCallback,
                              final @NonNull Consumer<@NonNull Throwable> failureCallback,
                              final @NonNull Executor executor) {
        assert successCallback != null;
        assert failureCallback != null;
        assert executor != null;

        while (true) {
            final var currentCallbacks = callbacks;

            final var nextCallbacks = currentCallbacks.plus(successCallback, failureCallback, executor);
            if (nextCallbacks == null || CALLBACKS_HANDLE.compareAndSet(this, currentCallbacks, nextCallbacks)) {
                return;
            }

            // We lost the race to mutate the callback stack. Try again!
        }
    }

    private static <T> @NonNull Function<T, @Nullable Void> convertConsumerToFunction(
            final @NonNull Consumer<? super T> action
    ) {
        assert action != null;

        return result -> {
            action.accept(result);
            return null;
        };
    }

    private static <T> @NonNull Function<T, @Nullable Void> convertRunnableToFunction(final @NonNull Runnable action) {
        assert action != null;

        return result -> {
            action.run();
            return null;
        };
    }

    /**
     * Handler that can be used in whenComplete method.
     *
     * @return BiConsumer that passes values to this CompletionStage.
     */
    private static <T> @NonNull BiConsumer<T, @Nullable Throwable> completeHandler(
            final @NonNull RealPromesse<T> promesse
    ) {
        assert promesse != null;

        return (result, ex) -> {
            if (ex == null) {
                promesse.complete(result);
            } else {
                handleFailure(promesse, ex);
            }
        };
    }

    /**
     * Wraps exception completes exceptionally.
     */
    private static @NonNull Consumer<@NonNull Throwable> handleFailure(final @NonNull RealPromesse<?> promesse) {
        assert promesse != null;
        return (ex) -> handleFailure(promesse, ex);
    }

    private static void handleFailure(final @NonNull RealPromesse<?> promesse, final @NonNull Throwable ex) {
        assert promesse != null;
        assert ex != null;

        promesse.completeExceptionally(wrapException(ex));
    }

    /**
     * Wraps {@code ex} to a {@link CompletionException} if needed.
     */
    private static @NonNull Throwable wrapException(final @NonNull Throwable ex) {
        assert ex != null;

        if (ex instanceof CompletionException) {
            return ex;
        } else {
            return new CompletionException(ex);
        }
    }

    public static final class Builder implements Promesse.Builder {
        private final @NonNull Executor executor;
        private boolean useInitialExecutor = true;
        private @Nullable Runnable onCancel = null;

        public Builder(final @NonNull Executor executor) {
            assert executor != null;
            this.executor = executor;
        }

        @Override
        public @NonNull Builder asyncExecution(final @NonNull AsyncExecution asyncExecution) {
            Objects.requireNonNull(asyncExecution);
            useInitialExecutor = (asyncExecution == AsyncExecution.INITIAL_EXECUTOR);
            return this;
        }

        @Override
        public @NonNull Builder onCancel(final @NonNull Runnable onCancel) {
            this.onCancel = Objects.requireNonNull(onCancel);
            return this;
        }

        @Override
        public <T> @NonNull Promesse<T> call(final @NonNull Callable<T> callable,
                                             final boolean interruptWhenCancelled) {
            Objects.requireNonNull(callable);
            return new RunnableFuturePromesse<>(executor, onCancel, useInitialExecutor, callable, interruptWhenCancelled);
        }

        @Override
        public <T> @NonNull Completable<T> buildCompletable() {
            return new RealPromesse<>(executor, onCancel, useInitialExecutor);
        }
    }

    //region Boring

    private static final @NonNull Executor SAME_THREAD_EXECUTOR = new Executor() {
        @Override
        public void execute(final @NonNull Runnable command) {
            assert command != null;
            command.run();
        }

        @Override
        public @NonNull String toString() {
            return "SAME_THREAD_EXECUTOR";
        }
    };

    @Override
    public final <U> @NonNull Promesse<U> thenApply(final @NonNull Function<? super T, ? extends U> fn) {
        return thenApplyAsync(fn, SAME_THREAD_EXECUTOR);
    }

    @Override
    public final <U> @NonNull Promesse<U> thenApplyAsync(final @NonNull Function<? super T, ? extends U> fn) {
        return thenApplyAsync(fn, executor);
    }

    @Override
    public final @NonNull Promesse<@Nullable Void> thenAccept(final @NonNull Consumer<? super T> action) {
        return thenAcceptAsync(action, SAME_THREAD_EXECUTOR);
    }

    @Override
    public final @NonNull Promesse<@Nullable Void> thenAcceptAsync(final @NonNull Consumer<? super T> action) {
        return thenAcceptAsync(action, executor);
    }

    @Override
    public final @NonNull Promesse<@Nullable Void> thenRun(final @NonNull Runnable action) {
        return thenRunAsync(action, SAME_THREAD_EXECUTOR);
    }

    @Override
    public final @NonNull Promesse<@Nullable Void> thenRunAsync(final @NonNull Runnable action) {
        return thenRunAsync(action, executor);
    }

    @Override
    public final <U, V> @NonNull Promesse<V> thenCombine(
            final @NonNull CompletionStage<? extends U> other,
            final @NonNull BiFunction<? super T, ? super U, ? extends V> fn
    ) {
        return thenCombineAsync(other, fn, SAME_THREAD_EXECUTOR);
    }

    @Override
    public final <U, V> @NonNull Promesse<V> thenCombineAsync(
            final @NonNull CompletionStage<? extends U> other,
            final @NonNull BiFunction<? super T, ? super U, ? extends V> fn
    ) {
        return thenCombineAsync(other, fn, executor);
    }

    @Override
    public final <U> @NonNull Promesse<@Nullable Void> thenAcceptBoth(
            final @NonNull CompletionStage<? extends U> other,
            final @NonNull BiConsumer<? super T, ? super U> action
    ) {
        return thenAcceptBothAsync(other, action, SAME_THREAD_EXECUTOR);
    }

    @Override
    public final <U> @NonNull Promesse<@Nullable Void> thenAcceptBothAsync(
            final @NonNull CompletionStage<? extends U> other,
            final @NonNull BiConsumer<? super T, ? super U> action
    ) {
        return thenAcceptBothAsync(other, action, executor);
    }

    @Override
    public final @NonNull Promesse<@Nullable Void> runAfterBoth(final @NonNull CompletionStage<?> other,
                                                                final @NonNull Runnable action) {
        return runAfterBothAsync(other, action, SAME_THREAD_EXECUTOR);
    }

    @Override
    public final @NonNull Promesse<@Nullable Void> runAfterBothAsync(final @NonNull CompletionStage<?> other,
                                                                     final @NonNull Runnable action) {
        return runAfterBothAsync(other, action, executor);
    }

    @Override
    public final <U> @NonNull Promesse<U> applyToEither(final @NonNull CompletionStage<? extends T> other,
                                                        final @NonNull Function<? super T, U> fn) {
        return applyToEitherAsync(other, fn, SAME_THREAD_EXECUTOR);
    }

    @Override
    public final <U> @NonNull Promesse<U> applyToEitherAsync(final @NonNull CompletionStage<? extends T> other,
                                                             final @NonNull Function<? super T, U> fn) {
        return applyToEitherAsync(other, fn, executor);
    }

    @Override
    public final @NonNull Promesse<@Nullable Void> acceptEither(final @NonNull CompletionStage<? extends T> other,
                                                                final @NonNull Consumer<? super T> action) {
        return acceptEitherAsync(other, action, SAME_THREAD_EXECUTOR);
    }

    @Override
    public final @NonNull Promesse<@Nullable Void> acceptEitherAsync(final @NonNull CompletionStage<? extends T> other,
                                                                     final @NonNull Consumer<? super T> action) {
        return acceptEitherAsync(other, action, executor);
    }

    @Override
    public final @NonNull Promesse<@Nullable Void> runAfterEither(final @NonNull CompletionStage<?> other,
                                                                  final @NonNull Runnable action) {
        return runAfterEitherAsync(other, action, SAME_THREAD_EXECUTOR);
    }

    @Override
    public final @NonNull Promesse<@Nullable Void> runAfterEitherAsync(final @NonNull CompletionStage<?> other,
                                                                       final @NonNull Runnable action) {
        return runAfterEitherAsync(other, action, executor);
    }

    @Override
    public final <U> @NonNull Promesse<U> thenCompose(
            final @NonNull Function<? super T, ? extends @NonNull CompletionStage<U>> fn
    ) {
        return thenComposeAsync(fn, SAME_THREAD_EXECUTOR);
    }

    @Override
    public final <U> @NonNull Promesse<U> thenComposeAsync(
            final @NonNull Function<? super T, ? extends @NonNull CompletionStage<U>> fn
    ) {
        return thenComposeAsync(fn, executor);
    }

    @Override
    public final @NonNull Promesse<T> exceptionally(final @NonNull Function<@NonNull Throwable, ? extends T> fn) {
        return exceptionallyAsync(fn, SAME_THREAD_EXECUTOR);
    }

    @Override
    public final @NonNull Promesse<T> exceptionallyAsync(final @NonNull Function<@NonNull Throwable, ? extends T> fn) {
        return exceptionallyAsync(fn, executor);
    }

    @Override
    public final @NonNull Promesse<T> exceptionallyCompose(
            final @NonNull Function<@NonNull Throwable, ? extends @NonNull CompletionStage<T>> fn
    ) {
        return exceptionallyComposeAsync(fn, SAME_THREAD_EXECUTOR);
    }

    @Override
    public final @NonNull Promesse<T> exceptionallyComposeAsync(
            final @NonNull Function<@NonNull Throwable, ? extends @NonNull CompletionStage<T>> fn
    ) {
        return exceptionallyComposeAsync(fn, executor);
    }

    @Override
    public final @NonNull Promesse<T> whenComplete(
            final @NonNull BiConsumer<? super T, ? super @Nullable Throwable> action
    ) {
        return whenCompleteAsync(action, SAME_THREAD_EXECUTOR);
    }

    @Override
    public @NonNull Promesse<T> whenCompleteAsync(
            final @NonNull BiConsumer<? super T, ? super @Nullable Throwable> action
    ) {
        return whenCompleteAsync(action, executor);
    }

    @Override
    public final <U> @NonNull Promesse<U> handle(
            final @NonNull BiFunction<? super T, @Nullable Throwable, ? extends U> fn
    ) {
        return handleAsync(fn, SAME_THREAD_EXECUTOR);
    }

    @Override
    public final <U> @NonNull Promesse<U> handleAsync(
            final @NonNull BiFunction<? super T, @Nullable Throwable, ? extends U> fn
    ) {
        return handleAsync(fn, executor);
    }

    @Override
    public final @NonNull Executor getExecutor() {
        return this.executor;
    }

    //endregion
}
