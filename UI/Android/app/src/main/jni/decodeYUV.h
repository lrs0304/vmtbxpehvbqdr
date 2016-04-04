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

void get_Y_U_V(const unsigned char *yuvData, unsigned char *in_Y,
               unsigned char *in_U, unsigned char *in_V, int nStride, int height);

// hello world
jstring Java_com_legaocar_rison_android_util_NativeUtil_stringFromJNI(JNIEnv *env, jclass);

jlong Java_com_legaocar_rison_android_util_NativeUtil_compressYuvToJpeg
        (JNIEnv *env, jclass, jbyteArray byteYuv, jbyteArray byteJpg,
         int format, int quality, int width, int height);

#ifdef __cplusplus
}
#endif