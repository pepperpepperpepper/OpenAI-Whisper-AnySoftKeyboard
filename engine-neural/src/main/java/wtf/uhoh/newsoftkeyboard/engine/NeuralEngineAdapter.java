package wtf.uhoh.newsoftkeyboard.engine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.dictionaries.neural.NeuralPredictionManager;
import java.util.List;

/** Adapter over NeuralPredictionManager to conform to PredictionEngine. */
public final class NeuralEngineAdapter implements PredictionEngine {

  private final NeuralPredictionManager manager;

  public NeuralEngineAdapter(@NonNull NeuralPredictionManager manager) {
    this.manager = manager;
  }

  @NonNull
  @Override
  public EngineType getType() {
    return EngineType.NEURAL;
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
    final List<String> preds = manager.predictNextWords(contextTokens, maxResults);
    return new PredictionResult(preds);
  }
}

