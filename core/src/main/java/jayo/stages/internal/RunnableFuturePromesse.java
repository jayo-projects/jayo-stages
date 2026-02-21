/*
 * Copyright (c) 2026-present, pull-vert and Jayo contributors.
 * Use of this source code is governed by the Apache 2.0 license.
 */

package jayo.stages.internal;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.*;

final class RunnableFuturePromesse<T> extends RealPromesse<T> {
    private final @NonNull RunnableFuture<T> asyncTask;
    private final boolean interruptWhenCanceled;

    RunnableFuturePromesse(final @NonNull Executor executor,
                           final @Nullable Runnable onCancel,
                           final boolean useInitialExecutor,
                           final @NonNull Callable<T> callable,
                           final boolean interruptWhenCanceled) {
        super(executor, onCancel, useInitialExecutor);
        assert callable != null;

        this.interruptWhenCanceled = interruptWhenCanceled;
        this.asyncTask = new FutureTask<>(callable) {
            @Override
            protected void set(final T result) {
                super.set(result);
                complete(result);
            }

            @Override
            protected void setException(final @NonNull Throwable ex) {
                assert ex != null;

                super.setException(ex);
                completeExceptionally(new CompletionException(ex));
            }
        };
        // execute this async task with the given executor
        executor.execute(asyncTask);
    }

    @Override
    public boolean cancel() {
        final var canceled = asyncTask.cancel(interruptWhenCanceled);
        if (canceled) {
            completeExceptionally(new CancellationException());
        }
        return canceled;
    }
}
