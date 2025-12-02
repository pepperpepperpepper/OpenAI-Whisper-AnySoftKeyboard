package wtf.uhoh.newsoftkeyboard.engine;

import androidx.annotation.NonNull;
import java.util.Collections;
import java.util.List;

/** A simple value object for prediction results. */
public final class PredictionResult {
  private final List<String> candidates;

  public PredictionResult(@NonNull List<String> candidates) {
    this.candidates = candidates;
  }

  @NonNull
  public List<String> getCandidates() {
    return Collections.unmodifiableList(candidates);
  }
}

