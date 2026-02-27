/*
 * Copyright (c) 2026-present, pull-vert and Jayo contributors.
 * Use of this source code is governed by the Apache 2.0 license.
 */

package jayo.stages.internal;

import jayo.result.Result;
import jayo.stages.StagesAsyncStream;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public final class RealStagesAsyncStream<T> implements StagesAsyncStream<T> {
    private final @NonNull CompletionStage<T> @NonNull [] stages;

    // used only in iterator.
    @SuppressWarnings("FieldMayBeFinal")
    private volatile /* lateinit */ BlockingArrayList<T> results = null;


    private static final @NonNull VarHandle RESULTS_HANDLE;

    static {
        try {
            final var lookup = MethodHandles.lookup();
            RESULTS_HANDLE = lookup.findVarHandle(RealStagesAsyncStream.class, "results", BlockingArrayList.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public RealStagesAsyncStream(final @NonNull CompletionStage<T> @NonNull [] stages) {
        assert stages != null;
        this.stages = stages;
    }

    @Override
    public @NonNull <U> RealStagesAsyncStream<U> map(final @NonNull Function<? super T, ? extends U> fn) {
        Objects.requireNonNull(fn);

        final CompletionStage<U>[] nextStages = new CompletionStage[stages.length];
        for (var i = 0; i < stages.length; i++) {
            nextStages[i] = stages[i].thenApply(fn);
        }
        return new RealStagesAsyncStream<>(nextStages);
    }

    @Override
    public @NonNull <U> RealStagesAsyncStream<U> mapAsync(final @NonNull Function<? super T, ? extends U> fn) {
        Objects.requireNonNull(fn);

        final CompletionStage<U>[] nextStages = new CompletionStage[stages.length];
        for (var i = 0; i < stages.length; i++) {
            nextStages[i] = stages[i].thenApplyAsync(fn);
        }
        return new RealStagesAsyncStream<>(nextStages);
    }

    @Override
    public @NonNull <U> RealStagesAsyncStream<U> mapAsync(final @NonNull Executor executor,
                                                          final @NonNull Function<? super T, ? extends U> fn) {
        Objects.requireNonNull(fn);
        Objects.requireNonNull(executor);

        final CompletionStage<U>[] nextStages = new CompletionStage[stages.length];
        for (var i = 0; i < stages.length; i++) {
            nextStages[i] = stages[i].thenApplyAsync(fn, executor);
        }
        return new RealStagesAsyncStream<>(nextStages);
    }

    @Override
    public @NonNull <U> RealStagesAsyncStream<U> mapCompose(
            final @NonNull Function<? super T, ? extends @NonNull CompletionStage<U>> fn
    ) {
        Objects.requireNonNull(fn);

        final CompletionStage<U>[] nextStages = new CompletionStage[stages.length];
        for (var i = 0; i < stages.length; i++) {
            nextStages[i] = stages[i].thenCompose(fn);
        }
        return new RealStagesAsyncStream<>(nextStages);
    }

    @Override
    public @NonNull <U> RealStagesAsyncStream<U> mapComposeAsync(
            final @NonNull Function<? super T, ? extends @NonNull CompletionStage<U>> fn
    ) {
        Objects.requireNonNull(fn);

        final CompletionStage<U>[] nextStages = new CompletionStage[stages.length];
        for (var i = 0; i < stages.length; i++) {
            nextStages[i] = stages[i].thenComposeAsync(fn);
        }
        return new RealStagesAsyncStream<>(nextStages);
    }

    @Override
    public @NonNull <U> RealStagesAsyncStream<U> mapComposeAsync(
            final @NonNull Executor executor,
            final @NonNull Function<? super T, ? extends @NonNull CompletionStage<U>> fn
    ) {
        Objects.requireNonNull(fn);
        Objects.requireNonNull(executor);

        final CompletionStage<U>[] nextStages = new CompletionStage[stages.length];
        for (var i = 0; i < stages.length; i++) {
            nextStages[i] = stages[i].thenComposeAsync(fn, executor);
        }
        return new RealStagesAsyncStream<>(nextStages);
    }

    @Override
    public @NonNull RealStagesAsyncStream<T> recover(final @NonNull Function<@NonNull Throwable, ? extends T> fn) {
        Objects.requireNonNull(fn);

        final CompletionStage<T>[] nextStages = new CompletionStage[stages.length];
        for (var i = 0; i < stages.length; i++) {
            nextStages[i] = stages[i].exceptionally(fn);
        }
        return new RealStagesAsyncStream<>(nextStages);
    }

    @Override
    public @NonNull RealStagesAsyncStream<T> recoverAsync(final @NonNull Function<@NonNull Throwable, ? extends T> fn) {
        Objects.requireNonNull(fn);

        final CompletionStage<T>[] nextStages = new CompletionStage[stages.length];
        for (var i = 0; i < stages.length; i++) {
            nextStages[i] = stages[i].exceptionallyAsync(fn);
        }
        return new RealStagesAsyncStream<>(nextStages);
    }

    @Override
    public @NonNull RealStagesAsyncStream<T> recoverAsync(
            final @NonNull Executor executor,
            final @NonNull Function<@NonNull Throwable, ? extends T> fn
    ) {
        Objects.requireNonNull(fn);
        Objects.requireNonNull(executor);

        final CompletionStage<T>[] nextStages = new CompletionStage[stages.length];
        for (var i = 0; i < stages.length; i++) {
            nextStages[i] = stages[i].exceptionallyAsync(fn, executor);
        }
        return new RealStagesAsyncStream<>(nextStages);
    }

    @Override
    public @NonNull RealStagesAsyncStream<T> recoverCompose(
            final @NonNull Function<@NonNull Throwable, ? extends @NonNull CompletionStage<T>> fn
    ) {
        Objects.requireNonNull(fn);

        final CompletionStage<T>[] nextStages = new CompletionStage[stages.length];
        for (var i = 0; i < stages.length; i++) {
            nextStages[i] = stages[i].exceptionallyCompose(fn);
        }
        return new RealStagesAsyncStream<>(nextStages);
    }

    @Override
    public @NonNull RealStagesAsyncStream<T> recoverComposeAsync(
            final @NonNull Function<@NonNull Throwable, ? extends @NonNull CompletionStage<T>> fn
    ) {
        Objects.requireNonNull(fn);

        final CompletionStage<T>[] nextStages = new CompletionStage[stages.length];
        for (var i = 0; i < stages.length; i++) {
            nextStages[i] = stages[i].exceptionallyComposeAsync(fn);
        }
        return new RealStagesAsyncStream<>(nextStages);
    }

    @Override
    public @NonNull RealStagesAsyncStream<T> recoverComposeAsync(
            final @NonNull Executor executor,
            final @NonNull Function<@NonNull Throwable, ? extends @NonNull CompletionStage<T>> fn
    ) {
        Objects.requireNonNull(fn);
        Objects.requireNonNull(executor);

        final CompletionStage<T>[] nextStages = new CompletionStage[stages.length];
        for (var i = 0; i < stages.length; i++) {
            nextStages[i] = stages[i].exceptionallyComposeAsync(fn, executor);
        }
        return new RealStagesAsyncStream<>(nextStages);
    }

    @Override
    public void whenEachCompletes(final @NonNull BiConsumer<? super T, ? super @Nullable Throwable> action) {
        Objects.requireNonNull(action);

        for (final var stage : stages) {
            stage.whenComplete(action);
        }
    }

    @Override
    public void whenEachCompletesAsync(final @NonNull BiConsumer<? super T, ? super @Nullable Throwable> action) {
        Objects.requireNonNull(action);

        for (final var stage : stages) {
            stage.whenCompleteAsync(action);
        }
    }

    public void whenEachCompletesAsync(final @NonNull Executor executor,
                                       final @NonNull BiConsumer<? super T, ? super @Nullable Throwable> action) {
        Objects.requireNonNull(action);
        Objects.requireNonNull(executor);

        for (final var stage : stages) {
            stage.whenCompleteAsync(action, executor);
        }
    }

    @Override
    public @NonNull Spliterator<@NonNull Result<T>> spliterator() {
        return Spliterators.spliterator(
                iterator(),
                stages.length,
                (Spliterator.NONNULL | Spliterator.IMMUTABLE | Spliterator.ORDERED));
    }

    @Override
    public @NonNull Iterator<@NonNull Result<T>> iterator() {
        // init results if needed
        if (results == null) {
            final var newResults = new BlockingArrayList<T>(stages.length);
            if (RESULTS_HANDLE.compareAndSet(this, null, newResults)) {
                whenEachCompletes((result, exception) ->
                        results.add((exception != null) ? Result.failure(exception) : Result.success(result)));
            }
        }
        return new Itr();
    }

    private final class Itr implements Iterator<@NonNull Result<T>> {
        private int index = 0;

        @Override
        public boolean hasNext() {
            return index < stages.length;
        }

        @Override
        public @NonNull Result<T> next() {
            return results.get(index++);
        }
    }

    private static final class BlockingArrayList<T> {
        private final @Nullable Result<T> @NonNull [] array;

        private int addIndex = 0;
        private final @NonNull Lock lock = new ReentrantLock();
        private final @NonNull Condition condition = lock.newCondition();

        private BlockingArrayList(final int size) {
            array = new Result[size];
        }

        private void add(final @NonNull Result<T> result) {
            assert result != null;

            lock.lock();
            try {
                array[addIndex++] = result;
                condition.signalAll();
            } finally {
                lock.unlock();
            }
        }

        private @NonNull Result<T> get(final int index) {
            lock.lock();
            try {
                final var result = array[index];
                if (result != null) {
                    return result;
                }
                condition.await();
                //noinspection DataFlowIssue
                return array[index];
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            } finally {
                lock.unlock();
            }
        }
    }
}
