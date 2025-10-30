/*
 * Copyright (C) 2025 AnySoftKeyboard
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.voiceime.backends;

import android.content.Context;
import android.content.SharedPreferences;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.voiceime.OpenAIDefaultPrompts;
import com.google.android.voiceime.OpenAITranscriber;
import com.google.android.voiceime.R;
import com.google.android.voiceime.utils.SpeechToTextFileUtils;

import java.io.File;

/** Speech-to-text backend for OpenAI Whisper / GPT transcription APIs. */
public final class OpenAISpeechBackend implements SpeechToTextBackend {

    public static final String ID = "openai";
    private static final String TAG = "OpenAISpeechBackend";

    private final OpenAITranscriber mTranscriber = new OpenAITranscriber();

    @NonNull
    @Override
    public String getId() {
        return ID;
    }

    @Override
    public boolean isSelected(@NonNull Context context, @NonNull SharedPreferences prefs) {
        String selectionKey = context.getString(R.string.settings_key_speech_to_text_backend);
        String selectedBackend = prefs.getString(selectionKey, ID);
        if (selectedBackend == null || selectedBackend.isEmpty()) {
            // Legacy flag support: fall back to OpenAI enabled flag
            String enabledKey = context.getString(R.string.settings_key_openai_enabled);
            return prefs.getBoolean(enabledKey, false);
        }
        return ID.equals(selectedBackend);
    }

    @Override
    public boolean isConfigured(@NonNull Context context, @NonNull SharedPreferences prefs) {
        if (!isSelected(context, prefs)) {
            return false;
        }
        String apiKey = prefs.getString(context.getString(R.string.settings_key_openai_api_key), "");
        return apiKey != null && !apiKey.isEmpty();
    }

