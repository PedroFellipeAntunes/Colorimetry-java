package colorimetry;

/**
 * Controls how {@link ColorValue} factories handle out-of-range values
 * on bounded channels ({@link ColorSpace#isChannelBounded(int)}).
 *
 * Set globally via {@link ColorValue#setValidationMode(ValidationMode)}.
 *
 * NONE  - no validation, values pass through unchanged (default).
 * CLAMP - silently clamps bounded channels to [min, max].
 * ERROR - throws IllegalArgumentException if a bounded channel
 *         is outside [min - eps, max + eps].
 *
 * Unbounded channels are never affected regardless of mode.
 */
public enum ValidationMode {
    NONE,
    CLAMP,
    ERROR
}