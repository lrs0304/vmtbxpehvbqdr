#include <stddef.h>
#include "decodeYUV.h"

// hello world
jstring Java_com_legaocar_rison_android_util_NativeUtil_stringFromJNI(JNIEnv *env, jclass) {
    return env->NewStringUTF("Lego Control Sever");
}

/**
 * format 参数暂时不使用。
 */
jlong Java_com_legaocar_rison_android_util_NativeUtil_compressYuvToJpeg
        (JNIEnv *env, jclass, jbyteArray byteYuv, jbyteArray byteJpg,
         int format, int quality, int width, int height) {
    jboolean isCopy = JNI_TRUE;
    jbyte *yuv = env->GetByteArrayElements(byteYuv, NULL);
    jbyte *jpg = env->GetByteArrayElements(byteJpg, &isCopy);

    int originLength = env->GetArrayLength(byteYuv);
    for (int i = 0; i < originLength; i++) {
        jpg[i] = yuv[i];
    }

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

    return 0;
}

/**
 * 分离YUV通道
 */
void get_Y_U_V(const unsigned char *yuvData, unsigned char *in_Y,
               unsigned char *in_U, unsigned char *in_V, int nStride, int height) {

    int i = 0, u = 0, v = 2;
    // y,u,v通道迭代计数器
    int y_n = 0, u_n = 0, v_n = 0;

    int size = nStride * height * 2;

    while (i < size) {
        if (i % 2 != 0) {
            in_Y[y_n] = yuvData[i];
            y_n++;
        } else if (i == u) {
            in_U[u_n] = yuvData[i];
            u += 4;
            u_n++;
        } else if (i == v) {
            in_V[v_n] = yuvData[i];
            v += 4;
            v_n++;
        }
        i++;
    }
}


