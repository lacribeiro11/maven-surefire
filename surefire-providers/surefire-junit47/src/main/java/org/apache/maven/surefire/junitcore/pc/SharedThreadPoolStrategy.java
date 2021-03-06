package org.apache.maven.surefire.junitcore.pc;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Parallel strategy for shared thread pool in private package.
 *
 * @author Tibor Digana (tibor17)
 * @since 2.16
 *
 * @see AbstractThreadPoolStrategy
 */
final class SharedThreadPoolStrategy extends AbstractThreadPoolStrategy {
    SharedThreadPoolStrategy(ExecutorService threadPool) {
        super(threadPool, new ConcurrentLinkedQueue<Future<?>>());
    }

    @Override
    public boolean hasSharedThreadPool() {
        return true;
    }

    @Override
    public boolean finished() throws InterruptedException {
        boolean wasRunningAll = canSchedule();
        for (Future<?> futureResult : getFutureResults()) {
            try {
                futureResult.get();
            } catch (InterruptedException e) {
                // after called external ExecutorService#shutdownNow()
                // or ExecutorService#shutdown()
                wasRunningAll = false;
            } catch (ExecutionException e) {
                // test throws exception
            } catch (CancellationException e) {
                // cannot happen because not calling Future#cancel()
            }
        }
        disable();
        return wasRunningAll;
    }

    @Override
    protected final boolean stop() {
        return stop(false);
    }

    @Override
    protected final boolean stopNow() {
        return stop(true);
    }

    private boolean stop(boolean interrupt) {
        final boolean wasRunning = canSchedule();
        for (Future<?> futureResult : getFutureResults()) {
            futureResult.cancel(interrupt);
        }
        disable();
        return wasRunning;
    }
}