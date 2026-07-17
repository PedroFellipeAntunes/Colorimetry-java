package colorimetry_test.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Runs tasks in parallel batches sized by available heap memory.
 * Each batch fills a fixed thread pool and completes before the next starts,
 * ensuring concurrent memory usage stays within heap limits.
 */
public final class BatchRunner {
    private BatchRunner() {}

    /**
     * Submits tasks in batches, waiting for each batch to complete before
     * starting the next. Batch size is computed from available heap.
     *
     * @param <T> task type
     * @param tasks list of work items
     * @param bytesPerTask estimated peak memory per concurrent task
     * @param action work to perform on each task
     * @throws Exception if waiting on a batch is interrupted
     */
    public static <T> void run(List<T> tasks, long bytesPerTask, TaskAction<T> action) throws Exception {
        int batchSize = computeBatchSize(bytesPerTask);

        System.out.println("Batch size: " + batchSize + " | Workers: " + Runtime.getRuntime().availableProcessors()
            + " | Heap: " + (Runtime.getRuntime().maxMemory() / (1024 * 1024)) + "MB\n");

        ExecutorService pool = Executors.newFixedThreadPool(batchSize);

        for (int i = 0; i < tasks.size(); i += batchSize) {
            int end = Math.min(i + batchSize, tasks.size());
            List<Future<?>> futures = new ArrayList<>();

            for (int j = i; j < end; j++) {
                T task = tasks.get(j);
                
                futures.add(pool.submit(() -> {
                    try {
                        action.execute(task);
                    } catch (Exception e) {
                        System.err.println("Task failed: " + e.getMessage());
                    }
                }));
            }

            // Wait for the batch to finish before starting the next
            for (Future<?> f : futures) {
                f.get();
            }
        }

        pool.shutdown();
    }

    /**
     * Computes how many tasks can run concurrently within 75% of max heap.
     *
     * @param bytesPerTask estimated peak memory per concurrent task
     * @return batch size (at least 1)
     */
    private static int computeBatchSize(long bytesPerTask) {
        long usableHeap = (long) (Runtime.getRuntime().maxMemory() * 0.75);
        int batch = (int) (usableHeap / Math.max(1, bytesPerTask));

        return Math.max(1, batch);
    }
}