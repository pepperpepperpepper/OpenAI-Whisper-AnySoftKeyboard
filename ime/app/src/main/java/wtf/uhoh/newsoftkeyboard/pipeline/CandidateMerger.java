package wtf.uhoh.newsoftkeyboard.pipeline;

import androidx.annotation.NonNull;
import java.util.Collection;
import java.util.List;

/** Utility for merging model candidates into the suggestion holder with uniqueness. */
public final class CandidateMerger {

  /**
   * Adds up to {@code limit} items from {@code additions} into {@code holder},
   * skipping items already present (using {@code Collection#contains}).
   * Returns the number of items actually added.
   */
  public static int mergeUnique(
      @NonNull Collection<CharSequence> holder, @NonNull List<String> additions, int limit) {
    if (limit <= 0) return 0;
    int added = 0;
    for (String s : additions) {
      if (s == null || s.isEmpty()) continue;
      if (holder.contains(s)) continue;
      holder.add(s);
      added++;
      if (added == limit) break;
    }
    return added;
  }
}

