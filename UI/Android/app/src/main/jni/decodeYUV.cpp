#include "decodeYUV.h"

// hello world
jstring Java_com_legaocar_rison_android_util_NativeUtil_stringFromJNI(JNIEnv *env, jclass) {
    LOGI("Lego Control Sever %d\n", 1);
    return env->NewStringUTF("Lego Control Sever");
}

BYTE *mChannelY = NULL, *mChannelU = NULL, *mChannelV = NULL;

/**
 * 创建临时内存变量，避免频繁申请
 */
void initEncoder(int width, int height) {
    //LOGI("init encoder");
    if (mChannelY == NULL) {
        size_t totalPixels = (size_t) width * height;
        mChannelY = new BYTE[totalPixels];
        mChannelU = new BYTE[totalPixels];
        mChannelV = new BYTE[totalPixels];
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
        (JNIEnv *env, jclass, jbyteArray byteYuv, jbyteArray byteJpg,
         int format, int quality, int width, int height) {

    jboolean isCopy = JNI_TRUE;
    jbyte *yuv = env->GetByteArrayElements(byteYuv, NULL);
    jbyte *jpg = env->GetByteArrayElements(byteJpg, &isCopy);

    if (mChannelY == NULL) {
        initEncoder(width, height);
    }

    unsigned long dwSize = 0;

    //LOGI("get yuv channel");
    getYUVChannelOfNV21((BYTE *) yuv, mChannelY, mChannelU, mChannelV, width, height);
    YUV2Jpg(mChannelY, mChannelU, mChannelV, width, height, width, quality, (BYTE *) jpg, &dwSize);

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
 * NV21--->
 * YYYYYYYYYYYY...UVUVUVUVUV...
 * length of  Y = width * height
 * length of u/v= width * height / 4
 * https://wiki.videolan.org/YUV#NV21
 * 比较详细的YUV格式 http://stackoverflow.com/questions/5272388/extract-black-and-white-image-from-android-cameras-nv21-format
 */
void getYUVChannelOfNV21(const BYTE *nv21,
                         BYTE *channelY, BYTE *channelU, BYTE *channelV,
                         int width, int height) {

    long frameSize = width * height;
    long currentPosition = 0, uvStartPosition, uvPosition = 0;

    BYTE temU = 0, temV = 0;

    uvStartPosition = frameSize - width;
    for (int j = 0; j < height; j++) {
        // 主要想避免乘法运算 uvPosition = frameSize + (j >> 1) * width;
        if ((j & 1) == 0) {
            uvStartPosition += width;
        }
        uvPosition = uvStartPosition;

        // 分离YUV通道
        for (int i = 0; i < width; i++) {

            channelY[currentPosition] = nv21[currentPosition];
            if ((i & 1) == 0) {
                temV = nv21[uvPosition++];
                temU = nv21[uvPosition++];
            }
            channelV[currentPosition] = temV;
            channelU[currentPosition] = temU;

            currentPosition++;
        }

    }

//    // 插值补充
//    for (int i = 1; i < frameSize - 1; i++) {
//        if ((i & 1) == 1) {
//            //u[n] = (u[n-1]+u[n+1])/2;
//            channelV[i] = (channelV[i-1] + channelV[i+1]) >> 1;
//            channelU[i] = (channelU[i-1] + channelU[i+1]) >> 1;;
//        }
//    }
}

/**
 * 释放临时变量
 */
void Java_com_legaocar_rison_android_util_NativeUtil_releaseJpegEncoder(JNIEnv *env, jclass) {
    if (mChannelY != NULL) {
        delete[]mChannelY;
        delete[]mChannelU;
        delete[]mChannelV;

        // new 出来的内存需要使用delete清除，并将指针置为null
        mChannelY = NULL;
        mChannelU = NULL;
        mChannelV = NULL;
    }
}
