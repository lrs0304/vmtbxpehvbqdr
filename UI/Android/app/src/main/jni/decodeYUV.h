#include <jni.h>

#include <string.h>
// 声明使用C++编程
#define __cplusplus

// extern "C" {
// 	#include "yuv2jpg.h"
// };

#ifdef __cplusplus
//最好有这个，否则可能被编译器改了函数名字
extern "C" {
#endif

// hello world
jstring Java_com_legaocar_rison_android_util_NativeUtil_stringFromJNI(JNIEnv *env, jclass);

void Java_com_legaocar_rison_android_util_NativeUtil_compressYuvToJpeg
        (JNIEnv *env, jclass, jbyteArray byteYuv, jbyteArray byteJpg,
         int format, int quality, int width, int height);

#ifdef __cplusplus
}
#endif