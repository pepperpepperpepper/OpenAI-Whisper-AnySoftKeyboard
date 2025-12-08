package wtf.uhoh.newsoftkeyboard.pipeline;

import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lightweight, engine-agnostic normalization for next-word candidates.
 *
 * Current scope: trim whitespace, drop empty/punctuation-only tokens,
 * and de-duplicate case-insensitively while preserving original casing.
 *
 * Note: Casing transformation (e.g., sentence-case) is intentionally
 * not applied here yet to avoid behavioral changes. Add that later
 * once UX rules are finalized.
 */
public final class CandidateNormalizer {

  /**
   * Normalizes a list of model predictions.
   *
   * - Trims whitespace
   * - Drops empty strings
   * - Drops punctuation-only tokens
   * - De-duplicates case-insensitively (keeps first occurrence)
   */
  @NonNull
  public static List<String> normalize(@NonNull List<String> raw) {
    return normalize(raw, java.util.Collections.emptySet());
  }

  /**
   * Normalizes a list of model predictions, skipping any token whose lowercase form appears in
   * {@code disallowLower}. If everything is filtered out, returns an empty list (callers may
   * choose to fall back to legacy sources instead of repeating the same token).
   */
  @NonNull
  public static List<String> normalize(
      @NonNull List<String> raw, @NonNull Collection<String> disallowLower) {
    final List<String> out = new ArrayList<>(raw.size());
    final Set<String> seenLower = new HashSet<>();
    for (String token : raw) {
      if (token == null) continue;
      final String trimmed = token.trim();
      if (trimmed.isEmpty()) continue;
      if (isPunctuationOnly(trimmed)) continue;
      final String lower = trimmed.toLowerCase();
      if (disallowLower.contains(lower)) continue;
      if (seenLower.add(lower)) {
        // preserve original casing for now
        out.add(trimmed);
      }
    }
    return out;
  }

  private static boolean isPunctuationOnly(@NonNull String s) {
    for (int i = 0; i < s.length(); i++) {
      final char c = s.charAt(i);
      if (Character.isLetterOrDigit(c)) return false;
    }
    return true;
  }
}
