#include <stddef.h>
#include <stdint.h>
#include "decodeYUV.h"

// hello world
jstring Java_com_legaocar_rison_android_util_NativeUtil_stringFromJNI(JNIEnv *env, jclass) {
    LOGI("Lego Control Sever %d\n", 1);
    return env->NewStringUTF("Lego Control Sever");
}

BYTE *in_Y = NULL, *in_U = NULL, *in_V = NULL;

/**
 * 创建临时内存变量，避免频繁申请
 */
void initEncoder(int width, int height) {
    if (in_Y == NULL) {
        size_t totalPixels = (size_t) width * height;
        in_Y = (BYTE *) malloc(totalPixels);
        in_U = (BYTE *) malloc(totalPixels >> 1);
        in_V = (BYTE *) malloc(totalPixels >> 1);
    }

}

void Java_com_legaocar_rison_android_util_NativeUtil_initJpegEncoder
        (JNIEnv *env, jclass, int width, int height) {
    initEncoder(width, height);
}

/**
 * format 参数暂时不使用。
 */
jlong Java_com_legaocar_rison_android_util_NativeUtil_compressYuvToJpeg
        (JNIEnv *env, jclass obj, jbyteArray byteYuv, jbyteArray byteJpg,
         int format, int quality, int width, int height) {
    jboolean isCopy = JNI_TRUE;
    jbyte *yuv = env->GetByteArrayElements(byteYuv, NULL);
    jbyte *jpg = env->GetByteArrayElements(byteJpg, &isCopy);

    if (in_Y == NULL) {
        initEncoder(width, height);
        LOGI("init encoder");
    }

    unsigned long dwSize = 0;

    LOGI("get yuv");
    get_Y_U_V((BYTE *) yuv, in_Y, in_U, in_V, width, height);

    LOGI("yuv convert");
    YUV2Jpg(in_Y, in_U, in_V, width, height, quality, width, (BYTE *) jpg, &dwSize);

    LOGI("release");
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

    LOGI("finish");
    return dwSize;
}

/**
 * 分离YUV通道
 */
// NV21--->YYYYYYYY UVUV
//https://wiki.videolan.org/YUV#NV21
//比较详细的YUV格式 http://stackoverflow.com/questions/5272388/extract-black-and-white-image-from-android-cameras-nv21-format
/*void get_Y_U_V(const BYTE *yuv, BYTE *in_Y, BYTE *in_U, BYTE *in_V, int width, int height) {

    int frameSize = width * height, uvp;

    BYTE u = 0, v = 0;

    for (int j = 0, yp = 0; j < height; j++) {
        uvp = frameSize + (j >> 1) * width;
        for (int i = 0; i < width; i++, yp++) {
            in_Y[yp] = yuv[yp];
            if (in_Y[yp] < 0) {
                in_Y[yp] = 0;
            }

            if ((i & 1) == 0) {
                v = yuv[uvp++];
                u = yuv[uvp++];
            }

            in_V[yp] = v;
            in_U[yp] = u;
        }
    }
}*/
void get_Y_U_V(const BYTE *yuv, BYTE *in_Y, BYTE *in_U, BYTE *in_V, int width, int height) {

    int frameSize = width * height;

    int y_n = 0, u_n = 0, v_n = 0;

    for (int i = 0; i < height; i++) {
        for (int j = 0; j < width; j++) {
            in_Y[y_n++] = yuv[i * width + j];

            if (j % 2 == 0) {// 只要单数列的UV

                if (i % 2 == 0) {//第一行
                    in_U[u_n++] = yuv[frameSize + i * width / 2];
                    in_V[v_n++] = yuv[frameSize + i * width / 2 + 1];
                } else { // 第二行
                    in_U[u_n] = in_U[u_n - width / 2];
                    u_n++;
                    in_V[v_n] = in_V[v_n - width / 2];
                    v_n++;
                }
            }
        }
    }

}

/**
 * 释放临时变量
 */
void Java_com_legaocar_rison_android_util_NativeUtil_releaseJpegEncoder(JNIEnv *env, jclass) {
    if (in_Y != NULL) {
        free(in_Y);
        free(in_U);
        free(in_V);
    }
}
