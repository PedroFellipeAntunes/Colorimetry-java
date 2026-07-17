package colorimetry_test.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe error collector for concurrent test runs.
 * Stores the first MAX_LOGGED_ERRORS messages and tracks the total count.
 */
public final class ErrorLog {
    private static final int MAX_LOGGED_ERRORS = 10;

    private final List<String> entries = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger count = new AtomicInteger(0);

    /**
     * Records an error. Stores the message if under the log limit.
     *
     * @param context description of what was being converted
     * @param error the exception that occurred
     */
    public void log(String context, Exception error) {
        int current = count.incrementAndGet();
        
        if (current <= MAX_LOGGED_ERRORS) {
            entries.add("  " + context + " " + error.getClass().getSimpleName() + ": " + error.getMessage());
        }
    }

    /**
     * Returns the total number of logged errors.
     *
     * @return error count
     */
    public int count() {
        return count.get();
    }

    /**
     * Prints stored error messages to stdout.
     * Shows overflow count if errors exceed MAX_LOGGED_ERRORS.
     */
    public void print() {
        for (String entry : entries) {
            System.out.println(entry);
        }
        
        int total = count.get();
        if (total > MAX_LOGGED_ERRORS) {
            System.out.println("  ... and " + (total - MAX_LOGGED_ERRORS) + " more");
        }
    }
}