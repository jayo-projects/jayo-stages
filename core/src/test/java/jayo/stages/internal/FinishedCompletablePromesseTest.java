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

import jayo.stages.Promesse;

import java.util.concurrent.CompletionStage;

public class FinishedCompletablePromesseTest extends AbstractCompletionStageTest {
    private final Promesse.Builder promesseBuilder = Promesse.builder(defaultExecutor);

    @Override
    protected CompletionStage<String> createCompletionStage(String value) {
        final var promesse = promesseBuilder.<String>buildCompletable();
        promesse.complete(value);
        return promesse;
    }

    @Override
    protected CompletionStage<String> createCompletionStage(Throwable e) {
        final var promesse = promesseBuilder.<String>buildCompletable();
        promesse.completeExceptionally(e);
        return promesse;
    }

    @Override
    protected void finish(CompletionStage<String> completionStage) {

    }
}