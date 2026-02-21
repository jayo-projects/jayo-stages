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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Tests CompletableFuture, to be sure we honor the spec correctly. The same tests are executed on Promesse and
 * CompletableFuture.
 */
public class FinishedCompletableFutureTest extends AbstractCompletionStageTest {

    @Override
    protected CompletionStage<String> createCompletionStage(String value) {
        CompletableFuture<String> completableFuture = new CompletableFuture<>();
        completableFuture.complete(value);
        return completableFuture;
    }

    @Override
    protected CompletionStage<String> createCompletionStage(Throwable e) {
        CompletableFuture<String> completableFuture = new CompletableFuture<>();
        completableFuture.completeExceptionally(e);
        return completableFuture;
    }

    @Override
    protected void finish(CompletionStage<String> completionStage) {

    }
}