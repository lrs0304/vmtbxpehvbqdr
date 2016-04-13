#include <stdlib.h>
#include <string.h>
#include <stdlib.h>
#include <malloc.h>
#include <errno.h>
#include "../jni/yuv2jpg.h"

// NV21--->YYYYYYYY UVUV
//https://wiki.videolan.org/YUV#NV21
//比较详细的YUV格式 http://stackoverflow.com/questions/5272388/extract-black-and-white-image-from-android-cameras-nv21-format

//RGB取值范围均为0~255,Y=0~255,U=-122~+122,V=-157~+157
int get_Y_U_V(BYTE *yuv, BYTE *in_Y, BYTE *in_U, BYTE *in_V, int width, int height) {
    int frameSize = width * height;
    memset(in_Y, 1, frameSize * sizeof(BYTE));
    memset(in_U, 1, frameSize * sizeof(BYTE));
    memset(in_V, 1, frameSize * sizeof(BYTE));

    for (int j = 0, yp = 0; j < height; j++) {
        int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
        for (int i = 0; i < width; i++) {
            int y = yuv[yp];
            if (y < 0) y = 0;
            if ((i & 1) == 0) {
                v = yuv[uvp++];
                u = yuv[uvp++];
            }

            in_Y[yp] = y;
            in_U[yp] = u;
            in_V[yp] = v;
            yp++;
        }
    }

    return 0;
}


int main(int argc, char *argv[]) {

    LOGI("start width=%s, height=%s\n", argv[2], argv[3]);

    int width = 480;
    int height = 320;
    if (argc >= 4) {
        width = atoi(argv[2]);
        height = atoi(argv[3]);
    }

    unsigned long dwSize = 0;
    FILE *rfp = fopen(argv[1], "rb");
    if (NULL == rfp) {
        fprintf(stderr, "fopen fp error:%s\n", strerror(errno));
        return 0;
    }

    BYTE *in_Y = (BYTE *) malloc(width * height);//
    BYTE *in_U = (BYTE *) malloc(width * height);//
    BYTE *in_V = (BYTE *) malloc(width * height);//
    BYTE *pData = (BYTE *) malloc(width * height * 2);//
    BYTE *rData = (BYTE *) malloc(width * height * 2);

    fread(rData, width * height * 2, 1, rfp);

    LOGI("get yuv\n");
    get_Y_U_V(rData, in_Y, in_U, in_V, width, height);

    LOGI("convert\n");
    YUV2Jpg(in_Y, in_U, in_V, width, height, 75, width, pData, &dwSize);
    FILE *fp = fopen("3-1.jpg", "wb");
    fwrite(pData, dwSize, 1, fp);
    fclose(fp);

    LOGI("end\n");
    free(in_Y);
    free(in_U);
    free(in_V);
    free(pData);
}