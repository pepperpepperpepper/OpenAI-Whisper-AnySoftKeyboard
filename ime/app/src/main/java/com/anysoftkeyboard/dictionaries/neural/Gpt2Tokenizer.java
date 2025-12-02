package com.anysoftkeyboard.dictionaries.neural;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.anysoftkeyboard.base.utils.Logger;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Minimal GPT-2 byte-pair tokenizer compatible with Hugging Face DistilGPT-2 exports.
 *
 * <p>This implementation mirrors the original Python reference with byte fallback handling and
 * simple caching for BPE merges.
 */
final class Gpt2Tokenizer {

  private static final String TAG = "Gpt2Tokenizer";
  private static final Pattern GPT2_TOKEN_PATTERN =
      Pattern.compile(
          "'s|'t|'re|'ve|'m|'ll|'d| ?[\\p{L}\\p{M}]+| ?\\p{N}+| ?[^\\s\\p{L}\\p{M}\\p{N}]+|\\s+(?!\\S)|\\s+");

  private final Map<String, Integer> mEncoder = new LinkedHashMap<>();
  private final Map<Integer, String> mDecoder = new HashMap<>();
  private final Map<String, Integer> mBpeRanks = new HashMap<>();
  private final Map<String, String> mBpeCache = new ConcurrentHashMap<>();
  private final Map<Integer, String> mByteEncoder;
  private final Map<String, Integer> mByteDecoder = new HashMap<>();

  int getVocabSize() {
    return mEncoder.size();
  }

  @Nullable
  String getTokenString(int tokenId) {
    return mDecoder.get(tokenId);
  }

  boolean isSpecialToken(int tokenId) {
    final String token = mDecoder.get(tokenId);
    if (token == null || token.isEmpty()) {
      return true;
    }
    // Treat bracketed tokens (e.g., <s>, </s>, <unk>) and control tokens as special.
    return (token.startsWith("<") && token.endsWith(">"))
        || token.startsWith("Ġ<")
        || token.startsWith("▁<");
  }

  Gpt2Tokenizer(@NonNull File vocabFile, @NonNull File mergesFile) throws IOException {
    try {
      loadVocabulary(vocabFile);
    } catch (JSONException exception) {
      throw new IOException("Failed parsing GPT-2 vocabulary JSON", exception);
    }
    loadMerges(mergesFile);
    mByteEncoder = Collections.unmodifiableMap(buildByteEncoder());
    for (Map.Entry<Integer, String> entry : mByteEncoder.entrySet()) {
      mByteDecoder.put(entry.getValue(), entry.getKey());
    }
  }

  @NonNull
  int[] encode(@NonNull String text) {
    final List<Integer> tokens = new ArrayList<>();
    final Matcher matcher = GPT2_TOKEN_PATTERN.matcher(text);
    while (matcher.find()) {
      final String token = matcher.group();
      if (token == null || token.isEmpty()) {
        continue;
      }
      final StringBuilder transformed = new StringBuilder();
      for (byte b : token.getBytes(StandardCharsets.UTF_8)) {
        transformed.append(mByteEncoder.get(b & 0xFF));
      }
      final String bpeTokens = bpe(transformed.toString());
      for (String piece : bpeTokens.split(" ")) {
        final Integer id = mEncoder.get(piece);
        if (id != null) {
          tokens.add(id);
        }
      }
    }
    final int[] result = new int[tokens.size()];
    for (int i = 0; i < tokens.size(); i++) {
      result[i] = tokens.get(i);
    }
    return result;
  }

  @NonNull
  String decodeTokens(@NonNull int[] tokens) {
    final StringBuilder builder = new StringBuilder();
    for (int token : tokens) {
      builder.append(decodeTokenInternal(token));
    }
    return builder.toString();
  }

  @NonNull
  String decodeToken(int token) {
    return decodeTokenInternal(token).trim();
  }

  private void loadVocabulary(@NonNull File vocabFile) throws IOException, JSONException {
    final StringBuilder builder = new StringBuilder();
    try (FileInputStream fis = new FileInputStream(vocabFile);
        InputStreamReader reader = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
      final char[] buffer = new char[4096];
      int read;
      while ((read = reader.read(buffer)) != -1) {
        builder.append(buffer, 0, read);
      }
    }
    final JSONObject vocabJson = new JSONObject(builder.toString());
    final ArrayList<String> keys = new ArrayList<>();
    vocabJson.keys().forEachRemaining(keys::add);
    for (String key : keys) {
      final int id = vocabJson.getInt(key);
      mEncoder.put(key, id);
      mDecoder.put(id, key);
    }
  }

