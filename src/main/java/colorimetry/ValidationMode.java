package colorimetry;

/**
 * Controls how the library handles invalid or incompatible input.
 *
 * Set globally via {@link ColorValue#setValidationMode(ValidationMode)}.
 *
 * NONE    - no validation, values pass through unchanged (default).
 * RESOLVE - silently resolves issues (clamps out-of-range bounded
 *           channels to [min, max], converts mismatched color spaces, etc.).
 * ERROR   - throws IllegalArgumentException on invalid input.
 *
 * Unbounded channels are never affected regardless of mode.
 */
public enum ValidationMode {
    NONE,
    RESOLVE,
    ERROR
}