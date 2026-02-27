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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * An immutable stack of consumer callbacks implemented as a singly linked list.
 * <p>
 * Build up a stack by starting with {@linkplain EmptyCallbacks EmptyCallbacks} and repeatedly calling
 * {@link #plus(Consumer, Consumer, Executor)}. Each such call returns a new instance. The termination is
 * triggered by a single call of either {@link #complete(Object)} or {@link #completeExceptionally(Throwable)}.
 * <p>
 * This stack is optimized for safe concurrent access over a very small number of elements.
 * <p>
 * This stack is expected to hold fewer than 10 elements. Each operation is <i>O(N)</i>, and so building an
 * instance with <i>N</i> elements is <i>O(N**2)</i>.
 */
sealed abstract class Callbacks<T> {
    @Nullable Callbacks<T> plus(final @NonNull Consumer<? super T> successCallback,
                                final @NonNull Consumer<@NonNull Throwable> failureCallback,
                                final @NonNull Executor executor) {
        assert successCallback != null;
        assert failureCallback != null;
        assert executor != null;

        return new LinkedCallbacks<>(successCallback, failureCallback, executor, this);
    }

    @NonNull Success<T> complete(final T result) {
        throw new IllegalStateException("The complete method should not be called multiple times");
    }

    @NonNull Failure<T> completeExceptionally(final Throwable ex) {
        throw new IllegalStateException("The completeExceptionally method should not be called multiple times");
    }

    void callSuccessCallbacks(final T result) {
    }

    void callFailureCallbacks(final @NonNull Throwable ex) {
    }

    boolean isCompleted() {
        return false;
    }

    /**
     * An empty callback stack. This is always the tail of a callback list until completion.
     */
    static final class EmptyCallbacks<T> extends Callbacks<T> {
        private static final @NonNull EmptyCallbacks<Object> instance = new EmptyCallbacks<>();

        @SuppressWarnings("unchecked")
        static <T> Callbacks<T> instance() {
            return (Callbacks<T>) instance;
        }

        private EmptyCallbacks() {
        }

        @Override
        @NonNull Success<T> complete(final T result) {
            return new Success<>(result);
        }

        @Override
        @NonNull Failure<T> completeExceptionally(final Throwable ex) {
            return new Failure<>(ex);
        }
    }

    /**
     * A list of real callbacks. The callbacks in this stack are in the opposite order from how they were added, so we
     * submit them to the executor in reverse order. The {@code CompletionStage} javadoc allows implementations to
     * execute tasks in any order.
     */
    private static final class LinkedCallbacks<T> extends Callbacks<T> {
        private final @NonNull Consumer<? super T> successCallback;
        private final @NonNull Consumer<@NonNull Throwable> failureCallback;
        private final @NonNull Executor executor;
        private final @NonNull Callbacks<T> next;

        private LinkedCallbacks(final @NonNull Consumer<? super T> successCallback,
                                final @NonNull Consumer<@NonNull Throwable> failureCallback,
                                final @NonNull Executor executor,
                                final @NonNull Callbacks<T> next) {
            assert successCallback != null;
            assert failureCallback != null;
            assert executor != null;
            assert next != null;

            this.successCallback = successCallback;
            this.failureCallback = failureCallback;
            this.executor = executor;
            this.next = next;
        }

        @Override
        void callSuccessCallbacks(final T result) {
            Callbacks<T> callback = this;
            while (callback instanceof LinkedCallbacks<T> linkedCallback) {
                executor.execute(() -> linkedCallback.successCallback.accept(result));
                callback = linkedCallback.next;
            }
        }

        @Override
        void callFailureCallbacks(final @NonNull Throwable ex) {
            assert ex != null;

            Callbacks<T> callback = this;
            while (callback instanceof LinkedCallbacks<T> linkedCallback) {
                executor.execute(() -> linkedCallback.failureCallback.accept(ex));
                callback = linkedCallback.next;
            }
        }

        @Override
        @NonNull Success<T> complete(final T result) {
            return new Success<>(result);
        }

        @Override
        @NonNull Failure<T> completeExceptionally(final @NonNull Throwable ex) {
            assert ex != null;
            return new Failure<>(ex);
        }
    }

    /**
     * When the stage completed successfully with a value, this element becomes the only {@link #isCompleted completed}
     * element of this callbacks stack.
     */
    private static final class Success<T> extends Callbacks<T> {
        private final T result;

        Success(final T result) {
            this.result = result;
        }

        @Override
        @Nullable Callbacks<T> plus(final @NonNull Consumer<? super T> successCallback,
                                    final @NonNull Consumer<@NonNull Throwable> failureCallback,
                                    final @NonNull Executor executor) {
            assert successCallback != null;
            assert failureCallback != null;
            assert executor != null;

            executor.execute(() -> successCallback.accept(result));
            return null;
        }

        boolean isCompleted() {
            return true;
        }
    }

    /**
     * When the stage failed with an exception, this element becomes the only {@link #isCompleted completed} element of
     * this callbacks stack.
     */
    private static final class Failure<T> extends Callbacks<T> {
        private final @NonNull Throwable ex;

        Failure(final @NonNull Throwable ex) {
            assert ex != null;
            this.ex = ex;
        }

        @Override
        @Nullable Callbacks<T> plus(final @NonNull Consumer<? super T> successCallback,
                                    final @NonNull Consumer<@NonNull Throwable> failureCallback,
                                    final @NonNull Executor executor) {
            assert successCallback != null;
            assert failureCallback != null;
            assert executor != null;

            executor.execute(() -> failureCallback.accept(ex));
            return null;
        }

        boolean isCompleted() {
            return true;
        }
    }
}
