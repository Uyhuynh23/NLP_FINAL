#include <jni.h>
#include <string>
#include <android/log.h>

#define TAG "EspeakJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// Global state for espeak (mock implementation)
static bool g_initialized = false;
static std::string g_dataPath;

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_nlp_1final_phonemizer_EspeakPhonemizerNative_nativeInit(
        JNIEnv* env,
        jobject /* this */,
        jstring dataPath) {

    const char* path = env->GetStringUTFChars(dataPath, nullptr);
    g_dataPath = std::string(path);
    env->ReleaseStringUTFChars(dataPath, path);

    LOGI("Espeak mock init with data path: %s", g_dataPath.c_str());

    // In a real implementation, you would call espeak_Initialize() here
    // For now, this is a mock that will use fallback phonemization
    g_initialized = true;

    return JNI_TRUE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_nlp_1final_phonemizer_EspeakPhonemizerNative_nativePhonemize(
        JNIEnv* env,
        jobject /* this */,
        jstring text,
        jstring voice) {

    if (!g_initialized) {
        LOGE("Espeak not initialized");
        return nullptr;
    }

    const char* inputText = env->GetStringUTFChars(text, nullptr);
    const char* voiceId = env->GetStringUTFChars(voice, nullptr);

    LOGI("Mock phonemize: text='%s', voice='%s'", inputText, voiceId);

    // This is a MOCK implementation
    // In a real implementation, you would:
    // 1. Call espeak_SetVoiceByName(voiceId)
    // 2. Call espeak_TextToPhonemes() to get IPA output
    // 3. Return the phoneme string

    // For now, return the input text lowercased as a fallback
    // The Kotlin fallback phonemizer will handle this
    std::string result = std::string(inputText);

    env->ReleaseStringUTFChars(text, inputText);
    env->ReleaseStringUTFChars(voice, voiceId);

    // Return null to trigger fallback in Kotlin
    return nullptr;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_nlp_1final_phonemizer_EspeakPhonemizerNative_nativeCleanup(
        JNIEnv* env,
        jobject /* this */) {

    LOGI("Espeak cleanup");
    // In a real implementation: espeak_Terminate()
    g_initialized = false;
}

