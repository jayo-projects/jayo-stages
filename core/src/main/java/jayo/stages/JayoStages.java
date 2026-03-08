/*
 * Copyright (c) 2026-present, pull-vert and Jayo contributors.
 * Use of this source code is governed by the Apache 2.0 license.
 */

package jayo.stages;

import jayo.stages.internal.JayoStagesUtils;
import jayo.stages.internal.RealStagesAsyncStream;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

public final class JayoStages {
    // un-instantiable
    private JayoStages() {
    }

    /**
     * @return a {@link StagesAsyncStream} that streams the results of the given {@code stages}.
     */
    public static <T> @NonNull StagesAsyncStream<T> asyncStreamOf(
            final @NonNull Collection<? extends @NonNull CompletionStage<T>> stages
    ) {
        Objects.requireNonNull(stages);

        // do a protective copy to array
        @SuppressWarnings("unchecked") final CompletionStage<T>[] copy = stages.toArray(new CompletionStage[0]);
        return new RealStagesAsyncStream<>(copy);
    }

    /**
     * As soon as one stage succeeds, the returned stage completes with its result.
     * <p>
     * Then unfinished stages are cancelled if possible by calling:
     * <ul>
     * <li>{@linkplain java.util.concurrent.Future#cancel(boolean) Future.cancel(true)} (for
     * {@code CompletionStages} that implement {@code j.u.c.Future} like {@code j.u.c.CompletableFuture})
     * <li>{@link Promesse#cancel()} for Jayo Promesses.
     * </ul>
     * If all stages fail, the returned stage completes exceptionally with an exception from one of the failed stages.
     * <p>
     * If you {@linkplain Promesse#cancel() cancel} the returned stage, it will cancel all stages if possible.
     */
    public static <T> @NonNull Promesse<T> firstSuccessfulOrThrow(
            final @NonNull Executor executor,
            final @NonNull Collection<? extends @NonNull CompletionStage<T>> stages
    ) {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(stages);

        // do a protective copy to array
        @SuppressWarnings("unchecked") final CompletionStage<T>[] copy = stages.toArray(new CompletionStage[0]);
        return JayoStagesUtils.firstSuccessfulOrThrow(executor, copy);
    }

    /**
     * If all stages succeed, the returned stage completes with {@code null}.
     * <p>
     * If any stage fails, the returned stage completes exceptionally with that exception; then unfinished stages are
     * cancelled if possible by calling:
     * <ul>
     * <li>{@linkplain java.util.concurrent.Future#cancel(boolean) Future.cancel(true)} (for
     * {@code CompletionStages} that implement {@code j.u.c.Future} like {@code j.u.c.CompletableFuture})
     * <li>{@link Promesse#cancel()} for Jayo Promesses.
     * </ul>
     * If you {@linkplain Promesse#cancel() cancel} the returned stage, it will cancel all stages if possible.
     */
    public static @NonNull Promesse<@Nullable Void> allSuccessfulOrThrow(
            final @NonNull Executor executor,
            final @NonNull Collection<@NonNull CompletionStage<?>> stages
    ) {
        Objects.requireNonNull(executor);
        Objects.requireNonNull(stages);

        // do a protective copy to array
        final CompletionStage<?>[] copy = stages.toArray(new CompletionStage[0]);
        return JayoStagesUtils.allSuccessfulOrThrow(executor, copy);
    }
}
