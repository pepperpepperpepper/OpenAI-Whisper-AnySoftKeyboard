package com.anysoftkeyboard.dictionaries.neural;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.base.utils.Logger;
import com.anysoftkeyboard.dictionaries.presage.PresageModelDefinition;
import com.anysoftkeyboard.dictionaries.presage.PresageModelStore;
import com.anysoftkeyboard.dictionaries.presage.PresageModelStore.ActiveModel;
import ai.onnxruntime.NodeInfo;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.OrtSession.Result;
import ai.onnxruntime.TensorInfo;
import ai.onnxruntime.ValueInfo;
import java.io.File;
import java.io.IOException;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Handles ONNX Runtime backed next-word predictions. */
public final class NeuralPredictionManager {

  private static final String TAG = "NeuralPredictionManager";
  private static final int MAX_CONTEXT_TOKENS = 64;
  private static final Pattern PAST_KEY_VALUE_PATTERN =
      Pattern.compile("past_key_values\\.(\\d+)\\.(key|value)");

  private final Context mContext;
  private final PresageModelStore mModelStore;
  private final ReentrantLock mSessionLock = new ReentrantLock();

  @Nullable private ActiveModel mActiveModel;
  @Nullable private OrtEnvironment mEnvironment;
  @Nullable private OrtSession.SessionOptions mSessionOptions;
  @Nullable private OrtSession mSession;
  @Nullable private Gpt2Tokenizer mTokenizer;
  @Nullable private String mLastActivationError;
  @Nullable private java.util.Set<String> mSessionInputNames;

  private final List<String> mPastKeyValueInputNames = new ArrayList<>();
  private final Map<String, long[]> mPastKeyValueInputShapes = new HashMap<>();
  private final Map<String, ai.onnxruntime.OnnxJavaType> mPastKeyValueInputTypes = new HashMap<>();
  private int mModelVocabSize = 0;

  public NeuralPredictionManager(@NonNull Context context) {
    this(context, new PresageModelStore(context));
  }

  NeuralPredictionManager(
      @NonNull Context context, @NonNull PresageModelStore presageModelStore) {
    mContext = context.getApplicationContext();
    mModelStore = presageModelStore;
  }

  public boolean activate() {
    mSessionLock.lock();
    try {
      if (isActive()) {
        return true;
      }

      mLastActivationError = null;
      final ActiveModel activeModel =
          mModelStore.ensureActiveModel(PresageModelDefinition.EngineType.NEURAL);
      if (activeModel == null) {
        mLastActivationError = "No neural language model installed.";
        return false;
      }

      try {
        final File onnxFile = activeModel.requireFile("onnx");
        final File vocabFile = activeModel.requireFile("tokenizer.vocab");
        final File mergesFile = activeModel.requireFile("tokenizer.merges");

        mTokenizer = new Gpt2Tokenizer(vocabFile, mergesFile);
        mModelVocabSize = mTokenizer.getVocabSize();
        mEnvironment = OrtEnvironment.getEnvironment();
        mSessionOptions = new OrtSession.SessionOptions();
        mSession = mEnvironment.createSession(onnxFile.getAbsolutePath(), mSessionOptions);
        mSessionInputNames = mSession.getInputNames();
        Logger.i(TAG, "Neural model input names: " + mSessionInputNames);
        initializeSessionMetadata();
        mActiveModel = activeModel;
        Logger.i(
            TAG, "Neural predictor activated with model " + activeModel.getDefinition().getId());
        return true;
      } catch (IOException exception) {
        mLastActivationError = "Failed loading tokenizer assets: " + exception.getMessage();
        Logger.e(TAG, mLastActivationError, exception);
        deactivate();
        return false;
      } catch (OrtException exception) {
        mLastActivationError = "Failed initializing ONNX runtime: " + exception.getMessage();
        Logger.e(TAG, mLastActivationError, exception);
        deactivate();
        return false;
      }
    } finally {
      mSessionLock.unlock();
    }
  }

  public void deactivate() {
    mSessionLock.lock();
    try {
      mActiveModel = null;
      if (mSession != null) {
        try {
          mSession.close();
        } catch (OrtException ignore) {
        }
      }
      mSession = null;
      if (mSessionOptions != null) {
        mSessionOptions.close();
      }
      mSessionOptions = null;
      mTokenizer = null;
      mEnvironment = null;
      mSessionInputNames = null;
      mPastKeyValueInputNames.clear();
      mPastKeyValueInputShapes.clear();
      mPastKeyValueInputTypes.clear();
      mModelVocabSize = 0;
    } finally {
      mSessionLock.unlock();
    }
  }

  public boolean isActive() {
    return mSession != null && mTokenizer != null;
  }

  @Nullable
  public String getLastActivationError() {
    return mLastActivationError;
  }

