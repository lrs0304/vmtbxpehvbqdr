#include "decodeYUV.h"

// hello world
jstring  Java_com_legaocar_rison_android_util_NativeUtil_stringFromJNI( JNIEnv* env, jobject thiz ) {
        return (*env)->NewStringUTF(env, "Hello from JNI !");
}