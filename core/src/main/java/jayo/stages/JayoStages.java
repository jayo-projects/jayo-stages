/*
 * Copyright (c) 2026-present, pull-vert and Jayo contributors.
 * Use of this source code is governed by the Apache 2.0 license.
 */

package jayo.stages;

import jayo.stages.internal.JayoStagesUtils;
import jayo.stages.internal.RealStagesAsyncStream;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

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
     */
    public static <T> @NonNull Promesse<T> firstSuccessfulOrThrow(
            final Promesse.@NonNull Builder promesseBuilder,
            final @NonNull Collection<? extends @NonNull CompletionStage<T>> stages
    ) {
        Objects.requireNonNull(promesseBuilder);
        Objects.requireNonNull(stages);

        // do a protective copy to array
        @SuppressWarnings("unchecked") final CompletionStage<T>[] copy = stages.toArray(new CompletionStage[0]);
        return JayoStagesUtils.firstSuccessfulOrThrow(promesseBuilder, copy);
    }
}
