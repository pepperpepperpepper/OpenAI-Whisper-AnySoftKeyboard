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

  public static int predictAndMerge(
      PredictionEngine engine,
      Deque<String> contextTokens,
      int maxNextWordSuggestionsCount,
      Collection<CharSequence> suggestionsHolder,
      int limit,
      boolean enableTestLogging,
      String logTag) {
    if (limit <= 0 || contextTokens.isEmpty()) {
      return 0;
    }

    final String[] contextArray = contextTokens.toArray(new String[0]);
    final PredictionResult result =
        engine.predict(contextArray, Math.min(limit, maxNextWordSuggestionsCount));

    if (enableTestLogging) {
      Logger.d(
          logTag,
          "Engine "
              + engine.getType()
              + " raw candidates="
              + result.getCandidates()
              + " ctx="
              + Arrays.toString(contextArray));
    }

    final List<String> predictions = CandidateNormalizer.normalize(result.getCandidates());
    if (predictions.isEmpty()) {
      return 0;
    }

    return CandidateMerger.mergeUnique(suggestionsHolder, predictions, limit);
  }
}
