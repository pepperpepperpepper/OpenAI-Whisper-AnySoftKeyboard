package wtf.uhoh.newsoftkeyboard.engine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Minimal abstraction over prediction backends. */
public interface PredictionEngine {

  /** Engine family (NGRAM/NEURAL/HYBRID). */
  @NonNull
  EngineType getType();

  /** Returns true if the engine is initialized and ready for inference. */
  boolean isReady();

  /** Attempts to initialize the engine; returns true on success. */
  boolean activate();

  /** Releases resources. */
  void deactivate();

  /** Optional human‑readable last error message. */
  @Nullable
  String getLastError();

  /**
   * Computes next‑word candidates for the given context.
   * Implementations should be fast and avoid allocations when returning empty results.
   */
  @NonNull
  PredictionResult predict(@NonNull String[] contextTokens, int maxResults);
}

