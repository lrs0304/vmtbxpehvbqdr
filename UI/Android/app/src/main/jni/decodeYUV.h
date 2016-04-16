#include <jni.h>

//// 声明使用C++编程
//#define __cplusplus

#define _ANDROID__

extern "C" {
#include "yuv2jpg.h"
};

#ifdef __cplusplus
//最好有这个，否则可能被编译器改了函数名字
extern "C" {
#endif

//分离YUV三通道
void getYUVChannelOfNV21(const BYTE *nv21, BYTE *channelY, BYTE *channelU, BYTE *channelV,
                         int width, int height);

// hello world
jstring Java_com_legaocar_rison_android_util_NativeUtil_stringFromJNI(JNIEnv *env, jclass);

void initEncoder(int width, int height);
void Java_com_legaocar_rison_android_util_NativeUtil_initJpegEncoder
        (JNIEnv *env, jclass, int width, int height);

jint Java_com_legaocar_rison_android_util_NativeUtil_compressYuvToJpeg
        (JNIEnv *env, jclass, jbyteArray byteYuv, jbyteArray byteJpg,
         int format, int quality, int width, int height);

void Java_com_legaocar_rison_android_util_NativeUtil_releaseJpegEncoder(JNIEnv *env, jclass);

#ifdef __cplusplus
}
#endif