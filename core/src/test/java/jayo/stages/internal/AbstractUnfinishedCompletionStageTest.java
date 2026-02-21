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

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.mockito.Mockito.*;

public abstract class AbstractUnfinishedCompletionStageTest extends AbstractCompletionStageTest {

    protected boolean finished() {
        return false;
    }

    @Test
    public void acceptEitherTakesOtherValueIfTheFirstIsNotReady() {
        CompletionStage<String> completionStage = createCompletionStage(VALUE);
        CompletionStage<String> completionStage2 = createCompletionStage(VALUE2);
        finish(completionStage2);

        Consumer<String> consumer = mockConsumer();
        completionStage.acceptEither(completionStage2, consumer);

        verify(consumer, times(1)).accept(VALUE2);
        finish(completionStage);

        verify(consumer, times(1)).accept(any(String.class));
    }

    @Test
    public void acceptEitherPropagatesExceptionFromSecondCompletable() {
        CompletionStage<String> completionStage = createCompletionStage(VALUE);
        CompletionStage<String> completionStage2 = createCompletionStage(EXCEPTION);
        finish(completionStage2);

        Consumer<String> consumer = mockConsumer();

        Function<Throwable, Void> errorHandler = mockFunction();
        completionStage.acceptEither(completionStage2, consumer).exceptionally(errorHandler);

        verify(errorHandler, times(1)).apply(any(CompletionException.class));
        finish(completionStage);

        verifyNoInteractions(consumer);
    }

    @Test
    public void acceptEitherPropagatesExceptionFromFirstCompletable() {
        CompletionStage<String> completionStage = createCompletionStage(EXCEPTION);
        CompletionStage<String> completionStage2 = createCompletionStage(VALUE2);
        finish(completionStage);

        Consumer<String> consumer = mockConsumer();

        Function<Throwable, Void> errorHandler = mockFunction();
        completionStage.acceptEither(completionStage2, consumer).exceptionally(errorHandler);

        verify(errorHandler, times(1)).apply(any(CompletionException.class));
        finish(completionStage2);

        verifyNoInteractions(consumer);
    }

    @Test
    public void acceptEitherPropagatesExceptionThrownByConsumerWhenProcessingFirstStage() {
        CompletionStage<String> completionStage = createCompletionStage(VALUE);
        CompletionStage<String> completionStage2 = createCompletionStage(VALUE2);
        finish(completionStage);

        Consumer<String> consumer = s -> {
            throw EXCEPTION;
        };


        Function<Throwable, Void> errorHandler = mockFunction();
        completionStage.acceptEither(completionStage2, consumer).exceptionally(errorHandler);

        verify(errorHandler, times(1)).apply(any(CompletionException.class));
        finish(completionStage2);
    }

    @Test
    public void acceptEitherPropagatesExceptionThrownByConsumerWhenProcessingSecondStage() {
        CompletionStage<String> completionStage = createCompletionStage(VALUE);
        CompletionStage<String> completionStage2 = createCompletionStage(VALUE2);
        finish(completionStage2);

        Consumer<String> consumer = s -> {
            throw EXCEPTION;
        };


        Function<Throwable, Void> errorHandler = mockFunction();
        completionStage.acceptEither(completionStage2, consumer).exceptionally(errorHandler);

        verify(errorHandler, times(1)).apply(any(CompletionException.class));
        finish(completionStage);
    }

    @SuppressWarnings("unchecked")
    private Consumer<String> mockConsumer() {
        return mock(Consumer.class);
    }

    @SuppressWarnings("unchecked")
    private Function<Throwable, Void> mockFunction() {
        return mock(Function.class);
    }
}
