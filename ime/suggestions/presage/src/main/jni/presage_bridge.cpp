/*
 * Bridge between AnySoftKeyboard and the vendored Presage prediction
 * library. This implementation keeps the Presage engine entirely on
 * the native side and exposes a minimal JNI surface that mirrors the
 * Java-facing PresageNative helper.
 */

#include <android/log.h>
#include <jni.h>

#include <memory>
#include <mutex>
#include <sstream>
#include <string>
#include <unordered_map>
#include <vector>

#include "presage.h"
#include "presageCallback.h"
#include "presageException.h"

namespace {

constexpr const char* kTag = "PresageBridge";

class ContextCallback final : public PresageCallback {
 public:
  ContextCallback() = default;

  void SetContext(std::string past, std::string future) {
    past_stream_ = std::move(past);
    future_stream_ = std::move(future);
  }

  std::string get_past_stream() const override { return past_stream_; }
  std::string get_future_stream() const override { return future_stream_; }

 private:
  std::string past_stream_;
  std::string future_stream_;
};

struct PresageSession {
  std::unique_ptr<ContextCallback> callback;
  std::unique_ptr<Presage> engine;
};

std::mutex g_mutex;
std::unordered_map<jlong, std::unique_ptr<PresageSession>> g_sessions;
jlong g_next_handle = 1;

std::string JoinTokens(const std::vector<std::string>& tokens) {
  if (tokens.empty()) return std::string();
  std::ostringstream builder;
  for (size_t index = 0; index < tokens.size(); ++index) {
    if (index > 0) builder << ' ';
    builder << tokens[index];
  }
  builder << ' ';
  return builder.str();
}

std::vector<std::string> CollectContext(JNIEnv* env, jobjectArray context_array) {
  std::vector<std::string> tokens;
  if (context_array == nullptr) return tokens;
  const jsize length = env->GetArrayLength(context_array);
  tokens.reserve(static_cast<size_t>(length));
  for (jsize index = 0; index < length; ++index) {
    const auto element =
        static_cast<jstring>(env->GetObjectArrayElement(context_array, index));
    if (element == nullptr) continue;
    const char* chars = env->GetStringUTFChars(element, nullptr);
    tokens.emplace_back(chars ? chars : "");
    env->ReleaseStringUTFChars(element, chars);
    env->DeleteLocalRef(element);
  }
  return tokens;
}

PresageSession* LookupSession(jlong handle) {
  const auto it = g_sessions.find(handle);
  if (it == g_sessions.end()) return nullptr;
  return it->second.get();
}

std::vector<std::string> Predict(PresageSession* session) {
  try {
    return session->engine->predict();
  } catch (const PresageException& exception) {
    __android_log_print(ANDROID_LOG_ERROR, kTag, "predict failed: %s", exception.what());
  } catch (...) {
    __android_log_print(ANDROID_LOG_ERROR, kTag, "predict failed with unknown exception");
  }
  return {};
}

float ScoreCandidate(PresageSession* session, const std::string& candidate) {
  try {
    std::vector<std::string> filter{candidate};
    std::multimap<double, std::string> scored = session->engine->predict(filter);
    for (const auto& entry : scored) {
      if (entry.second == candidate) {
        return static_cast<float>(entry.first);
      }
    }
  } catch (const PresageException& exception) {
    __android_log_print(ANDROID_LOG_ERROR, kTag, "score failed: %s", exception.what());
  } catch (...) {
    __android_log_print(ANDROID_LOG_ERROR, kTag, "score failed with unknown exception");
  }
  return 0.f;
}

jobjectArray ToJavaStringArray(JNIEnv* env,
                               const std::vector<std::string>& predictions,
                               jint max_results) {
  const jclass string_class = env->FindClass("java/lang/String");
  if (string_class == nullptr) return nullptr;
  const jint bounded =
      max_results <= 0 ? static_cast<jint>(predictions.size())
                       : std::min(max_results, static_cast<jint>(predictions.size()));
  jobjectArray array = env->NewObjectArray(bounded, string_class, nullptr);
  if (array == nullptr) return nullptr;
  for (jint index = 0; index < bounded; ++index) {
    const std::string& value = predictions[static_cast<size_t>(index)];
    jstring java_string = env->NewStringUTF(value.c_str());
    if (java_string == nullptr) continue;
    env->SetObjectArrayElement(array, index, java_string);
    env->DeleteLocalRef(java_string);
  }
  return array;
}

}  // namespace

