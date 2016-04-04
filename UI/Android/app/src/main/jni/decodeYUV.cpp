#include <stddef.h>
#include "decodeYUV.h"

// hello world
jstring Java_com_legaocar_rison_android_util_NativeUtil_stringFromJNI(JNIEnv *env, jclass) {
    return env->NewStringUTF("Lego Control Sever");
}

void Java_com_legaocar_rison_android_util_NativeUtil_compressYuvToJpeg
        (JNIEnv *env, jclass, jbyteArray byteYuv, jbyteArray byteJpg,
         int format, int quality, int width, int height) {
    // todo something
    jbyte *yuv = env->GetByteArrayElements(byteYuv, NULL);
    jbyte *jpg = env->GetByteArrayElements(byteJpg, NULL);

    strcpy((char *) jpg, (char *) yuv);
//    SkWStream* strm = CreateJavaOutputStreamAdaptor(env, jstream, jstorage);
//
//    jint* imgOffsets = env->GetIntArrayElements(offsets, NULL);
//    jint* imgStrides = env->GetIntArrayElements(strides, NULL);

    /**
     * 调用了Get就必须调用Release
     * The array is returned to the calling Java language method, which in turn,
     * garbage collects the reference to the array when it is no longer used.
     * The array can be explicitly freed with the following call.
     *
     * (*env)-> ReleaseByteArrayElements(env, jb, (jbyte *)m, 0);
     *
     * The last argument to the ReleaseByteArrayElements function above can have the following values:
     *
     * 0:           Updates to the array from within the C code are reflected in the Java language copy.
     * JNI_COMMIT:  The Java language copy is updated, but the local jbyteArray is not freed.
     * JNI_ABORT:   Changes are not copied back, but the jbyteArray is freed.
     *              The value is used only if the array is obtained with a get mode
     *              of JNI_TRUE meaning the array is a copy.
     */
    env->ReleaseByteArrayElements(byteYuv, yuv, JNI_ABORT);
    env->ReleaseByteArrayElements(byteJpg, jpg, 0);
}

