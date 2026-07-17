package colorimetry_test.utils;

/**
 * Action that can throw checked exceptions, used by {@link BatchRunner}.
 *
 * @param <T> task type
 */
@FunctionalInterface
public interface TaskAction<T> {
    /**
     * Executes the action for one task.
     *
     * @param task the work item
     * @throws Exception if execution fails
     */
    void execute(T task) throws Exception;
}