package wtf.uhoh.newsoftkeyboard.engine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface PredictionEngine {

  @NonNull
  EngineType getType();

  boolean isReady();

  boolean activate();

  void deactivate();

  @Nullable
  String getLastError();

  @NonNull
  PredictionResult predict(@NonNull String[] contextTokens, int maxResults);
}

