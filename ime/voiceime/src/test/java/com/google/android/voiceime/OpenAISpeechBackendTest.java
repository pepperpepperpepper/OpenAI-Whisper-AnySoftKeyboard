package com.google.android.voiceime;

import android.content.Context;
import android.content.SharedPreferences;
import com.anysoftkeyboard.AnySoftKeyboardPlainTestRunner;
import com.google.android.voiceime.backends.OpenAISpeechBackend;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@RunWith(AnySoftKeyboardPlainTestRunner.class)
public class OpenAISpeechBackendTest {

  private Context mContext;
  private InMemorySharedPreferences mPrefs;
  private OpenAISpeechBackend mBackend;

  @Before
  public void setUp() {
    mContext = Mockito.mock(Context.class);
    Mockito.when(mContext.getString(R.string.settings_key_speech_to_text_backend))
        .thenReturn("settings_key_speech_to_text_backend");
    Mockito.when(mContext.getString(R.string.settings_key_openai_enabled))
        .thenReturn("settings_key_openai_enabled");
    Mockito.when(mContext.getString(R.string.settings_key_openai_api_key))
        .thenReturn("settings_key_openai_api_key");
    mPrefs = new InMemorySharedPreferences();
    mBackend = new OpenAISpeechBackend();
  }

  @After
  public void tearDown() {
    mPrefs.clear();
  }

  @Test
  public void testIsSelectedUsesBackendPreference() {
    mPrefs
        .edit()
        .putString("settings_key_speech_to_text_backend", OpenAISpeechBackend.ID)
        .apply();

    Assert.assertTrue(mBackend.isSelected(mContext, mPrefs));
  }

  @Test
  public void testIsSelectedFallsBackToLegacyFlag() {
    mPrefs
        .edit()
        .putString("settings_key_speech_to_text_backend", "")
        .putBoolean("settings_key_openai_enabled", true)
        .apply();

    Assert.assertTrue(mBackend.isSelected(mContext, mPrefs));
  }

  @Test
  public void testIsConfiguredFalseWhenApiKeyMissing() {
    mPrefs
        .edit()
        .putString("settings_key_speech_to_text_backend", OpenAISpeechBackend.ID)
        .putString("settings_key_openai_api_key", "")
        .apply();

    Assert.assertFalse(mBackend.isConfigured(mContext, mPrefs));
  }

  @Test
  public void testIsConfiguredTrueWhenApiKeyPresent() {
    mPrefs
        .edit()
        .putString("settings_key_speech_to_text_backend", OpenAISpeechBackend.ID)
        .putString("settings_key_openai_api_key", "sk-123")
        .apply();

    Assert.assertTrue(mBackend.isConfigured(mContext, mPrefs));
  }

  private static class InMemorySharedPreferences implements SharedPreferences {
    private final Map<String, Object> mValues = new HashMap<>();

    @Override
    public Map<String, ?> getAll() {
      return new HashMap<>(mValues);
    }

    @Override
    public String getString(String key, String defValue) {
      Object value = mValues.get(key);
      return value instanceof String ? (String) value : defValue;
    }

    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
      Object value = mValues.get(key);
      //noinspection unchecked
      return value instanceof Set ? (Set<String>) value : defValues;
    }

    @Override
    public int getInt(String key, int defValue) {
      Object value = mValues.get(key);
      return value instanceof Integer ? (Integer) value : defValue;
    }

    @Override
    public long getLong(String key, long defValue) {
      Object value = mValues.get(key);
      return value instanceof Long ? (Long) value : defValue;
    }

    @Override
    public float getFloat(String key, float defValue) {
      Object value = mValues.get(key);
      return value instanceof Float ? (Float) value : defValue;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
      Object value = mValues.get(key);
      return value instanceof Boolean ? (Boolean) value : defValue;
    }

    @Override
    public boolean contains(String key) {
      return mValues.containsKey(key);
    }

    @Override
    public Editor edit() {
      return new Editor() {
        private final Map<String, Object> mPending = new HashMap<>();
        private boolean mClear = false;

        @Override
        public Editor putString(String key, String value) {
          mPending.put(key, value);
          return this;
        }

        @Override
        public Editor putStringSet(String key, Set<String> values) {
          mPending.put(key, values);
          return this;
        }

        @Override
        public Editor putInt(String key, int value) {
          mPending.put(key, value);
          return this;
        }

        @Override
        public Editor putLong(String key, long value) {
          mPending.put(key, value);
          return this;
        }

        @Override
        public Editor putFloat(String key, float value) {
          mPending.put(key, value);
          return this;
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
          mPending.put(key, value);
          return this;
        }

        @Override
        public Editor remove(String key) {
          mPending.put(key, null);
          return this;
        }

        @Override
        public Editor clear() {
          mClear = true;
          return this;
        }

        @Override
        public boolean commit() {
          apply();
          return true;
        }

        @Override
        public void apply() {
          if (mClear) {
            mValues.clear();
            mClear = false;
          }
          for (Map.Entry<String, Object> entry : mPending.entrySet()) {
            if (entry.getValue() == null) {
              mValues.remove(entry.getKey());
            } else {
              mValues.put(entry.getKey(), entry.getValue());
            }
          }
          mPending.clear();
        }
      };
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}

    private void clear() {
      mValues.clear();
    }
  }
}
