package wtf.uhoh.newsoftkeyboard.engine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.dictionaries.PresagePredictionManager;
import java.util.Arrays;
import java.util.Collections;

/** Adapter over PresagePredictionManager to conform to PredictionEngine. */
public final class PresageEngineAdapter implements PredictionEngine {

  private final PresagePredictionManager manager;

  public PresageEngineAdapter(@NonNull PresagePredictionManager manager) {
    this.manager = manager;
  }

  @NonNull
  @Override
  public EngineType getType() {
    return EngineType.NGRAM;
  }

  @Override
  public boolean isReady() {
    return manager.isActive();
  }

  @Override
  public boolean activate() {
    return manager.activate();
  }

  @Override
  public void deactivate() {
    manager.deactivate();
  }

  @Nullable
  @Override
  public String getLastError() {
    return manager.getLastActivationError();
  }

  @NonNull
  @Override
  public PredictionResult predict(@NonNull String[] contextTokens, int maxResults) {
    if (maxResults <= 0 || contextTokens.length == 0 || !manager.isActive()) {
      return new PredictionResult(Collections.emptyList());
    }
    final String[] preds = manager.predictNext(contextTokens, maxResults);
    return new PredictionResult(Arrays.asList(preds));
  }
}

