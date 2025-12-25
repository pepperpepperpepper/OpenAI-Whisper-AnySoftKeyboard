package com.anysoftkeyboard.ui.settings;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.menny.android.anysoftkeyboard.NskApplicationBase;
import com.menny.android.anysoftkeyboard.R;

final class NextWordPreferenceSummaries {

  private NextWordPreferenceSummaries() {}

  @Nullable
  static NeuralFailureStatus readLastNeuralFailureStatus(@NonNull Context context) {
    final String storedValue =
        NskApplicationBase.prefs(context)
            .getString(
                R.string.settings_key_prediction_engine_last_neural_error,
                R.string.settings_default_prediction_engine_last_neural_error)
            .get();
    if (TextUtils.isEmpty(storedValue)) {
      return null;
    }
    long timestamp = 0L;
    String message = storedValue;
    final int delimiterIndex = storedValue.indexOf('|');
    if (delimiterIndex >= 0) {
      final String timestampPart = storedValue.substring(0, delimiterIndex);
      if (!TextUtils.isEmpty(timestampPart)) {
        try {
          timestamp = Long.parseLong(timestampPart);
        } catch (NumberFormatException ignored) {
          timestamp = 0L;
        }
      }
      message = storedValue.substring(Math.min(delimiterIndex + 1, storedValue.length()));
    }
    if (TextUtils.isEmpty(message)) {
      message = context.getString(R.string.prediction_engine_error_unknown);
    }
    return new NeuralFailureStatus(timestamp, message);
  }

  @NonNull
  static CharSequence buildPredictionSummary(
      @NonNull Context context,
      @NonNull String mode,
      boolean suggestionsEnabled,
      @Nullable NeuralFailureStatus failureStatus,
      @Nullable String ngramLabel,
      @Nullable String neuralLabel,
      @Nullable String hybridNgramLabel,
      @Nullable String hybridNeuralLabel) {
    if (!suggestionsEnabled) {
      return context.getString(R.string.prediction_engine_summary_requires_suggestions);
    }

    if (failureStatus != null) {
      return buildNeuralFailureSummary(context, failureStatus, mode);
    }

    switch (mode) {
      case "ngram":
        return ngramLabel != null
            ? context.getString(R.string.prediction_engine_mode_summary, ngramLabel)
            : context.getString(R.string.prediction_engine_summary_missing_model);
      case "hybrid":
        return hybridNgramLabel != null && hybridNeuralLabel != null
            ? context.getString(
                R.string.prediction_engine_summary_hybrid_dual, hybridNgramLabel, hybridNeuralLabel)
            : context.getString(R.string.prediction_engine_summary_missing_model);
      case "neural":
        return neuralLabel != null
            ? context.getString(R.string.prediction_engine_summary_neural, neuralLabel)
            : context.getString(R.string.prediction_engine_summary_missing_model);
      case "none":
      default:
        return context.getString(R.string.prediction_engine_summary_disabled);
    }
  }

  @NonNull
  static CharSequence buildManageModelsSummary(
      @NonNull Context context,
      @Nullable String ngramLabel,
      @Nullable String neuralLabel,
      @Nullable NeuralFailureStatus failureStatus) {
    final String baseSummary;
    if (TextUtils.isEmpty(ngramLabel) && TextUtils.isEmpty(neuralLabel)) {
      baseSummary = context.getString(R.string.presage_models_manage_summary_empty);
    } else {
      final String displayNgram =
          TextUtils.isEmpty(ngramLabel)
              ? context.getString(R.string.presage_models_manage_summary_unavailable)
              : ngramLabel;
      final String displayNeural =
          TextUtils.isEmpty(neuralLabel)
              ? context.getString(R.string.presage_models_manage_summary_unavailable)
              : neuralLabel;
      baseSummary =
          context.getString(
              R.string.presage_models_manage_summary_active_multi, displayNgram, displayNeural);
    }
    if (failureStatus == null) {
      return baseSummary;
    }
    final CharSequence reason = buildNeuralFailureReason(context, failureStatus);
    return context.getString(
        R.string.presage_models_manage_summary_with_error, baseSummary, reason);
  }

  private static CharSequence buildNeuralFailureSummary(
      @NonNull Context context, @NonNull NeuralFailureStatus status, @NonNull String currentMode) {
    final CharSequence reason = buildNeuralFailureReason(context, status);
    if ("neural".equals(currentMode) || "hybrid".equals(currentMode)) {
      return context.getString(R.string.prediction_engine_summary_neural_failure, reason);
    }
    return context.getString(
        R.string.prediction_engine_summary_neural_failure_fallback,
        reason,
        resolveModeLabel(context, currentMode));
  }

  private static CharSequence buildNeuralFailureReason(
      @NonNull Context context, @NonNull NeuralFailureStatus status) {
    if (status.timestamp > 0L) {
      final CharSequence relativeTime =
          DateUtils.getRelativeTimeSpanString(
              status.timestamp,
              System.currentTimeMillis(),
              DateUtils.MINUTE_IN_MILLIS,
              DateUtils.FORMAT_ABBREV_RELATIVE);
      return context.getString(
          R.string.prediction_engine_neural_failure_reason_relative, relativeTime, status.message);
    }
    return status.message;
  }

  private static CharSequence resolveModeLabel(@NonNull Context context, @NonNull String mode) {
    switch (mode) {
      case "ngram":
        return context.getString(R.string.prediction_engine_mode_ngram);
      case "neural":
        return context.getString(R.string.prediction_engine_mode_neural);
      case "hybrid":
        return context.getString(R.string.prediction_engine_mode_hybrid);
      case "none":
      default:
        return context.getString(R.string.prediction_engine_mode_none);
    }
  }

  static final class NeuralFailureStatus {
    final long timestamp;
    @NonNull final String message;

    NeuralFailureStatus(long timestamp, @NonNull String message) {
      this.timestamp = timestamp;
      this.message = message;
    }
  }
}
