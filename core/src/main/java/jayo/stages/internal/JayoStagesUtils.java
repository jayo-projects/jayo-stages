/*
 * Copyright (c) 2026-present, pull-vert and Jayo contributors.
 * Use of this source code is governed by the Apache 2.0 license.
 */

package jayo.stages.internal;

import jayo.stages.Promesse;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public final class JayoStagesUtils {
    // un-instantiable
    private JayoStagesUtils() {
    }

    public static <T> @NonNull Promesse<T> firstSuccessfulOrThrow(
            final Promesse.@NonNull Builder promesseBuilder,
            final @NonNull CompletionStage<T> @NonNull [] stages
    ) {
        assert promesseBuilder != null;
        assert stages != null;

        final var failureCount = new AtomicInteger(stages.length);
        final var promesse = promesseBuilder.<T>buildCompletable();
        for (final var stage : stages) {
            stage.whenComplete((result, ex) -> {
                if (ex == null) {
                    promesse.complete(result);

                    // cancel other stages after this success
                    Arrays.stream(stages)
                            .filter(s -> s != stage)
                            .forEach(JayoStagesUtils::tryCancel);
                } else if (failureCount.decrementAndGet() == 0) {
                    promesse.completeExceptionally(ex);
                }
            });
        }
        return promesse;
    }

    private static <T> void tryCancel(final @NonNull CompletionStage<T> stage) {
        assert stage != null;

        if (stage instanceof Promesse<T> promesse) {
            promesse.cancel();
        } else if (stage instanceof Future<?> future) {
            future.cancel(true);
        }
    }
}
