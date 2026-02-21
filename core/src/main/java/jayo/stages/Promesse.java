/*
 * Copyright (c) 2026-present, pull-vert and Jayo contributors.
 * Use of this source code is governed by the Apache 2.0 license.
 */

package jayo.stages;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import jayo.stages.internal.RealPromesse;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A {@link #cancel() cancelable} {@link CompletionStage}.
 *
 * @see Promesse.Completable
 */
public interface Promesse<T> extends CompletionStage<T> {
    static @NonNull Builder builder(final @NonNull Executor initialExecutor) {
        Objects.requireNonNull(initialExecutor);
        return new RealPromesse.Builder(initialExecutor);
    }

    /**
     * If not already completed, exceptionally completes this stage with a {@link CancellationException}. Dependent
     * stages that have not already completed will also complete exceptionally, with a {@link CompletionException}
     * caused by this {@code CancellationException}.
     *
     * @return {@code true} if this stage is now canceled.
     */
    boolean cancel();

    /**
     * @return the executor that will be used for all {@code *Async} methods without explicit {@link Executor} argument.
     */
    @NonNull Executor getExecutor();

    /**
     * A {@link Promesse} that must be explicitly completed.
     * <p>
     * Only the first invocation of one of the 3 terminal operations succeeds by returning {@code true}.
     * <ul>
     * <li>Success: {@link #complete complete}
     * <li>Failure: {@link #completeExceptionally completeExceptionally}
     * <li>Cancellation: {@link #cancel cancel}
     * </ul>
     */
    interface Completable<T> extends Promesse<T> {
        /**
         * If not already completed, completes this stage with the given result value.
         *
         * @param result the result value.
         * @return {@code true} if this invocation caused this stage to transition to a completed state.
         */
        boolean complete(final T result);

        /**
         * If not already completed, completes this stage with the given exception.
         *
         * @param ex the exception
         * @return {@code true} if this invocation caused this stage to transition to a completed state.
         */
        boolean completeExceptionally(final @NonNull Throwable ex);
    }

    /**
     * The builder used to create a {@link Promesse} instance.
     */
    sealed interface Builder permits RealPromesse.Builder {
        /**
         * Sets the default asynchronous execution facility that defines which {@link Executor} will be used for all
         * {@code *Async} methods without the explicit {@link Executor} argument:
         * <ul>
         * <li>{@link AsyncExecution#INITIAL_EXECUTOR}: always use the initial executor. This is the default option.
         * <li>{@link AsyncExecution#LAST_EXECUTOR}: propagate the latest explicit {@link Executor} passed to an
         * {@code *Async} method.
         * </ul>
         */
        @NonNull Builder asyncExecution(final @NonNull AsyncExecution asyncExecution);

        /**
         * Sets the {@link Runnable} to be invoked when this stage is canceled. It may be used to close or cancel the
         * underlying source of this stage (an IO socket, a database connection, etc.).
         * <p>
         * Note: This callback must execute fast and should not throw since it will be invoked synchronously.
         */
        @NonNull Builder onCancel(final @NonNull Runnable onCancel);

        /**
         * Asynchronously executes the given {@link Runnable} using the {@link #builder(Executor) initial executor}.
         */
        default @NonNull Promesse<@Nullable Void> run(final @NonNull Runnable runnable,
                                                      final boolean interruptWhenCanceled) {
            return call(
                    // transform Runnable to Callable
                    () -> {
                        runnable.run();
                        return null;
                    },
                    interruptWhenCanceled
            );
        }

        /**
         * Asynchronously executes the given {@link Callable} using the {@link #builder(Executor) initial executor}.
         */
        <T> @NonNull Promesse<T> call(final @NonNull Callable<T> call, final boolean interruptWhenCanceled);

        /**
         * Builds a {@link Completable completable Promesse} for explicit completion.
         */
        <T> @NonNull Completable<T> buildCompletable();
    }

    @Override
    <U> @NonNull Promesse<U> thenApply(final @NonNull Function<? super T, ? extends U> fn);

    @Override
    <U> @NonNull Promesse<U> thenApplyAsync(final @NonNull Function<? super T, ? extends U> fn);

    @Override
    <U> @NonNull Promesse<U> thenApplyAsync(final @NonNull Function<? super T, ? extends U> fn,
                                            final @NonNull Executor executor);

    @Override
    @NonNull Promesse<@Nullable Void> thenAccept(final @NonNull Consumer<? super T> action);

    @Override
    @NonNull Promesse<@Nullable Void> thenAcceptAsync(final @NonNull Consumer<? super T> action);

    @Override
    @NonNull Promesse<@Nullable Void> thenAcceptAsync(final @NonNull Consumer<? super T> action,
                                                      final @NonNull Executor executor);

    @Override
    @NonNull Promesse<@Nullable Void> thenRun(final @NonNull Runnable action);

    @Override
    @NonNull Promesse<@Nullable Void> thenRunAsync(final @NonNull Runnable action);

    @Override
    @NonNull Promesse<@Nullable Void> thenRunAsync(final @NonNull Runnable action, final @NonNull Executor executor);

    @Override
    <U, V> @NonNull Promesse<V> thenCombine(final @NonNull CompletionStage<? extends U> other,
                                            final @NonNull BiFunction<? super T, ? super U, ? extends V> fn);

    @Override
    <U, V> @NonNull Promesse<V> thenCombineAsync(final @NonNull CompletionStage<? extends U> other,
                                                 final @NonNull BiFunction<? super T, ? super U, ? extends V> fn);

    @Override
    <U, V> @NonNull Promesse<V> thenCombineAsync(final @NonNull CompletionStage<? extends U> other,
                                                 final @NonNull BiFunction<? super T, ? super U, ? extends V> fn,
                                                 final @NonNull Executor executor);

    @Override
    <U> @NonNull Promesse<Void> thenAcceptBoth(final @NonNull CompletionStage<? extends U> other,
                                               final @NonNull BiConsumer<? super T, ? super U> action);

    @Override
    <U> @NonNull Promesse<Void> thenAcceptBothAsync(final @NonNull CompletionStage<? extends U> other,
                                                    final @NonNull BiConsumer<? super T, ? super U> action);

    @Override
    default <U> @NonNull Promesse<Void> thenAcceptBothAsync(final @NonNull CompletionStage<? extends U> other,
                                                            final @NonNull BiConsumer<? super T, ? super U> action,
                                                            final @NonNull Executor executor) {
        Objects.requireNonNull(other);
        Objects.requireNonNull(action);
        Objects.requireNonNull(executor);

        return thenCombineAsync(
                other,
                // transform BiConsumer to BiFunction
                (t, u) -> {
                    action.accept(t, u);
                    return null;
                },
                executor
        );
    }

    @Override
    @NonNull Promesse<Void> runAfterBoth(final @NonNull CompletionStage<?> other,
                                         final @NonNull Runnable action);

    @Override
    @NonNull Promesse<@Nullable Void> runAfterBothAsync(final @NonNull CompletionStage<?> other,
                                                        final @NonNull Runnable action);

    @Override
    default @NonNull Promesse<@Nullable Void> runAfterBothAsync(final @NonNull CompletionStage<?> other,
                                                                final @NonNull Runnable action,
                                                                final @NonNull Executor executor) {
        Objects.requireNonNull(other);
        Objects.requireNonNull(action);
        Objects.requireNonNull(executor);

        return thenCombineAsync(
                other,
                // transform Runnable to BiFunction
                (t, r) -> {
                    action.run();
                    return null;
                },
                executor
        );
    }

    @Override
    <U> @NonNull Promesse<U> applyToEither(final @NonNull CompletionStage<? extends T> other,
                                           final @NonNull Function<? super T, U> fn);

    @Override
    <U> @NonNull Promesse<U> applyToEitherAsync(final @NonNull CompletionStage<? extends T> other,
                                                final @NonNull Function<? super T, U> fn);

    @Override
    <U> @NonNull Promesse<U> applyToEitherAsync(final @NonNull CompletionStage<? extends T> other,
                                                final @NonNull Function<? super T, U> fn,
                                                final @NonNull Executor executor);

    @Override
    @NonNull Promesse<@Nullable Void> acceptEither(final @NonNull CompletionStage<? extends T> other,
                                                   final @NonNull Consumer<? super T> action);

    @Override
    @NonNull Promesse<@Nullable Void> acceptEitherAsync(final @NonNull CompletionStage<? extends T> other,
                                                        final @NonNull Consumer<? super T> action);

    @Override
    @NonNull Promesse<@Nullable Void> acceptEitherAsync(final @NonNull CompletionStage<? extends T> other,
                                                        final @NonNull Consumer<? super T> action,
                                                        final @NonNull Executor executor);

    @Override
    @NonNull Promesse<@Nullable Void> runAfterEither(final @NonNull CompletionStage<?> other,
                                                     final @NonNull Runnable action);

    @Override
    @NonNull Promesse<@Nullable Void> runAfterEitherAsync(final @NonNull CompletionStage<?> other,
                                                          final @NonNull Runnable action);

    @Override
    @NonNull Promesse<@Nullable Void> runAfterEitherAsync(final @NonNull CompletionStage<?> other,
                                                          final @NonNull Runnable action,
                                                          final @NonNull Executor executor);

    @Override
    <U> @NonNull Promesse<U> thenCompose(final @NonNull Function<? super T, ? extends @NonNull CompletionStage<U>> fn);

    @Override
    <U> @NonNull Promesse<U> thenComposeAsync(
            final @NonNull Function<? super T, ? extends @NonNull CompletionStage<U>> fn);

    @Override
    <U> @NonNull Promesse<U> thenComposeAsync(
            final @NonNull Function<? super T, ? extends @NonNull CompletionStage<U>> fn,
            final @NonNull Executor executor);

    @Override
    @NonNull Promesse<T> exceptionally(final @NonNull Function<@NonNull Throwable, ? extends T> fn);

    @Override
    @NonNull Promesse<T> exceptionallyAsync(final @NonNull Function<@NonNull Throwable, ? extends T> fn);

    @Override
    @NonNull Promesse<T> exceptionallyAsync(final @NonNull Function<@NonNull Throwable, ? extends T> fn,
                                            final @NonNull Executor executor);

    @Override
    @NonNull Promesse<T> exceptionallyCompose(
            final @NonNull Function<@NonNull Throwable, ? extends @NonNull CompletionStage<T>> fn);

    @Override
    @NonNull Promesse<T> exceptionallyComposeAsync(
            final @NonNull Function<@NonNull Throwable, ? extends @NonNull CompletionStage<T>> fn);

    @Override
    @NonNull Promesse<T> exceptionallyComposeAsync(
            final @NonNull Function<@NonNull Throwable, ? extends @NonNull CompletionStage<T>> fn,
            final @NonNull Executor executor);

    @Override
    @NonNull Promesse<T> whenComplete(final @NonNull BiConsumer<? super T, ? super @Nullable Throwable> action);

    @Override
    @NonNull Promesse<T> whenCompleteAsync(final @NonNull BiConsumer<? super T, ? super @Nullable Throwable> action);

    @Override
    @NonNull Promesse<T> whenCompleteAsync(final @NonNull BiConsumer<? super T, ? super @Nullable Throwable> action,
                                           final @NonNull Executor executor);

    @Override
    <U> @NonNull Promesse<U> handle(final @NonNull BiFunction<? super T, @Nullable Throwable, ? extends U> fn);

    @Override
    <U> @NonNull Promesse<U> handleAsync(final @NonNull BiFunction<? super T, @Nullable Throwable, ? extends U> fn);

    @Override
    <U> @NonNull Promesse<U> handleAsync(final @NonNull BiFunction<? super T, @Nullable Throwable, ? extends U> fn,
                                         final @NonNull Executor executor);
}
