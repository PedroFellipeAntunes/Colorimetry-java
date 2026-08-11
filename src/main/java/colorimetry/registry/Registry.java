package colorimetry.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Generic registry for singleton entries with display names.
 * Provides registration, lookup, and listing.
 *
 * @param <T> entry type
 */
public final class Registry<T> {
    private final List<T> entries = new ArrayList<>();
    private final String typeName;
    private final Function<T, String> nameOf;

    /**
     * Creates a new registry.
     *
     * @param typeName display name for error messages (e.g. "ColorSpace")
     * @param nameOf function to extract the display name from an entry
     */
    public Registry(String typeName, Function<T, String> nameOf) {
        this.typeName = typeName;
        this.nameOf = nameOf;
    }

    /**
     * Adds an entry to the registry.
     *
     * @param entry entry to register
     * @throws IllegalArgumentException if entry is null or already registered
     */
    public void register(T entry) {
        if (entry == null) {
            throw new IllegalArgumentException(typeName + " cannot be null");
        }

        if (entries.contains(entry)) {
            throw new IllegalArgumentException(typeName + " already registered: " + nameOf.apply(entry));
        }

        entries.add(entry);
    }

    /**
     * Removes an entry from the registry.
     *
     * @param entry entry to remove
     * @throws IllegalArgumentException if entry is null or not registered
     */
    public void unregister(T entry) {
        if (entry == null) {
            throw new IllegalArgumentException(typeName + " cannot be null");
        }

        if (!entries.remove(entry)) {
            throw new IllegalArgumentException(typeName + " not registered: " + nameOf.apply(entry));
        }
    }

    /**
     * Checks whether an entry is registered.
     *
     * @param entry entry to look up
     * @return true if the entry is in the registry
     * @throws IllegalArgumentException if entry is null
     */
    public boolean contains(T entry) {
        if (entry == null) {
            throw new IllegalArgumentException(typeName + " cannot be null");
        }

        return entries.contains(entry);
    }

    /**
     * Returns the number of registered entries.
     *
     * @return registry size
     */
    public int size() {
        return entries.size();
    }

    /**
     * Returns all registered entries.
     *
     * @return unmodifiable list of entries
     */
    public List<T> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    /**
     * Removes all registered entries. Intended for testing.
     */
    public void clear() {
        entries.clear();
    }
}