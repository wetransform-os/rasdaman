/*
 * This file is part of rasdaman community.
 *
 * Rasdaman community is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Rasdaman community is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU  General Public License for more details.
 *
 * You should have received a copy of the GNU  General Public License
 * along with rasdaman community.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2003 - 2023 Peter Baumann / rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 */

package petascope.util;

import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

public class Task implements Callable<Object> {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(ThreadUtil.class);

    // Unique name for a task and a short description what it will do
    // e.g. thread-1 @ I'm sending request to external Petascope: https://somehost/rasdaman/ows
    private String threadNameWithDescription;
    // Max time to run a task in ms
    private long timeoutMs;
    private Runnable runnable;

    /**
     *
     * @param threadIndex: 0-based index
     * @param timeoutMs: max time to run a task in ms
     */
    public Task(int threadIndex, int totalThreadsToRun, String taskDescription, long timeoutMs, Runnable runnable) {
        this.threadNameWithDescription = "thread " + (threadIndex + 1) + "/" + totalThreadsToRun + ": " + taskDescription;
        this.timeoutMs = timeoutMs;
        this.runnable = runnable;
    }

    public String getThreadNameWithDescription() {
        return this.threadNameWithDescription;
    }
    public Runnable getRunnable() {
        return this.runnable;
    }

    public Callable<Object> call() throws InterruptedException, ExecutionException, TimeoutException {
        ExecutorService executorServiceTmp = Executors.newSingleThreadExecutor();
        Future<Object> future = executorServiceTmp.submit(Executors.callable(runnable));

        long startTime = System.currentTimeMillis();

        try {
            future.get(this.timeoutMs, TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
            if (ex instanceof TimeoutException) {
                // Important to interrupt the running thread after future.get() is timeout
                future.cancel(true);
                log.warn(threadNameWithDescription + " has timed out after " + this.timeoutMs + " ms");
            } else {
                log.error(threadNameWithDescription + " failed with error: " + ex.getMessage(), ex);
                throw ex;
            }
        } finally {
            executorServiceTmp.shutdown();
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        log.debug("Total time to finish task: " + threadNameWithDescription + " is: " + totalTime + " ms.");

        return null;
    }
}