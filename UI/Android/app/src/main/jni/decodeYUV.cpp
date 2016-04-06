#include <stddef.h>
#include <stdint.h>
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

    BYTE *in_Y = (BYTE *) malloc(width * height);//
    BYTE *in_U = (BYTE *) malloc(width * height / 2);//
    BYTE *in_V = (BYTE *) malloc(width * height / 2);//

    unsigned long dwSize = 0;

    get_Y_U_V((BYTE *) yuv, in_Y, in_U, in_V, width, height);

    YUV2Jpg(in_Y, in_U, in_V, width, height, quality, width, (BYTE *) jpg, &dwSize);

    free(in_Y);
    free(in_U);
    free(in_V);

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

    return dwSize;
}

/**
 * 分离YUV通道
 */
// NV21--->YYYYYYYY UVUV
//https://wiki.videolan.org/YUV#NV21
//比较详细的YUV格式 http://stackoverflow.com/questions/5272388/extract-black-and-white-image-from-android-cameras-nv21-format
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


