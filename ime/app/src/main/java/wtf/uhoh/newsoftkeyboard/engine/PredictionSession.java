package wtf.uhoh.newsoftkeyboard.engine;

import androidx.annotation.NonNull;

/** Lightweight context tracker for next-word predictions. */
public interface PredictionSession {

  /** Records a token (typically the last committed word). */
  void recordToken(@NonNull String token);

  /**
   * Returns the current context window as an array of tokens. Implementations may trim to a
   * configured max length.
   */
  @NonNull
  String[] getContextTokens();

  /** Clears the recorded context. */
  void reset();
}

