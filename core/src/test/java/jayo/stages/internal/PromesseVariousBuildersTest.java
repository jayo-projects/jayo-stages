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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import jayo.stages.Promesse;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PromesseVariousBuildersTest {

    public static final RuntimeException TEST_EXCEPTION = new RuntimeException("Test exception");
    public static final String TEST_VALUE = "test";
    private Promesse.Builder builder;

    @Mock
    private Executor defaultExecutor;

    @Captor
    private ArgumentCaptor<Runnable> runnableCaptor;

    @Mock
    private Callable<String> callable;

    @Mock
    private Runnable runnable;

    @BeforeEach
    public void before() {
        builder = Promesse.builder(defaultExecutor);
    }

    @Test
    public void completedFutureTest() throws Exception {
        var stage = builder.<String>buildCompletable();
        stage.complete(TEST_VALUE);

        CompletableFuture<String> future = stage.toCompletableFuture();
        assertThat(future.isDone()).isTrue();
        assertThat(future.get()).isEqualTo(TEST_VALUE);
    }

    private <U> void doSupplyAsyncTest(CompletionStage<U> stage, Object supplier, String expectedResult) throws Exception {
        CompletableFuture<U> future = doSupplyAsyncTestWithoutResultCheck(stage, supplier);
        assertThat(future.get()).isEqualTo(expectedResult);
    }

    private <U> CompletableFuture<U> doSupplyAsyncTestWithoutResultCheck(CompletionStage<U> stage, Object supplier) {
        CompletableFuture<U> future = stage.toCompletableFuture();

        // preconditions:
        assertThat(future.isDone()).isFalse();
        verifyNoInteractions(supplier);

        verify(defaultExecutor).execute(runnableCaptor.capture());
        runnableCaptor.getValue().run();

        assertThat(future.isDone()).isTrue();
        return future;
    }

    @Test
    public void callAsyncTest() throws Exception {
        when(callable.call()).thenReturn(TEST_VALUE);

        CompletionStage<String> stage = builder.call(callable, false);

        doSupplyAsyncTest(stage, callable, TEST_VALUE);
    }

    @Test
    public void exceptionFromCallableShouldBePropagated() throws Exception {
        CompletionStage<String> stage = builder.call(callable, false);

        checkExceptionThrown(stage);
    }

    /**
     * Cross-check if the CompletableFuture has the same behavior.
     */
    @Test
    public void exceptionFromCallableShouldBePropagatedInCompletableFuture() throws Exception {
        checkExceptionThrown(CompletableFuture.supplyAsync(() -> {
            try {
                return callable.call();
            } catch (RuntimeException re) {
                throw re; // rethrow
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, defaultExecutor));
    }

    protected void checkExceptionThrown(CompletionStage<String> stage) throws Exception {
        when(callable.call()).thenThrow(TEST_EXCEPTION);

        AtomicReference<Throwable> exception = new AtomicReference<>();
        stage.exceptionally(e -> {
            exception.set(e);
            return null;
        });

        CompletableFuture<String> future = doSupplyAsyncTestWithoutResultCheck(stage, callable);

        assertThatThrownBy(future::get)
                .isInstanceOf(ExecutionException.class)
                .cause().isSameAs(TEST_EXCEPTION);

        assertThat(exception.get())
                .isInstanceOf(CompletionException.class)
                .cause().isSameAs(TEST_EXCEPTION);
    }


    @Test
    public void runAsyncTest() throws Exception {
        CompletionStage<Void> stage = builder.run(runnable, false);

        doSupplyAsyncTest(stage, runnable, null);
    }
}