  private void loadMerges(@NonNull File mergesFile) throws IOException {
    try (BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(new FileInputStream(mergesFile), StandardCharsets.UTF_8))) {
      String line;
      int index = 0;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (line.isEmpty() || line.startsWith("#")) {
          continue;
        }
        final String[] parts = line.split("\\s+");
        if (parts.length != 2) {
          continue;
        }
        mBpeRanks.put(parts[0] + " " + parts[1], index++);
      }
    }
  }

  @NonNull
  private String bpe(@NonNull String token) {
    final String cached = mBpeCache.get(token);
    if (cached != null) {
      return cached;
    }

    List<String> word = new ArrayList<>();
    for (int i = 0; i < token.length(); ) {
      final int codePoint = token.codePointAt(i);
      word.add(new String(Character.toChars(codePoint)));
      i += Character.charCount(codePoint);
    }

    if (word.size() == 1) {
      final String result = token;
      mBpeCache.put(token, result);
      return result;
    }

    java.util.Set<String> pairs = getPairs(word);
    while (!pairs.isEmpty()) {
      String bestPair = null;
      int bestRank = Integer.MAX_VALUE;
      for (String pair : pairs) {
        final Integer rank = mBpeRanks.get(pair);
        if (rank != null && rank < bestRank) {
          bestRank = rank;
          bestPair = pair;
        }
      }

      if (bestPair == null) {
        break;
      }

      final String[] splitPair = bestPair.split(" ");
      final String first = splitPair[0];
      final String second = splitPair[1];

      final List<String> newWord = new ArrayList<>();
      int index = 0;
      while (index < word.size()) {
        if (index < word.size() - 1
            && word.get(index).equals(first)
            && word.get(index + 1).equals(second)) {
          newWord.add(first + second);
          index += 2;
        } else {
          newWord.add(word.get(index));
          index += 1;
        }
      }

      word = newWord;
      if (word.size() == 1) {
        break;
      }
      pairs = getPairs(word);
    }

    final String joined = String.join(" ", word);
    mBpeCache.put(token, joined);
    return joined;
  }

  @NonNull
  private static java.util.Set<String> getPairs(@NonNull List<String> word) {
    final java.util.LinkedHashSet<String> pairs = new java.util.LinkedHashSet<>();
    String prev = word.get(0);
    for (int i = 1; i < word.size(); i++) {
      final String current = word.get(i);
      pairs.add(prev + " " + current);
      prev = current;
    }
    return pairs;
  }

  @NonNull
  private static Map<Integer, String> buildByteEncoder() {
    final List<Integer> bs = new ArrayList<>();
    for (int i = 33; i <= 126; i++) {
      bs.add(i);
    }
    for (int i = 161; i <= 172; i++) {
      bs.add(i);
    }
    for (int i = 174; i <= 255; i++) {
      bs.add(i);
    }

    final List<Integer> cs = new ArrayList<>(bs);
    int n = 0;
    for (int b = 0; b < 256; b++) {
      if (!bs.contains(b)) {
        bs.add(b);
        cs.add(256 + n);
        n++;
      }
    }

    final Map<Integer, String> byteEncoder = new LinkedHashMap<>();
    for (int i = 0; i < bs.size(); i++) {
      byteEncoder.put(bs.get(i), new String(Character.toChars(cs.get(i))));
    }
    return byteEncoder;
  }

  @NonNull
  private String decodeTokenInternal(int token) {
    final String encoded = mDecoder.get(token);
    if (encoded == null) {
      return "";
    }
    final java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
    final java.io.Writer fallbackWriter =
        new java.io.OutputStreamWriter(output, StandardCharsets.UTF_8);
    for (int i = 0; i < encoded.length(); ) {
      final int codePoint = encoded.codePointAt(i);
      final String key = new String(Character.toChars(codePoint));
      final Integer byteValue = mByteDecoder.get(key);
      if (byteValue != null) {
        output.write(byteValue);
      } else {
        try {
          // For LLaMA-style tokens (e.g., ▁ prefix) we preserve the original glyph.
          fallbackWriter.write(key);
        } catch (IOException ignored) {
          Logger.w(TAG, "Failed writing fallback token chunk: " + key);
        }
      }
      i += Character.charCount(codePoint);
    }
    try {
      fallbackWriter.flush();
    } catch (IOException ignored) {
      // ignore
    }
    return new String(output.toByteArray(), StandardCharsets.UTF_8);
  }
}
