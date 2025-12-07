package wtf.uhoh.newsoftkeyboard.engine;

import androidx.annotation.NonNull;
import java.util.Collections;
import java.util.List;

public final class PredictionResult {
  @NonNull private final List<String> candidates;

  public PredictionResult(@NonNull List<String> candidates) {
    this.candidates = Collections.unmodifiableList(candidates);
  }

  @NonNull
  public List<String> getCandidates() {
    return candidates;
  }
}

