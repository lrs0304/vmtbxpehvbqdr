#include <jni.h>
//// 声明使用C++编程
//#define __cplusplus

extern "C" {
#include "yuv2jpg.h"
};

#ifdef __cplusplus
//最好有这个，否则可能被编译器改了函数名字
extern "C" {
#endif

//分离YUV三通道
void get_Y_U_V(const BYTE *yuv, BYTE *in_Y, BYTE *in_U, BYTE *in_V, int width, int height);

// hello world
jstring Java_com_legaocar_rison_android_util_NativeUtil_stringFromJNI(JNIEnv *env, jclass);

jlong Java_com_legaocar_rison_android_util_NativeUtil_compressYuvToJpeg
        (JNIEnv *env, jclass, jbyteArray byteYuv, jbyteArray byteJpg,
         int format, int quality, int width, int height);

#ifdef __cplusplus
}
#endif