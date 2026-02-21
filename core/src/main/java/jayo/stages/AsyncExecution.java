/*
 * Copyright (c) 2026-present, pull-vert and Jayo contributors.
 * Use of this source code is governed by the Apache 2.0 license.
 */

package jayo.stages;

/**
 * The stage's default asynchronous execution facility that defines which
 * {@linkplain java.util.concurrent.Executor Executor} will be used for all {@code *Async} methods without the explicit
 * {@linkplain java.util.concurrent.Executor Executor} argument.
 */
public enum AsyncExecution {
    /**
     * Propagate the latest explicit {@linkplain java.util.concurrent.Executor Executor} passed to {@code *Async} method
     */
    LAST_EXECUTOR,
    /**
     * Always use the initial executor
     */
    INITIAL_EXECUTOR
}