extern "C" JNIEXPORT jlong JNICALL
Java_com_anysoftkeyboard_suggestions_presage_PresageNative_nativeOpenModel(
    JNIEnv* env, jobject /*thiz*/, jstring model_path) {
  if (model_path == nullptr) return 0;
  const char* path_chars = env->GetStringUTFChars(model_path, nullptr);
  if (path_chars == nullptr) return 0;

  auto callback = std::make_unique<ContextCallback>();
  std::unique_ptr<Presage> engine;
  try {
    engine = std::make_unique<Presage>(callback.get(), path_chars);
  } catch (const PresageException& exception) {
    __android_log_print(ANDROID_LOG_ERROR, kTag, "Failed to create Presage: %s", exception.what());
  } catch (...) {
    __android_log_print(ANDROID_LOG_ERROR, kTag, "Failed to create Presage: unknown error");
  }

  env->ReleaseStringUTFChars(model_path, path_chars);

  if (!engine) return 0;

  auto session = std::make_unique<PresageSession>();
  session->callback = std::move(callback);
  session->engine = std::move(engine);

  std::lock_guard<std::mutex> lock(g_mutex);
  const jlong handle = g_next_handle++;
  g_sessions.emplace(handle, std::move(session));
  __android_log_print(ANDROID_LOG_INFO, kTag, "Presage session %lld created",
                      static_cast<long long>(handle));
  return handle;
}

extern "C" JNIEXPORT void JNICALL
Java_com_anysoftkeyboard_suggestions_presage_PresageNative_nativeCloseModel(
    JNIEnv* /*env*/, jobject /*thiz*/, jlong handle) {
  if (handle == 0) return;
  std::lock_guard<std::mutex> lock(g_mutex);
  const auto erased = g_sessions.erase(handle);
  __android_log_print(ANDROID_LOG_INFO, kTag, "Presage session %lld closed (%s)",
                      static_cast<long long>(handle), erased ? "ok" : "missing");
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_anysoftkeyboard_suggestions_presage_PresageNative_nativeScoreSequence(
    JNIEnv* env, jobject /*thiz*/, jlong handle, jobjectArray context, jstring candidate) {
  if (handle == 0 || candidate == nullptr) {
    return 0.f;
  }

  std::lock_guard<std::mutex> lock(g_mutex);
  PresageSession* session = LookupSession(handle);
  if (session == nullptr) {
    __android_log_print(ANDROID_LOG_WARN, kTag, "score requested for missing session %lld",
                        static_cast<long long>(handle));
    return 0.f;
  }

  std::vector<std::string> tokens = CollectContext(env, context);
  session->callback->SetContext(JoinTokens(tokens), std::string());

  const char* candidate_chars = env->GetStringUTFChars(candidate, nullptr);
  if (candidate_chars == nullptr) return 0.f;
  const float score = ScoreCandidate(session, candidate_chars);
  env->ReleaseStringUTFChars(candidate, candidate_chars);
  return score;
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_anysoftkeyboard_suggestions_presage_PresageNative_nativePredictNext(
    JNIEnv* env, jobject /*thiz*/, jlong handle, jobjectArray context, jint max_results) {
  if (handle == 0) {
    return env->NewObjectArray(0, env->FindClass("java/lang/String"), nullptr);
  }

  std::lock_guard<std::mutex> lock(g_mutex);
  PresageSession* session = LookupSession(handle);
  if (session == nullptr) {
    __android_log_print(ANDROID_LOG_WARN, kTag, "predict requested for missing session %lld",
                        static_cast<long long>(handle));
    return env->NewObjectArray(0, env->FindClass("java/lang/String"), nullptr);
  }

  std::vector<std::string> tokens = CollectContext(env, context);
  session->callback->SetContext(JoinTokens(tokens), std::string());

  const std::vector<std::string> predictions = Predict(session);
  return ToJavaStringArray(env, predictions, max_results);
}
