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
import jayo.stages.Promesse;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class CompletablePromesseTest {

    private static final String VALUE = "value";
    private static final RuntimeException EXCEPTION = new RuntimeException("test");

    @SuppressWarnings("unchecked")
    private final BiConsumer<? super String, ? super Throwable> action = mock(BiConsumer.class);
    private final Promesse.Completable<String> promesse;

    public CompletablePromesseTest() {
        promesse = Promesse.builder(Runnable::run).buildCompletable();
        promesse.whenComplete(action);
    }

    @Test
    public void shouldCompleteUsingMethodReference() {
        CompletableFuture<String> future = CompletableFuture.completedFuture(VALUE);
        future.thenAccept(promesse::complete);

        verify(action).accept(VALUE, null);
    }

    @Test
    public void shouldCompleteExceptionallyUsingMethodReference() {
        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(EXCEPTION);
        future.exceptionally(ex -> {
            promesse.completeExceptionally(ex);
            return null;
        });
        verify(action).accept(null, EXCEPTION);
    }
}