    @Override
    public void showConfigurationError(@NonNull Context context) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(
                () ->
                        Toast.makeText(
                                        context,
                                        context.getString(R.string.openai_error_api_key_unset),
                                        Toast.LENGTH_LONG)
                                .show());
    }

    @Override
    public void startTranscription(
            @NonNull InputMethodService ime,
            @NonNull SharedPreferences prefs,
            @NonNull File audioFile,
            @NonNull String mediaType,
            @NonNull TranscriptionResultCallback callback) {

        Context context = ime.getApplicationContext();

        String apiKey = prefs.getString(context.getString(R.string.settings_key_openai_api_key), "");
        if (apiKey == null || apiKey.isEmpty()) {
            callback.onError(context.getString(R.string.openai_error_api_key_unset));
            return;
        }

        String endpoint =
                prefs.getString(
                        context.getString(R.string.settings_key_openai_endpoint),
                        "https://api.openai.com/v1/audio/transcriptions");
        String model =
                prefs.getString(
                        context.getString(R.string.settings_key_openai_model),
                        "gpt-4o-transcribe");
        String language =
                prefs.getString(context.getString(R.string.settings_key_openai_language), "en");
        String temperature =
                sanitizeTemperature(
                        prefs.getString(context.getString(R.string.settings_key_openai_temperature), "0.0"));
        String responseFormat =
                sanitizeResponseFormat(
                        prefs.getString(context.getString(R.string.settings_key_openai_response_format), "text"));
        String chunkingStrategy =
                sanitizeChunkingStrategy(
                        prefs.getString(
                                context.getString(R.string.settings_key_openai_chunking_strategy), "none"),
                        model);
        String prompt =
                prefs.getString(context.getString(R.string.settings_key_openai_prompt), "");

        boolean addTrailingSpace =
                prefs.getBoolean(
                        context.getString(R.string.settings_key_openai_add_trailing_space), true);
        boolean useDefaultPrompt =
                prefs.getBoolean(
                        context.getString(R.string.settings_key_openai_use_default_prompt), false);
        String defaultPromptType =
                prefs.getString(
                        context.getString(R.string.settings_key_openai_default_prompt_type),
                        "whisper");
        boolean appendCustomPrompt =
                prefs.getBoolean(
                        context.getString(R.string.settings_key_openai_append_custom_prompt), true);

        if (useDefaultPrompt && "whisper".equals(defaultPromptType)) {
            OpenAIDefaultPrompts.PromptType recommended =
                    OpenAIDefaultPrompts.getRecommendedPromptType(model);
            defaultPromptType = recommended.getValue();
        }

        File fileForUpload = audioFile;
        String destinationPreference =
                prefs.getString(context.getString(R.string.settings_key_openai_copy_destination), "");
        if (destinationPreference != null && !destinationPreference.isEmpty()) {
            File copied =
                    SpeechToTextFileUtils.copyToDirectory(
                            audioFile,
                            destinationPreference,
                            "recorded",
                            "m4a");
            if (copied != null) {
                fileForUpload = copied;
            } else {
                Log.w(
                        TAG,
                        "Failed to copy audio to "
                                + destinationPreference
                                + "; will keep original recording until cleanup.");
            }
        }

        final File originalRecording = audioFile;
        final File uploadFile = fileForUpload;
        final boolean hasCopyDestination =
                destinationPreference != null && !destinationPreference.isEmpty();
        final boolean copySucceeded = hasCopyDestination && uploadFile != originalRecording;
        final boolean retainOriginalRecording = hasCopyDestination && !copySucceeded;
        final boolean retainUploadFile = copySucceeded;

        TranscriptionResultCallback managedCallback =
                new TranscriptionResultCallback() {
                    private boolean cleaned;

                    private void cleanup() {
                        if (cleaned) {
                            return;
                        }
                        cleaned = true;
                        if (!retainOriginalRecording) {
                            deleteQuietly(originalRecording, "original recording");
                        } else {
                            Log.d(
                                    TAG,
                                    "Keeping original recording "
                                            + originalRecording.getAbsolutePath()
                                            + " because copy destination is set but copy failed.");
                        }
                        if (!retainUploadFile && uploadFile != originalRecording) {
                            deleteQuietly(uploadFile, "copied recording");
                        }
                    }

                    @Override
                    public void onTranscriptionStarted() {
                        callback.onTranscriptionStarted();
                    }

                    @Override
                    public void onSuccess(@NonNull String text) {
                        try {
                            callback.onSuccess(text);
                        } finally {
                            cleanup();
                        }
                    }

                    @Override
                    public void onError(@NonNull String errorMessage) {
                        try {
                            callback.onError(errorMessage);
                        } finally {
                            cleanup();
                        }
                    }
                };

        managedCallback.onTranscriptionStarted();
        try {
            mTranscriber.startAsync(
                context,
                fileForUpload.getAbsolutePath(),
                mediaType,
                apiKey,
                endpoint,
                model,
                language,
                temperature,
                responseFormat,
                chunkingStrategy,
                prompt,
                addTrailingSpace,
                useDefaultPrompt,
                defaultPromptType,
                appendCustomPrompt,
                new OpenAITranscriber.TranscriptionCallback() {
                    @Override
                    public void onResult(String result) {
                        managedCallback.onSuccess(result);
                    }

                    @Override
                    public void onError(String error) {
                        managedCallback.onError(error);
                    }
                });
        } catch (RuntimeException runtimeException) {
            Log.e(TAG, "Failed to start OpenAI transcription", runtimeException);
            managedCallback.onError(
                    runtimeException.getMessage() != null
                            ? runtimeException.getMessage()
                            : context.getString(R.string.openai_error_transcription_failed));
        }
    }

    private static String sanitizeTemperature(String value) {
        try {
            float parsed = Float.parseFloat(value);
            if (parsed < 0f || parsed > 1f) {
                return "0.0";
            }
            return value;
        } catch (NumberFormatException e) {
            return "0.0";
        }
    }

    private static String sanitizeResponseFormat(String responseFormat) {
        if ("json".equals(responseFormat)
                || "text".equals(responseFormat)
                || "srt".equals(responseFormat)
                || "vtt".equals(responseFormat)
                || "debug".equals(responseFormat)) {
            return responseFormat;
        }
        return "text";
    }

    private static String sanitizeChunkingStrategy(String strategy, String model) {
        if ("none".equals(strategy)) {
            return "none";
        }
        if ("gpt-4o-transcribe".equals(model) || "gpt-4o-mini-transcribe".equals(model)) {
            if ("auto".equals(strategy) || "server_vad".equals(strategy)) {
                return "{\"type\": \"server_vad\"}";
            }
            return strategy;
        }
        return "none";
    }

    private static void deleteQuietly(@NonNull File file, @NonNull String label) {
        if (!file.exists()) {
            return;
        }
        if (file.delete()) {
            Log.d(TAG, "Deleted " + label + ": " + file.getAbsolutePath());
        } else {
            Log.w(TAG, "Failed to delete " + label + ": " + file.getAbsolutePath());
        }
    }
}