  @NonNull
  public java.util.List<String> predictNextWords(@NonNull String[] contextTokens, int maxResults) {
    if (maxResults <= 0) {
      return new java.util.ArrayList<>();
    }
    mSessionLock.lock();
    try {
      if (!isActive() && !activate()) {
        return new java.util.ArrayList<>();
      }
      if (!isActive() || mTokenizer == null || mSession == null || mEnvironment == null) {
        return new java.util.ArrayList<>();
      }

      final String contextText = String.join(" ", contextTokens);
      if (contextText.trim().isEmpty()) {
        return new java.util.ArrayList<>();
      }

      int[] encoded = mTokenizer.encode(contextText);
      if (encoded.length == 0) {
        return new java.util.ArrayList<>();
      }
      if (encoded.length > MAX_CONTEXT_TOKENS) {
        final int[] trimmed = new int[MAX_CONTEXT_TOKENS];
        System.arraycopy(
            encoded, encoded.length - MAX_CONTEXT_TOKENS, trimmed, 0, MAX_CONTEXT_TOKENS);
        encoded = trimmed;
      }

      try (OnnxTensor inputTensor = createInputTensor(encoded)) {
        final java.util.HashMap<String, OnnxTensor> inputs = new java.util.HashMap<>();
        inputs.put("input_ids", inputTensor);

        final Result result = mSession.run(inputs);
        try {
          final Object value = result.get(0).getValue();
          final float[] lastLogits = extractLogits(value);
          if (lastLogits == null) {
            return new java.util.ArrayList<>();
          }
          return extractTopTokens(lastLogits, maxResults);
        } finally {
          result.close();
        }
      }
    } catch (OrtException exception) {
      Logger.e(TAG, "ONNX runtime failure: " + exception.getMessage(), exception);
      mLastActivationError = exception.getMessage();
      deactivate();
      return new java.util.ArrayList<>();
    } finally {
      mSessionLock.unlock();
    }
  }

  private void initializeSessionMetadata() throws OrtException {
    mPastKeyValueInputNames.clear();
    mPastKeyValueInputShapes.clear();
    mPastKeyValueInputTypes.clear();
    if (mSession == null || mSessionInputNames == null) {
      return;
    }

    final Map<String, NodeInfo> inputInfo = mSession.getInputInfo();
    for (String name : mSessionInputNames) {
      final Matcher matcher = PAST_KEY_VALUE_PATTERN.matcher(name);
      if (!matcher.matches()) {
        continue;
      }
      mPastKeyValueInputNames.add(name);
      final NodeInfo nodeInfo = inputInfo.get(name);
      final ValueInfo valueInfo = nodeInfo != null ? nodeInfo.getInfo() : null;
      if (valueInfo instanceof TensorInfo) {
        final TensorInfo tensorInfo = (TensorInfo) valueInfo;
        final long[] rawShape = tensorInfo.getShape();
        final long[] shape = materializeShape(rawShape);
        mPastKeyValueInputShapes.put(name, shape);
        mPastKeyValueInputTypes.put(name, tensorInfo.type);
      }
    }
  }

  private boolean needsAttentionMask() { return false; }
  private boolean needsPositionIds() { return false; }

  private OnnxTensor createInputTensor(int[] tokenIds) throws OrtException {
    final long[] shape = new long[] {1L, tokenIds.length};
    final long[] data = new long[tokenIds.length];
    for (int i = 0; i < tokenIds.length; i++) data[i] = tokenIds[i];
    final LongBuffer buf = LongBuffer.wrap(data);
    return OnnxTensor.createTensor(mEnvironment, buf, shape);
  }

  private float[] extractLogits(Object value) {
    if (value instanceof float[][][]) {
      final float[][][] logits = (float[][][]) value;
      return logits[0][logits[0].length - 1];
    }
    if (value instanceof float[][]) {
      final float[][] logits = (float[][]) value;
      return logits[logits.length - 1];
    }
    return null;
  }

  private java.util.List<String> extractTopTokens(float[] lastLogits, int k) {
    final java.util.PriorityQueue<int[]> heap =
        new java.util.PriorityQueue<>(java.util.Comparator.comparingDouble(a -> a[1]));
    for (int i = 0; i < lastLogits.length; i++) {
      final int score = Float.floatToIntBits(lastLogits[i]);
      if (heap.size() < k) heap.add(new int[] {i, score});
      else if (score > heap.peek()[1]) { heap.poll(); heap.add(new int[] {i, score}); }
    }
    final java.util.ArrayList<String> out = new java.util.ArrayList<>(k);
    while (!heap.isEmpty()) {
      final int[] pair = heap.poll();
      out.add(String.valueOf(pair[0]));
    }
    java.util.Collections.reverse(out);
    return out;
  }

  private long[] materializeShape(long[] raw) {
    if (raw == null) return new long[] {};
    final long[] shape = raw.clone();
    for (int i = 0; i < shape.length; i++) if (shape[i] < 0) shape[i] = 1L;
    return shape;
  }
}
