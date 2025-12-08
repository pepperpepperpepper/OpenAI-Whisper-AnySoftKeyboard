package com.anysoftkeyboard.dictionaries.presage;

import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class PresageModelSelection {

  private static final String PREF_SELECTED_MODEL_ID = "selected_model_id";

  private final SharedPreferences mSelectionPreferences;

  PresageModelSelection(@NonNull SharedPreferences selectionPreferences) {
    mSelectionPreferences = selectionPreferences;
  }

  public void persistSelectedModelId(
      @NonNull PresageModelDefinition.EngineType engineType, @NonNull String modelId) {
    if (modelId.trim().isEmpty()) {
      clearSelectedModelId(engineType);
    } else {
      mSelectionPreferences.edit().putString(selectionPrefKey(engineType), modelId).apply();
    }
  }

  @Nullable
  public String getSelectedModelId(@NonNull PresageModelDefinition.EngineType engineType) {
    final String stored = mSelectionPreferences.getString(selectionPrefKey(engineType), "");
    if (stored == null || stored.trim().isEmpty()) {
      return null;
    }
    return stored;
  }

  public void clearSelectedModelId(@NonNull PresageModelDefinition.EngineType engineType) {
    mSelectionPreferences.edit().remove(selectionPrefKey(engineType)).apply();
  }

  private String selectionPrefKey(@NonNull PresageModelDefinition.EngineType engineType) {
    return PREF_SELECTED_MODEL_ID + "_" + engineType.getSerializedValue();
  }
}
