/*
 * Copyright (c) 2026-present, pull-vert and Jayo contributors.
 * Use of this source code is governed by the Apache 2.0 license.
 */

package jayo.stages;

import jayo.stages.internal.RealStagesAsyncStream;
import jayo.stream.AsyncStream;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * An {@link AsyncStream} that streams the results of several {@linkplain CompletionStage CompletionStages} of the same
 * type.
 *
 * @see JayoStages#asyncStreamOf(Collection)
 */
public sealed interface StagesAsyncStream<T> extends AsyncStream<T> permits RealStagesAsyncStream {
    <U> @NonNull StagesAsyncStream<U> map(final @NonNull Function<? super T, ? extends U> fn);

    <U> @NonNull StagesAsyncStream<U> mapAsync(final @NonNull Function<? super T, ? extends U> fn);

    <U> @NonNull StagesAsyncStream<U> mapAsync(final @NonNull Executor executor,
                                               final @NonNull Function<? super T, ? extends U> fn);

    <U> @NonNull StagesAsyncStream<U> mapCompose(
            final @NonNull Function<? super T, ? extends @NonNull CompletionStage<U>> fn);

    <U> @NonNull StagesAsyncStream<U> mapComposeAsync(
            final @NonNull Function<? super T, ? extends @NonNull CompletionStage<U>> fn);

    <U> @NonNull StagesAsyncStream<U> mapComposeAsync(
            final @NonNull Executor executor,
            final @NonNull Function<? super T, ? extends @NonNull CompletionStage<U>> fn);


    @NonNull StagesAsyncStream<T> recover(final @NonNull Function<@NonNull Throwable, ? extends T> fn);

    @NonNull StagesAsyncStream<T> recoverAsync(final @NonNull Function<@NonNull Throwable, ? extends T> fn);

    @NonNull StagesAsyncStream<T> recoverAsync(final @NonNull Executor executor,
                                               final @NonNull Function<@NonNull Throwable, ? extends T> fn);

    @NonNull StagesAsyncStream<T> recoverCompose(
            final @NonNull Function<@NonNull Throwable, ? extends @NonNull CompletionStage<T>> fn);

    @NonNull StagesAsyncStream<T> recoverComposeAsync(
            final @NonNull Function<@NonNull Throwable, ? extends @NonNull CompletionStage<T>> fn);

    @NonNull StagesAsyncStream<T> recoverComposeAsync(
            final @NonNull Executor executor,
            final @NonNull Function<@NonNull Throwable, ? extends @NonNull CompletionStage<T>> fn);


    void whenEachCompletesAsync(final @NonNull BiConsumer<? super T, ? super @Nullable Throwable> action);

    void whenEachCompletesAsync(final @NonNull Executor executor,
                                final @NonNull BiConsumer<? super T, ? super @Nullable Throwable> action);
}
