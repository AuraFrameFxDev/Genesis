// app/src/main/cpp/native-lib.cpp

#include <jni.h>
#include <string>

// This is a sample JNI function.
// The name MUST follow the pattern: Java_your_package_name_YourActivityName_yourMethodName
// Note that underscores in the package name are replaced with _1.
extern "C" /**
 * @brief Returns a greeting message from native C++ code to Java.
 *
 * Creates a Java string containing "Hello from Genesis C++ Core" and returns it to the Java caller.
 *
 * @return jstring Java string with the greeting message.
 */
JNIEXPORT jstring

JNICALL
Java_com_auraframes_fx_MainActivity_stringFromJNI(JNIEnv *env, jobject /* this */) {
    std::string hello = "Hello from Genesis C++ Core";
    return env->NewStringUTF(hello.c_str());
}