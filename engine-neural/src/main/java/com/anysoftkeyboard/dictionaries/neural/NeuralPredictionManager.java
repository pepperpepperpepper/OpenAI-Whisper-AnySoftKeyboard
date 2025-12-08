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
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Handles ONNX Runtime backed next-word predictions. */
public final class NeuralPredictionManager {

  private static final String TAG = "NeuralPredictionManager";
  private static final boolean ENABLE_TEST_LOGS = true;
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
    final java.util.ArrayList<OnnxTensor> owned = new java.util.ArrayList<>();
    try {
      if (!isActive() && !activate()) {
        return new java.util.ArrayList<>();
      }
      if (!isActive() || mTokenizer == null || mSession == null || mEnvironment == null) {
        return new java.util.ArrayList<>();
      }

      final String contextText = " " + String.join(" ", contextTokens);
      if (contextText.trim().isEmpty()) {
        return new java.util.ArrayList<>();
      }

      int[] encoded = mTokenizer.encode(contextText);
      if (encoded.length == 0) {
        return new java.util.ArrayList<>();
      }
      if (ENABLE_TEST_LOGS) {
        Logger.d(TAG, "predictNextWords ctx='" + contextText + "' tokens=" + Arrays.toString(encoded));
      }
      if (encoded.length > MAX_CONTEXT_TOKENS) {
        final int[] trimmed = new int[MAX_CONTEXT_TOKENS];
        System.arraycopy(
            encoded, encoded.length - MAX_CONTEXT_TOKENS, trimmed, 0, MAX_CONTEXT_TOKENS);
        encoded = trimmed;
      }

      final int pastLength = 0; // no cache yet
      try (OnnxTensor inputTensor = createInputTensor(encoded);
          OnnxTensor attentionMask = maybeCreateAttentionMask(encoded.length, pastLength);
          OnnxTensor positionIds = maybeCreatePositionIds(encoded.length, pastLength)) {
        final java.util.HashMap<String, OnnxTensor> inputs = new java.util.HashMap<>();
        inputs.put("input_ids", inputTensor);
        if (attentionMask != null) {
          inputs.put("attention_mask", attentionMask);
        }
        if (positionIds != null) {
          inputs.put("position_ids", positionIds);
        }
        inputs.putAll(createPastKeyValueInputs(encoded.length, pastLength, owned));

        final Result result = mSession.run(inputs);
        try {
          final Object value = result.get(0).getValue();
          final float[] lastLogits = extractLogits(value);
          if (lastLogits == null) {
            return new java.util.ArrayList<>();
          }
          if (ENABLE_TEST_LOGS && mTokenizer != null) {
            Logger.d(TAG, "top5 logits " + formatTopLogits(lastLogits, 5, mTokenizer));
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
      // Close any past_key_values tensors created outside the try-with-resources scope.
      for (OnnxTensor tensor : owned) {
        if (tensor != null) {
          tensor.close();
        }
      }
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
        if (ENABLE_TEST_LOGS) {
          Logger.d(
              TAG,
              "past_key_values input "
                  + name
                  + " shape="
                  + java.util.Arrays.toString(shape)
                  + " type="
                  + tensorInfo.type);
        }
      }
    }
  }

  @Nullable
  private OnnxTensor maybeCreateAttentionMask(int tokenCount, int pastLength) throws OrtException {
    if (mEnvironment == null
        || mSessionInputNames == null
        || !mSessionInputNames.contains("attention_mask")) {
      return null;
    }
    final int length = pastLength + tokenCount;
    final long[] shape = new long[] {1L, length};
    final long[] data = new long[length];
    java.util.Arrays.fill(data, 1L);
    return OnnxTensor.createTensor(mEnvironment, LongBuffer.wrap(data), shape);
  }

  @Nullable
  private OnnxTensor maybeCreatePositionIds(int tokenCount, int pastLength) throws OrtException {
    if (mEnvironment == null
        || mSessionInputNames == null
        || !mSessionInputNames.contains("position_ids")) {
      return null;
    }
    final long[] shape = new long[] {1L, tokenCount};
    final long[] data = new long[tokenCount];
    for (int i = 0; i < tokenCount; i++) {
      data[i] = pastLength + i;
    }
    return OnnxTensor.createTensor(mEnvironment, LongBuffer.wrap(data), shape);
  }

  private OnnxTensor createInputTensor(int[] tokenIds) throws OrtException {
    final long[] shape = new long[] {1L, tokenIds.length};
    final long[] data = new long[tokenIds.length];
    for (int i = 0; i < tokenIds.length; i++) data[i] = tokenIds[i];
    final LongBuffer buf = LongBuffer.wrap(data);
    return OnnxTensor.createTensor(mEnvironment, buf, shape);
  }

  private java.util.Map<String, OnnxTensor> createPastKeyValueInputs(
      int tokenCount, int pastLength, java.util.List<OnnxTensor> owned) throws OrtException {
    final java.util.HashMap<String, OnnxTensor> out = new java.util.HashMap<>();
    if (mEnvironment == null || mPastKeyValueInputNames.isEmpty()) {
      return out;
    }
    for (String name : mPastKeyValueInputNames) {
      final long[] recordedShape = mPastKeyValueInputShapes.get(name);
      final ai.onnxruntime.OnnxJavaType type = mPastKeyValueInputTypes.get(name);
      if (recordedShape == null || type == null) continue;
      final long[] shape = recordedShape.clone();
      if (shape.length >= 3) {
        final int seqIndex = shape.length - 2; // for [batch, heads, seq, dim] this is seq
        shape[seqIndex] = pastLength;
      }
      final long size = elementCount(shape);
      if (size < 0 || size > Integer.MAX_VALUE) continue;
      switch (type) {
        case FLOAT:
          final float[] fData = new float[(int) size];
          final OnnxTensor fTensor =
              OnnxTensor.createTensor(mEnvironment, FloatBuffer.wrap(fData), shape);
          owned.add(fTensor);
          out.put(name, fTensor);
          break;
        case INT64:
          final long[] lData = new long[(int) size];
          final OnnxTensor lTensor = OnnxTensor.createTensor(mEnvironment, LongBuffer.wrap(lData), shape);
          owned.add(lTensor);
          out.put(name, lTensor);
          break;
        default:
          // unsupported type; skip
          break;
      }
    }
    return out;
  }

  private long elementCount(long[] shape) {
    long n = 1L;
    for (long dim : shape) {
      n *= dim;
    }
    return n;
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
    return selectTopTokens(lastLogits, k, mTokenizer);
  }

  private static String formatTopLogits(
      float[] lastLogits, int k, @NonNull Gpt2Tokenizer tokenizer) {
    if (lastLogits == null || lastLogits.length == 0 || k <= 0) {
      return "[]";
    }
    final java.util.PriorityQueue<int[]> heap =
        new java.util.PriorityQueue<>(java.util.Comparator.comparingDouble(a -> Float.intBitsToFloat(a[1])));
    for (int i = 0; i < lastLogits.length; i++) {
      final float score = lastLogits[i];
      final int packed = Float.floatToRawIntBits(score);
      if (heap.size() < k) {
        heap.add(new int[] {i, packed});
      } else if (score > Float.intBitsToFloat(heap.peek()[1])) {
        heap.poll();
        heap.add(new int[] {i, packed});
      }
    }
    final java.util.ArrayList<String> out = new java.util.ArrayList<>(heap.size());
    while (!heap.isEmpty()) {
      final int[] pair = heap.poll();
      out.add(
          pair[0]
              + ":"
              + tokenizer.decodeId(pair[0]).replace("\n", "\\n").replace("\r", "")
              + "="
              + Float.intBitsToFloat(pair[1]));
    }
    java.util.Collections.reverse(out);
    return out.toString();
  }

  /** Visible for testing: selects the top-k token strings by logit value. */
  static java.util.List<String> selectTopTokens(
      float[] lastLogits, int k, @Nullable Gpt2Tokenizer tokenizer) {
    if (k <= 0 || lastLogits == null || lastLogits.length == 0) {
      return java.util.Collections.emptyList();
    }
    final int target = Math.min(lastLogits.length, Math.max(k * 6, k + 8));
    final java.util.PriorityQueue<int[]> heap =
        new java.util.PriorityQueue<>(java.util.Comparator.comparingDouble(a -> Float.intBitsToFloat(a[1])));
    for (int i = 0; i < lastLogits.length; i++) {
      final float score = lastLogits[i];
      final int packed = Float.floatToRawIntBits(score);
      if (heap.size() < target) {
        heap.add(new int[] {i, packed});
      } else if (score > Float.intBitsToFloat(heap.peek()[1])) {
        heap.poll();
        heap.add(new int[] {i, packed});
      }
    }
    final java.util.ArrayList<int[]> sorted = new java.util.ArrayList<>(heap.size());
    while (!heap.isEmpty()) {
      final int[] pair = heap.poll();
      sorted.add(pair);
    }
    java.util.Collections.reverse(sorted);

    final java.util.ArrayList<String> out = new java.util.ArrayList<>(k);
    for (int[] pair : sorted) {
      final int tokenId = pair[0];
      final String decoded = tokenizer != null ? tokenizer.decodeId(tokenId) : String.valueOf(tokenId);
      if (tokenizer != null && isPunctuationOnly(decoded)) {
        continue;
      }
      out.add(decoded);
      if (out.size() == k) break;
    }
    if (out.isEmpty()) {
      for (int[] pair : sorted) {
        out.add(tokenizer != null ? tokenizer.decodeId(pair[0]) : String.valueOf(pair[0]));
        if (out.size() == k) break;
      }
    }
    return out;
  }

  private long[] materializeShape(long[] raw) {
    if (raw == null) return new long[] {};
    final long[] shape = raw.clone();
    for (int i = 0; i < shape.length; i++) if (shape[i] < 0) shape[i] = 1L;
    return shape;
  }

  private static boolean isPunctuationOnly(@NonNull String token) {
    if (token.isEmpty()) return true;
    for (int i = 0; i < token.length(); i++) {
      final char c = token.charAt(i);
      if (Character.isLetterOrDigit(c)) {
        return false;
      }
    }
    return true;
  }
}
