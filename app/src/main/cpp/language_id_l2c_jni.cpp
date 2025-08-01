#include <jni.h>
#include <string>
#include <android/log.h>

#define LOG_TAG "LanguageIdJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#ifdef __cplusplus
extern "C" {
#endif

/**
 * @brief Initializes the native language identifier and logs the provided model path.
 *
 * Converts the Java model path string to UTF-8 and logs it for potential future use. The model path is not currently used for detection.
 *
 * @return jstring The native library version string "1.2.0", or an empty string if the model path is null.
 */
JNIEXPORT jstring

JNICALL
Java_com_example_app_language_LanguageIdentifier_nativeInitialize(
        JNIEnv *env,
        jobject /* this */,
        jstring modelPath) {
    const char *path = env->GetStringUTFChars(modelPath, nullptr);
    if (path == nullptr) {
        return env->NewStringUTF("");
    }

    LOGI("Initializing with model path: %s", path);

    // Initialize language identification with basic patterns
    // This implementation uses character frequency analysis and common word patterns
    // for basic language detection without external model dependencies.
    // Currently, the rule-based nativeDetectLanguage does not require this model path,
    // but it's logged for potential future use with a model-based approach.

    env->ReleaseStringUTFChars(modelPath, path);
    return env->NewStringUTF("1.2.0"); // Updated version to reflect improvements
}

/**
 * @brief Detects the language of the input text using heuristic keyword and character analysis.
 *

 * Analyzes the input string for language-specific keywords and articles to identify Spanish ("es"), French ("fr"), German ("de"), Italian ("it"), or Portuguese ("pt"). Defaults to English ("en") if no language-specific keywords are found. If more than 10% of the characters are non-ASCII and no language is detected, returns "mul" to indicate multiple or unknown accented languages. Returns "und" if the input is null or cannot be processed.
 *
 * @param text Input text to analyze for language identification.
 * @return jstring ISO 639-1 language code: "en", "es", "fr", "de", "it", "pt", "mul", or "und".
 */
JNIEXPORT jstring

JNICALL
Java_com_example_app_language_LanguageIdentifier_nativeDetectLanguage(
        JNIEnv *env,
        jobject /* this */,
        jlong handle,
        jstring text) {
    if (text == nullptr) {
        return env->NewStringUTF("und");
    }

    const char *nativeText = env->GetStringUTFChars(text, nullptr);
    if (nativeText == nullptr) {
        return env->NewStringUTF("und");
    }

    LOGI("Detecting language for text: %s", nativeText);

    // Enhanced language detection using multiple heuristics
    std::string textStr(nativeText);
    std::string result = "en"; // Default to English

    // Convert to lowercase for case-insensitive matching
    std::transform(textStr.begin(), textStr.end(), textStr.begin(), ::tolower);

    // Language detection based on common words, articles, and patterns
    // Keywords are checked with spaces around them to avoid matching substrings within words.
    if (textStr.find(" el ") != std::string::npos ||
        textStr.find(" la ") != std::string::npos ||
        textStr.find(" de ") != std::string::npos ||
        // Also in Portuguese, but more prominent in Spanish start
        textStr.find(" que ") != std::string::npos || // Also in French/Portuguese
        textStr.find(" es ") != std::string::npos ||
        textStr.find(" con ") != std::string::npos || // Also in Italian
        textStr.find(" y ") != std::string::npos ||
        textStr.find(" en ") != std::string::npos ||
        textStr.find(" un ") != std::string::npos || // Also in French/Italian
        textStr.find(" una ") != std::string::npos) { // Also in Italian
        result = "es"; // Spanish
    } else if (textStr.find(" le ") != std::string::npos ||
               textStr.find(" la ") != std::string::npos || // Also in Spanish/Italian
               textStr.find(" et ") != std::string::npos ||
               textStr.find(" ce ") != std::string::npos ||
               textStr.find(" qui ") != std::string::npos ||
               textStr.find(" avec ") != std::string::npos ||
               textStr.find(" est ") != std::string::npos ||
               textStr.find(" dans ") != std::string::npos ||
               textStr.find(" pour ") != std::string::npos ||
               textStr.find(" un ") != std::string::npos) { // Also in Spanish/Italian
        result = "fr"; // French
    } else if (textStr.find(" und ") != std::string::npos ||
               textStr.find(" der ") != std::string::npos ||
               textStr.find(" die ") != std::string::npos ||
               textStr.find(" das ") != std::string::npos ||
               textStr.find(" mit ") != std::string::npos ||
               textStr.find(" ist ") != std::string::npos ||
               textStr.find(" ein ") != std::string::npos ||
               textStr.find(" eine ") != std::string::npos ||
               textStr.find(" auf ") != std::string::npos ||
               textStr.find(" von ") != std::string::npos) {
        result = "de"; // German
    } else if (textStr.find(" il ") != std::string::npos ||
               textStr.find(" che ") != std::string::npos ||
               textStr.find(" con ") != std::string::npos || // Also in Spanish
               textStr.find(" per ") != std::string::npos ||
               textStr.find(" sono ") != std::string::npos ||
               textStr.find(" e ") != std::string::npos || // Also in Portuguese
               textStr.find(" in ") != std::string::npos ||
               textStr.find(" un ") != std::string::npos || // Also in Spanish/French
               textStr.find(" una ") != std::string::npos || // Also in Spanish
               textStr.find(" non ") != std::string::npos) {
        result = "it"; // Italian
    } else if (textStr.find(" o ") != std::string::npos || // Common words, 'o' and 'a' are articles
               textStr.find(" a ") != std::string::npos ||
               textStr.find(" que ") != std::string::npos || // Also in Spanish/French
               textStr.find(" para ") != std::string::npos ||
               textStr.find(" com ") != std::string::npos || // Also in Spanish
               textStr.find(" e ") != std::string::npos || // Also in Italian
               textStr.find(" em ") != std::string::npos ||
               textStr.find(" um ") != std::string::npos ||
               textStr.find(" uma ") != std::string::npos ||
               textStr.find(" de ") != std::string::npos) { // Also in Spanish
        result = "pt"; // Portuguese
    }

    // Additional character frequency analysis for better accuracy
    int accentCount = 0;
    for (char c: textStr) {
        // Basic check for non-ASCII characters. A more sophisticated approach might
        // involve checking specific Unicode ranges for common accented characters.
        if (c < 0 || c > 127) accentCount++; // Non-ASCII characters
    }

    // If a significant portion of the text contains non-ASCII characters (potential accents)
    // and no specific language was detected via keywords (still "en"), classify as "mul".
    if (accentCount > textStr.length() * 0.1 && result == "en") {
        result = "mul"; // Multiple/unknown with accents
    }

    env->ReleaseStringUTFChars(text, nativeText);
    return env->NewStringUTF(result.c_str());
}

/**
 * @brief Placeholder function for releasing resources tied to a language identifier handle.
 *
 * Currently, this function performs no action because the language identification implementation is stateless and does not allocate resources per handle. It is provided for future compatibility if resource management is introduced.
 *
 * @param handle Native handle for the language identifier instance.
 */
JNIEXPORT void JNICALL
Java_com_example_app_language_LanguageIdentifier_nativeRelease(
        JNIEnv
*env,
jobject /* this */,
jlong handle
) {
// Clean up resources if needed.
// In the current implementation, nativeInitialize does not allocate any specific resources
// tied to the handle, as detection is stateless and rule-based.
// This function serves as a placeholder for potential future enhancements
// where dynamic resources might be managed.
if (handle != 0) {
// Resource cleanup completed - handle closed
LOGI("Language identifier resources cleaned up for handle: %lld (Placeholder - no specific resources allocated)",
     (long long) handle);
}
}

/**
 * @brief Retrieves the current version of the native language identifier library.
 *
 * @return jstring The version string of the native library.
 */
JNIEXPORT jstring

JNICALL
Java_com_example_app_language_LanguageIdentifier_nativeGetVersion(
        JNIEnv * env,
        jclass /* clazz */) {
    return env->NewStringUTF("1.2.0"); // Standardized version
}

#ifdef __cplusplus
}
#endif
