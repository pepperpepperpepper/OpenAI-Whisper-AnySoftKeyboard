package com.anysoftkeyboard.dictionaries.neural;

import androidx.annotation.NonNull;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/** Minimal GPT-2 byte-pair tokenizer compatible with Hugging Face DistilGPT-2 exports. */
final class Gpt2Tokenizer {

  private final Map<String, Integer> vocab = new HashMap<>();
  private final Map<Integer, String> idToToken = new HashMap<>();
  private static final Map<Integer, Integer> BYTE_DECODER = buildByteDecoder();
  private static final Map<Integer, Integer> BYTE_ENCODER = buildByteEncoder();
  private static final Pattern GPT2_PATTERN =
      Pattern.compile(
          "'s|'t|'re|'ve|'m|'ll|'d| ?\\p{L}+| ?\\p{N}+| ?[^\\s\\p{L}\\p{N}]+|\\s+(?!\\S)|\\s+");
  private static final Pattern VOCAB_ENTRY_PATTERN =
      Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"\\s*:\\s*(\\d+)");
  private final Map<String, Integer> bpeRanks = new HashMap<>();
  private final Map<String, String[]> bpeCache = new HashMap<>();

  Gpt2Tokenizer(@NonNull File vocabJson, @NonNull File mergesTxt) throws IOException {
    loadVocab(vocabJson);
    loadMerges(mergesTxt);
  }

  int getVocabSize() {
    return vocab.size();
  }

  int[] encode(@NonNull String text) {
    if (text.isEmpty()) return new int[0];
    final byte[] utf8 = text.getBytes(StandardCharsets.UTF_8);
    final StringBuilder encoded = new StringBuilder(utf8.length);
    for (byte b : utf8) {
      int key = b & 0xFF;
      Integer cp = BYTE_ENCODER.get(key);
      if (cp != null) encoded.append((char) cp.intValue());
    }

    final List<Integer> ids = new ArrayList<>();
    final Matcher matcher = GPT2_PATTERN.matcher(encoded.toString());
    while (matcher.find()) {
      final String token = matcher.group();
      for (String bpeToken : bpe(token)) {
        final Integer id = vocab.get(bpeToken);
        ids.add(id != null ? id : vocab.getOrDefault("<unk>", 0));
      }
    }
    final int[] out = new int[ids.size()];
    for (int i = 0; i < ids.size(); i++) out[i] = ids.get(i);
    return out;
  }

  @NonNull
  private static String readAll(@NonNull File file) throws IOException {
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
      final StringBuilder builder = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) builder.append(line);
      return builder.toString();
    }
  }

  private void loadVocab(@NonNull File vocabJson) throws IOException {
    final String jsonText = readAll(vocabJson).trim();
    final Matcher matcher = VOCAB_ENTRY_PATTERN.matcher(jsonText);
    int count = 0;
    while (matcher.find()) {
      final String rawKey = matcher.group(1);
      final String key = unescapeJsonString(rawKey);
      final int val = Integer.parseInt(matcher.group(2));
      vocab.put(key, val);
      idToToken.putIfAbsent(val, key);
      count++;
    }
    if (count == 0) {
      throw new IOException("Failed to parse vocab.json entries.");
    }
  }

  private void loadMerges(@NonNull File mergesTxt) throws IOException {
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(new FileInputStream(mergesTxt), StandardCharsets.UTF_8))) {
      String line;
      int rank = 0;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (line.isEmpty() || line.startsWith("#")) continue;
        final String[] parts = line.split(" ");
        if (parts.length == 2) bpeRanks.put(parts[0] + " " + parts[1], rank++);
      }
    }
  }

  @NonNull
  private static String unescapeJsonString(@NonNull String s) throws IOException {
    final StringBuilder out = new StringBuilder(s.length());
    for (int i = 0; i < s.length(); i++) {
      final char c = s.charAt(i);
      if (c == '\\' && i + 1 < s.length()) {
        final char next = s.charAt(i + 1);
        // Handle double-escaped unicode (\"\\u0120\") or single-escaped (\" \u0120\")
        if (next == 'u' && i + 5 < s.length()) {
          final String hex = s.substring(i + 2, i + 6);
          out.append((char) Integer.parseInt(hex, 16));
          i += 5;
          continue;
        } else if (next == '\\'
            && i + 2 < s.length()
            && s.charAt(i + 2) == 'u'
            && i + 6 < s.length()) {
          final String hex = s.substring(i + 3, i + 7);
          out.append((char) Integer.parseInt(hex, 16));
          i += 6;
          continue;
        }
        // basic escapes
        i++;
        switch (next) {
          case '\\':
            out.append('\\');
            break;
          case '"':
            out.append('"');
            break;
          case 'n':
            out.append('\n');
            break;
          case 'r':
            out.append('\r');
            break;
          case 't':
            out.append('\t');
            break;
          default:
            out.append(next);
            break;
        }
      } else {
        out.append(c);
      }
    }
    return out.toString();
  }

  @NonNull
  String decodeId(int id) {
    final String token = idToToken.get(id);
    if (token == null) return String.valueOf(id);
    return decodeToken(token);
  }

  @NonNull
  private String decodeToken(@NonNull String token) {
    // GPT-2 byte-level BPE: map each UTF-8 pseudo-char back to the original byte.
    final ArrayDeque<Byte> bytes = new ArrayDeque<>(token.length());
    for (int i = 0; i < token.length(); i++) {
      final int codePoint = token.charAt(i);
      final Integer decoded = BYTE_DECODER.get(codePoint);
      if (decoded == null) continue;
      bytes.add((byte) decoded.intValue());
    }
    final byte[] out = new byte[bytes.size()];
    int idx = 0;
    while (!bytes.isEmpty()) {
      out[idx++] = bytes.removeFirst();
    }
    String text = new String(out, StandardCharsets.UTF_8);
    // GPT-2 uses a leading-space marker \u0120 (Ġ) and newline marker \u010a
    text = text.replace('\u0120', ' ');
    text = text.replace('\u010a', '\n');
    return text;
  }

  private static Map<Integer, Integer> buildByteDecoder() {
    // Mirrors GPT-2 byte encoder used by Hugging Face tokenizers.
    final List<Integer> bs = new ArrayList<>(256);
    final List<Integer> cs = new ArrayList<>(256);
    for (int i = '!'; i <= '~'; i++) bs.add(i);
    for (int i = 0xA1; i <= 0xAC; i++) bs.add(i);
    for (int i = 0xAE; i <= 0xFF; i++) bs.add(i);
    cs.addAll(bs);
    int n = 0;
    for (int b = 0; b < 256; b++) {
      if (!bs.contains(b)) {
        bs.add(b);
        cs.add(256 + n);
        n++;
      }
    }
    final Map<Integer, Integer> map = new HashMap<>(cs.size());
    for (int i = 0; i < bs.size(); i++) {
      map.put(cs.get(i), bs.get(i));
    }
    return map;
  }

  private static Map<Integer, Integer> buildByteEncoder() {
    final Map<Integer, Integer> enc = new HashMap<>();
    final Map<Integer, Integer> dec = buildByteDecoder();
    for (Map.Entry<Integer, Integer> e : dec.entrySet()) {
      enc.put(e.getValue(), e.getKey());
    }
    return enc;
  }

  private static Set<Pair> getPairs(List<String> symbols) {
    final Set<Pair> pairs = new HashSet<>();
    String prev = symbols.get(0);
    for (int i = 1; i < symbols.size(); i++) {
      final String s = symbols.get(i);
      pairs.add(new Pair(prev, s));
      prev = s;
    }
    return pairs;
  }

  private String[] bpe(@NonNull String token) {
    String[] cached = bpeCache.get(token);
    if (cached != null) return cached;

    List<String> word = new ArrayList<>(Arrays.asList(token.split("(?!^)")));
    Set<Pair> pairs = getPairs(word);
    if (pairs.isEmpty()) {
      final String[] single = new String[] {token};
      bpeCache.put(token, single);
      return single;
    }

    while (true) {
      Pair minPair = null;
      int bestRank = Integer.MAX_VALUE;
      for (Pair p : pairs) {
        Integer rank = bpeRanks.get(p.left + " " + p.right);
        if (rank != null && rank < bestRank) {
          bestRank = rank;
          minPair = p;
        }
      }
      if (minPair == null) break;

      final List<String> newWord = new ArrayList<>();
      int i = 0;
      while (i < word.size()) {
        final int j = indexOfPair(word, minPair, i);
        if (j == -1) {
          newWord.addAll(word.subList(i, word.size()));
          break;
        }
        newWord.addAll(word.subList(i, j));
        newWord.add(minPair.left + minPair.right);
        i = j + 2;
      }
      word = newWord;
      if (word.size() == 1) break;
      pairs = getPairs(word);
    }
    final String[] out = word.toArray(new String[0]);
    bpeCache.put(token, out);
    return out;
  }

  private int indexOfPair(List<String> word, Pair pair, int start) {
    for (int i = start; i < word.size() - 1; i++) {
      if (word.get(i).equals(pair.left) && word.get(i + 1).equals(pair.right)) {
        return i;
      }
    }
    return -1;
  }

  private static final class Pair {
    final String left;
    final String right;

    Pair(String l, String r) {
      left = l;
      right = r;
    }

    @Override
    public int hashCode() {
      return left.hashCode() * 31 + right.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Pair)) return false;
      Pair p = (Pair) o;
      return left.equals(p.left) && right.equals(p.right);
    }
  }
}
