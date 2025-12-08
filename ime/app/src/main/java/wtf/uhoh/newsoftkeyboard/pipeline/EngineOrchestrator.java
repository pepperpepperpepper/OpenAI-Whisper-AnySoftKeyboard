package wtf.uhoh.newsoftkeyboard.pipeline;

import com.anysoftkeyboard.base.utils.Logger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import wtf.uhoh.newsoftkeyboard.engine.PredictionEngine;
import wtf.uhoh.newsoftkeyboard.engine.PredictionResult;

/**
 * Shared helper that runs a prediction engine and merges normalized candidates into the holder.
 *
 * This keeps SuggestionsProvider slimmer while preserving existing behavior.
 */
public final class EngineOrchestrator {

  private EngineOrchestrator() {}

  public static MergeOutcome predictAndMerge(
      PredictionEngine engine,
      Deque<String> contextTokens,
      int maxNextWordSuggestionsCount,
      Collection<CharSequence> suggestionsHolder,
      int limit,
      boolean enableTestLogging,
      String logTag) {
    if (limit <= 0 || contextTokens.isEmpty()) {
      return MergeOutcome.empty();
    }

    final String[] contextArray = contextTokens.toArray(new String[0]);
    final PredictionResult result =
        engine.predict(contextArray, Math.min(limit, maxNextWordSuggestionsCount));

    final List<String> raw = result.getCandidates();
    if (enableTestLogging) {
      Logger.d(
          logTag,
          "Engine "
              + engine.getType()
              + " raw candidates="
              + raw
              + " ctx="
              + Arrays.toString(contextArray));
    }

    final List<String> predictions = CandidateNormalizer.normalize(raw);
    final boolean hadRaw = raw != null && !raw.isEmpty();
    if (predictions.isEmpty()) {
      return new MergeOutcome(0, hadRaw, false);
    }

    final int added = CandidateMerger.mergeUnique(suggestionsHolder, predictions, limit);
    return new MergeOutcome(added, hadRaw, true);
  }

  public static final class MergeOutcome {
    public final int added;
    public final boolean hadRaw;
    public final boolean hadNormalized;

    MergeOutcome(int added, boolean hadRaw, boolean hadNormalized) {
      this.added = added;
      this.hadRaw = hadRaw;
      this.hadNormalized = hadNormalized;
    }

    public static MergeOutcome empty() {
      return new MergeOutcome(0, false, false);
    }
  }
}
