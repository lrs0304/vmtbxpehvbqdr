#include "decodeYUV.h"

// hello world
jstring Java_com_legaocar_rison_android_util_NativeUtil_stringFromJNI
        (JNIEnv *env, jobject thiz) {
    return (*env)->NewStringUTF(env, "Lego Control Sever");
}

void Java_com_legaocar_rison_android_util_NativeUtil_compressYuvToJpeg
        (JNIEnv *env, jobject thiz, jbyteArray byteYuv, jbyteArray byteJpg,
         int format, int quality, int width, int height) {
    // do something
}
