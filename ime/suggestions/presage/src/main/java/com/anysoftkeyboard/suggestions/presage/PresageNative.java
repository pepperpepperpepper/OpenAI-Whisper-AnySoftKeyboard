package com.anysoftkeyboard.suggestions.presage;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class PresageNative {

  private static final boolean LIBRARY_LOADED = loadLibrary();

  private PresageNative() {}

  private static boolean loadLibrary() {
    try {
      System.loadLibrary("anysoftkeyboard_presage");
      return true;
    } catch (UnsatisfiedLinkError error) {
      return false;
    }
  }

  public static long openModel(@NonNull String modelPath) {
    if (!LIBRARY_LOADED) return 0L;
    return nativeOpenModel(modelPath);
  }

  public static void closeModel(long handle) {
    if (!LIBRARY_LOADED || handle == 0L) return;
    nativeCloseModel(handle);
  }

  public static float scoreSequence(long handle, @NonNull String[] context, @NonNull String candidate) {
    if (!LIBRARY_LOADED || handle == 0L) return 0f;
    return nativeScoreSequence(handle, context, candidate);
  }

  @NonNull
  public static String[] predictNext(long handle, @Nullable String[] context, int maxResults) {
    if (!LIBRARY_LOADED || handle == 0L) return new String[0];
    final String[] safeContext = context == null ? new String[0] : context;
    return nativePredictNext(handle, safeContext, maxResults);
  }

  private static native long nativeOpenModel(@NonNull String modelPath);

  private static native void nativeCloseModel(long handle);

  private static native float nativeScoreSequence(long handle, @NonNull String[] context, @NonNull String candidate);

  private static native String[] nativePredictNext(long handle, @NonNull String[] context, int maxResults);
}
