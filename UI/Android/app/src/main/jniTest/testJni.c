#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <malloc.h>
#include <errno.h>
#include "yuv2jpg.h"

void ProcessUV(BYTE *pUVBuf, BYTE *pTmpUVBuf, int width, int height, int nStride) {
    int i = 0;
    while (i < nStride * height) {
        pUVBuf[i] = pTmpUVBuf[i / 2];
        i++;
    }
}
/**
UYVY(YUV422)
*/
/*int get_Y_U_V( char*rData, char* in_Y, char* in_U, char* in_V,int nStride,int height)
{
	int i = 0;
	int y_n =0;
	int u_n =0;
	int v_n =0;
	int u = 0;
	int v = 2;
	int size = nStride*height*2;
	while(i<size){
		if(i%2 != 0){
			in_Y[y_n]= rData[i];
			y_n++;		
		}
		else if(i == u){
			in_U[u_n]= rData[i];
			u += 4;	
			u_n++;	
		}
		else if(i == v){
			in_V[v_n] = rData[i];
			v += 4;
			v_n++;
		}
		i++;
	}
	return 0;
}*/

// NV21--->YYYYYYYY UVUV
//https://wiki.videolan.org/YUV#NV21
//比较详细的YUV格式 http://stackoverflow.com/questions/5272388/extract-black-and-white-image-from-android-cameras-nv21-format
int get_Y_U_V(BYTE *yuv, BYTE *in_Y, BYTE *in_U, BYTE *in_V, int width, int height) {
    int frameSize = width * height;
    int ii = 0;
    int ij = 0;
    int di = +1;
    int dj = +1;

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

/*   for (int i = 0;i<frameSize;i++) {
            in_Y[y_n++] = yuv[i];
	if(y_n%2==1){
		continue;
	}
        
	in_V[v_n++] = 120;//(0xff & ((int) yuv[frameSize + (ci >> 1) * width + (cj & ~1) + 0]));
        in_U[u_n++] = 120;//(0xff & ((int) yuv[frameSize + (ci >> 1) * width + (cj & ~1) + 1]));
        
    }
*/
    return 0;
}


int main() {

    int width = 640, height = 360;
    int frameSize = width * height;

    BYTE *in_Y = (BYTE *) malloc(frameSize);//
    BYTE *in_U = (BYTE *) malloc(frameSize / 2);//
    BYTE *in_V = (BYTE *) malloc(frameSize / 2);//
    BYTE *pData = (BYTE *) malloc(frameSize);//
    BYTE *rData = (BYTE *) malloc(frameSize * 2);

    unsigned long dwSize = 0;
    FILE *rfp = fopen("../res/raw/origin.yuv", "rb");
    if (NULL == rfp)
        fprintf(stderr, "fopen fp error:%s\n", strerror(errno));
    fread(rData, frameSize * 2, 1, rfp);

    get_Y_U_V(rData, in_Y, in_U, in_V, width, height);

    BYTE *channelY = in_Y;//
    BYTE *channelU = (BYTE *) malloc(frameSize);//
    BYTE *channelV = (BYTE *) malloc(frameSize);//
    memset(channelU, 0, frameSize);
    memset(channelV, 0, frameSize);
    ProcessUV(channelU, in_U, width, height, width);
    ProcessUV(channelV, in_V, width, height, width);


    YUV2Jpg(channelY, channelU, channelV, width, height, 100, width, pData, &dwSize);
    FILE *fp = fopen("2.jpg", "wb");
    fwrite(pData, dwSize, 1, fp);
    fclose(fp);

    free(in_Y);
    free(in_U);
    free(in_V);
    free(channelU);
    free(channelV);
    free(pData);
}
