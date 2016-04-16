#include "yuv2jpg.h"
#include <time.h>

/**
 * 将width*height的一维数组重新组装成以8*8小矩阵为大矩阵的一维数组
 * <br/>
 * JPEG算法的第一步，图像被分割成大小为8X8的小块，这些小块在整个压缩过程中都是单独被处理的
 */
void divBufferInto8x8Matrix(BYTE *pBuf, int width, int height, int nStride) {
    size_t stepLen = DCTSIZE;
    int stepBits = 3;                  //8占3字节
    int xBufs = width >> stepBits;     // width / xLen
    int yBufs = height >> stepBits;    // height / yLen;
    size_t tmpBufLen = (size_t) width << stepBits; //xBufs * xLen * yLen
    BYTE *tmpBuf = (BYTE *) malloc(tmpBufLen);

    int hIndex, wIndex;
    int k;
    int tmpBufOffset, bufOffset = 0;
    for (hIndex = 0; hIndex < yBufs; ++hIndex) {                          // 第hIndex行
        for (tmpBufOffset = 0, wIndex = 0; wIndex < xBufs; ++wIndex) {    // 第wIndex列
            //bufOffset = (yLen * hIndex) * nStride + wIndex * xLen;
            bufOffset = (hIndex * nStride + wIndex) << stepBits;          // 数据拷贝的左上角的起始地址
            for (k = 0; k < stepLen; ++k) {                               // 待拷贝矩阵的第k行
                memcpy(tmpBuf + tmpBufOffset, pBuf + bufOffset, stepLen); // 每行拷贝xLen块
                tmpBufOffset += stepLen;                                  // 临时缓冲区的偏移
                bufOffset += nStride;
            }                                                             // 完成一个小分块
        }
        memcpy(pBuf + hIndex * tmpBufLen, tmpBuf, tmpBufLen);
    }
    free(tmpBuf);
}

/**
 * 图像压缩系数，取值范围 0 ～ 100 <br/>
 * 返回校正后的quality系数
 * The basic table is used as-is (scaling 100) for a quality of 50.
 * Qualities 50..100 are converted to scaling percentage 200 - 2*Q;
 * note that at Q=100 the scaling is 0, which will cause jpeg_add_quant_table
 * to make all the table entries 1 (hence, minimum quantization loss).
 * Qualities 1..50 are converted to scaling percentage 5000/Q.
 */
int qualityScaling(int quality) {
    if (quality <= 0) quality = 1;
    if (quality > 100) quality = 100;

    if (quality < 50)
        quality = 5000 / quality;
    else
        quality = 200 - (quality << 1);

    return quality;
}

/**
 * 根据质量系数来重新生成标准量化系数矩阵
 */
void scaleSTDQuantizationTable(BYTE *y_QuantizationTable, BYTE *uv_QuantizationTable, int quality) {
    int tmpVal = 0;
    int i;
    for (i = 0; i < DCTBLOCKSIZE; ++i) {
        // Y 亮度量化表
        tmpVal = (STD_Y_Quantization_Table[i] * quality + 50) / 100;
        if (tmpVal < 1) {
            tmpVal = 1;
        } else if (tmpVal > 0xFF) {
            tmpVal = 0xFF;
        }
        y_QuantizationTable[ZigZagTable[i]] = (BYTE) tmpVal;

        // UV色差量化表
        tmpVal = (STD_UV_Quantization_Table[i] * quality + 50) / 100;
        if (tmpVal < 1) {
            tmpVal = 1;
        } else if (tmpVal > 0xFF) {
            tmpVal = 0xFF;
        }
        uv_QuantizationTable[ZigZagTable[i]] = (BYTE) tmpVal;
    }
}

/**
 * 算法使用了float AAN IDCT算法，DCT离散余弦变换后需要使用Round(Gi,j/Qi,j)<br/>
 * 来量化，这里预先计算好1/[Q(i,j)*scale(i)*scale(j)]<br/>
 */
void initQuantizationTableForAANDCT(JPEGINFO *pJpgInfo) {
    unsigned int i, j, k = 0;
    double aanScaleFactor;

    for (i = 0; i < DCTSIZE; i++) {
        for (j = 0; j < DCTSIZE; ++j, ++k) {

            aanScaleFactor = AANScaleFactor[i] * AANScaleFactor[j] * 8.0;
            pJpgInfo->YQT_DCT[k] = (float) (1.0 / (pJpgInfo->YQT[ZigZagTable[k]] * aanScaleFactor));
            pJpgInfo->UVQT_DCT[k] = (float) (1.0 /
                                             (pJpgInfo->UVQT[ZigZagTable[k]] * aanScaleFactor));

        }
    }
}

/**
 * 返回2048以内的数字的二进制长度
 */
BYTE computeVLI(short val) {
    BYTE binStrLen = 0;
    if (val < 0) val = -val;

    if (val == 0) {
        binStrLen = 0;
    } else if (val == 1) {
        binStrLen = 1;
    } else if (val <= 3) {
        binStrLen = 2;
    } else if (val <= 7) {
        binStrLen = 3;
    } else if (val <= 15) {
        binStrLen = 4;
    } else if (val <= 31) {
        binStrLen = 5;
    } else if (val <= 63) {
        binStrLen = 6;
    } else if (val <= 127) {
        binStrLen = 7;
    } else if (val <= 255) {
        binStrLen = 8;
    } else if (val <= 511) {
        binStrLen = 9;
    } else if (val <= 1023) {
        binStrLen = 10;
    } else if (val <= 2047) {
        binStrLen = 11;
    }

    return binStrLen;
}

/**
 * 生成VLI表, 为赫夫曼编码做计算准备
 */
void buildVLITable(JPEGINFO *pJpgInfo) {

    short i = 0;

    for (i = 0; i < DC_MAX_QUANTED; ++i) {
        pJpgInfo->pVLITAB[i] = computeVLI(i);
    }

    for (i = DC_MIN_QUANTED; i < 0; ++i) {
        pJpgInfo->pVLITAB[i] = computeVLI(i);
    }
}

/**
 * start of image. 图像的开始，固定为0xFFD8
 */
int writeSOI(BYTE *pOut, int nDataLen) {
    memcpy(pOut + nDataLen, &SOITAG, sizeof(SOITAG));
    return nDataLen + sizeof(SOITAG);
}

/**
 * end of image. 图像的结束，固定为0xFFD9
 */
int writeEOI(BYTE *pOut, int nDataLen) {
    memcpy(pOut + nDataLen, &EOITAG, sizeof(EOITAG));
    return nDataLen + sizeof(EOITAG);
}

/**
 * APP0，Application，应用程序保留标记0
 * +  标记代码                 2字节     固定值0xFFE0
 * +  包含9个具体字段：
      ① 数据长度              2字节     ①~⑨9个字段的总长度。即不包括标记代码，但包括本字段
      ② 标识符                5字节    固定值0x4A46494600，即字符串“JFIF0”
      ③ 版本号                2字节    一般是0x0102，表示JFIF的版本号1.2。可能会有其他数值代表其他版本
      ④ X和Y的密度单位        1字节     只有三个值可选。0：无单位；1：点数/英寸；2：点数/厘米
      ⑤ X方向像素密度         2字节
      ⑥ Y方向像素密度         2字节
      ⑦ 缩略图水平像素数目    1字节
      ⑧ 缩略图垂直像素数目    1字节
      ⑨ 缩略图RGB位图         长度可能是3的倍数           缩略图RGB位图数据

 * 本标记段可以包含图像的一个微缩版本，存为24位的RGB像素。如果没有微缩图像（这种情况更常见），
 * 则字段⑦“缩略图水平像素数目”和字段⑧“缩略图垂直像素数目”的值均为0。
 */
int writeAPP0(BYTE *pOut, int nDataLen) {
    JPEGAPP0 APP0;
    APP0.segmentTag = 0xE0FF;
    APP0.length = 0x1000;
    APP0.id[0] = 'J';
    APP0.id[1] = 'F';
    APP0.id[2] = 'I';
    APP0.id[3] = 'F';
    APP0.id[4] = 0;
    APP0.ver = 0x0101;
    APP0.densityUnit = 0x00;
    APP0.densityX = 0x0100;
    APP0.densityY = 0x0100;
    APP0.thp = 0x00;//表示没有缩略图
    APP0.tvp = 0x00;

    memcpy(pOut + nDataLen, &APP0.segmentTag, 2);
    memcpy(pOut + nDataLen + 2, &APP0.length, 2);
    memcpy(pOut + nDataLen + 4, APP0.id, 5);
    memcpy(pOut + nDataLen + 9, &APP0.ver, 2);
    *(pOut + nDataLen + 11) = APP0.densityUnit;
    memcpy(pOut + nDataLen + 12, &APP0.densityX, 2);
    memcpy(pOut + nDataLen + 14, &APP0.densityY, 2);
    *(pOut + nDataLen + 16) = APP0.thp;
    *(pOut + nDataLen + 17) = APP0.tvp;

    return nDataLen + sizeof(APP0) - 2;

}

/**
 *  DQT，Define Quantization Table，定义量化表
 * + 标记代码                2字节            固定值0xFFDB
 * + 包含9个具体字段：
   ① 数据长度               2字节            字段①和多个字段②的总长度。即不包括标记代码，但包括本字段
   ② 量化表                 2字节
         a) 精度及量化表ID   1字节            高4位：精度，只有两个可选值。0：8位；1：16位
                                            低4位：量化表ID，取值范围为0～3
         b) 表项       (64×(精度+1))字节     如8位精度的量化表。其表项长度为64×（0+1）=64字节
本标记段中，字段②可以重复出现，表示多个量化表，但最多只能出现4次。
 */
int writeDQT(JPEGINFO *pJpgInfo, BYTE *pOut, int nDataLen) {

    //unsigned int i = 0;
    JPEGDQT_8BITS dQT;
    dQT.segmentTag = 0xDBFF;//编码格式的原因
    dQT.length = 0x4300;

    //Y
    dQT.tableInfo = 0x00;   //0表示使用8*8的量化表
    /**for (i = 0; i < DCTBLOCKSIZE; i++) {
        dQT.table[i] = pJpgInfo->YQT[i];
    }*/
    memcpy(pOut + nDataLen, &dQT.segmentTag, 2);
    memcpy(pOut + nDataLen + 2, &dQT.length, 2);
    *(pOut + nDataLen + 4) = dQT.tableInfo;
    memcpy(pOut + nDataLen + 5, /**dQT.table*/pJpgInfo->YQT, 64);

    //UV
    nDataLen += sizeof(dQT) - 1;
    dQT.tableInfo = 0x01;
    /**for (i = 0; i < DCTBLOCKSIZE; i++) {
        dQT.table[i] = pJpgInfo->UVQT[i];
    }*/
    memcpy(pOut + nDataLen, &dQT.segmentTag, 2);
    memcpy(pOut + nDataLen + 2, &dQT.length, 2);
    *(pOut + nDataLen + 4) = dQT.tableInfo;
    memcpy(pOut + nDataLen + 5, /*dQT.table*/pJpgInfo->UVQT, 64);

    nDataLen += sizeof(dQT) - 1;
    return nDataLen;
}

/**
 * 交换高8位与低8位<br/>
 *JPEG文件格式中，一个字（16位）的存储使用的是 Motorola 格式,
 * 而不是 Intel 格式。也就是说, 一个字的高字节（高8位）在数据流的前面,
 * 低字节（低8位）在数据流的后面，与平时习惯的Intel格式不一样。.
 */
unsigned short convertByteFromIntelToMotorola(unsigned short val) {
    unsigned short highBits = val >> 8;
    unsigned short lowBits = val & (unsigned short) 0xFF;
    return lowBits << 8 | highBits;
}

/**
 * Start of Frame，帧图像开始
 */
int writeSOF(BYTE *pOut, int nDataLen, int width, int height) {
    JPEGSOF0_24BITS SOF;
    SOF.segmentTag = 0xC0FF;//实际为0xFFC0
    SOF.length = 0x1100;    //数据长度
    SOF.precision = 0x08;   //精度,一像素占多少字节
    SOF.height = convertByteFromIntelToMotorola((unsigned short) height);
    SOF.width = convertByteFromIntelToMotorola((unsigned short) width);
    SOF.sigNum = 0x03;      //颜色分量数,1：灰度图；3：YCrCb或YIQ；4：CMYK
    SOF.YID = 0x01;         //Y,U,V,分量信息，分别为1,2,3
    SOF.HVY = 0x11;         //水平/垂直采样因子，高位水平，低位垂直
    SOF.QTY = 0x00;         //量化表的id
    SOF.UID = 0x02;
    SOF.HVU = 0x11;
    SOF.QTU = 0x01;
    SOF.VID = 0x03;
    SOF.HVV = 0x11;
    SOF.QTV = 0x01;

    memcpy(pOut + nDataLen, &SOF.segmentTag, 2);
    memcpy(pOut + nDataLen + 2, &SOF.length, 2);
    *(pOut + nDataLen + 4) = SOF.precision;
    memcpy(pOut + nDataLen + 5, &SOF.height, 2);
    memcpy(pOut + nDataLen + 7, &SOF.width, 2);
    *(pOut + nDataLen + 9) = SOF.sigNum;
    *(pOut + nDataLen + 10) = SOF.YID;
    *(pOut + nDataLen + 11) = SOF.HVY;
    *(pOut + nDataLen + 12) = SOF.QTY;
    *(pOut + nDataLen + 13) = SOF.UID;
    *(pOut + nDataLen + 14) = SOF.HVU;
    *(pOut + nDataLen + 15) = SOF.QTU;
    *(pOut + nDataLen + 16) = SOF.VID;
    *(pOut + nDataLen + 17) = SOF.HVV;
    *(pOut + nDataLen + 18) = SOF.QTV;
    return nDataLen + sizeof(SOF) - 1;
}

/**
 * 写入 Define Huffman Table，定义哈夫曼表
 * todo memcpy与for循环效率对比
 */
int writeDHT(BYTE *pOut, int nDataLen) {
    unsigned int i = 0;

    JPEGDHT DHT;
    DHT.segmentTag = 0xC4FF;
    DHT.length = convertByteFromIntelToMotorola(19 + 12);
    DHT.tableInfo = 0x00;
    //    for (i = 0; i < 16; i++) {
    //        DHT.huffCode[i] = STD_DC_Y_NRCODES[i + 1];
    //    }

    memcpy(pOut + nDataLen, &DHT.segmentTag, 2);
    memcpy(pOut + nDataLen + 2, &DHT.length, 2);
    *(pOut + nDataLen + 4) = DHT.tableInfo;
    memcpy(pOut + nDataLen + 5, /*DHT.huffCode*/&STD_DC_Y_NRCODES[1], 16);
    nDataLen += sizeof(DHT) - 1;
    for (i = 0; i <= 11; i++) {
        nDataLen = writeByte(STD_DC_Y_VALUES[i], pOut, nDataLen);
    }
    DHT.tableInfo = 0x01;
    // for (i = 0; i < 16; i++) {
    //     DHT.huffCode[i] = STD_DC_UV_NRCODES[i + 1];
    // }
    memcpy(pOut + nDataLen, &DHT.segmentTag, 2);
    memcpy(pOut + nDataLen + 2, &DHT.length, 2);
    *(pOut + nDataLen + 4) = DHT.tableInfo;
    memcpy(pOut + nDataLen + 5, /*DHT.huffCode*/&STD_DC_UV_NRCODES[1], 16);
    nDataLen += sizeof(DHT) - 1;
    for (i = 0; i <= 11; i++) {
        nDataLen = writeByte(STD_DC_UV_VALUES[i], pOut, nDataLen);
    }
    DHT.length = convertByteFromIntelToMotorola(19 + 162);
    DHT.tableInfo = 0x10;
    //for (i = 0; i < 16; i++) {
    //    DHT.huffCode[i] = STD_AC_Y_NRCODES[i + 1];
    //}
    memcpy(pOut + nDataLen, &DHT.segmentTag, 2);
    memcpy(pOut + nDataLen + 2, &DHT.length, 2);
    *(pOut + nDataLen + 4) = DHT.tableInfo;
    memcpy(pOut + nDataLen + 5, /*DHT.huffCode*/&STD_AC_Y_NRCODES[1], 16);
    nDataLen += sizeof(DHT) - 1;
    for (i = 0; i <= 161; i++) {
        nDataLen = writeByte(STD_AC_Y_VALUES[i], pOut, nDataLen);
    }
    DHT.tableInfo = 0x11;
    // for (i = 0; i < 16; i++) {
    //     DHT.huffCode[i] = STD_AC_UV_NRCODES[i + 1];
    // }
    memcpy(pOut + nDataLen, &DHT.segmentTag, 2);
    memcpy(pOut + nDataLen + 2, &DHT.length, 2);
    *(pOut + nDataLen + 4) = DHT.tableInfo;
    memcpy(pOut + nDataLen + 5, /*DHT.huffCode*/&STD_AC_UV_NRCODES[1], 16);
    nDataLen += sizeof(DHT) - 1;
    for (i = 0; i <= 161; i++) {
        nDataLen = writeByte(STD_AC_UV_VALUES[i], pOut, nDataLen);
    }
    return nDataLen;
}

/**
 * SOS，Start of Scan，扫描开始 12字节,以固定值0xFFDA开始
 */
int writeSOS(BYTE *pOut, int nDataLen) {
    JPEGSOS_24BITS SOS;
    SOS.segmentTag = 0xDAFF;
    SOS.length = 0x0C00;
    SOS.sigNum = 0x03;
    SOS.YID = 0x01;
    SOS.HTY = 0x00;
    SOS.UID = 0x02;
    SOS.HTU = 0x11;
    SOS.VID = 0x03;
    SOS.HTV = 0x11;
    SOS.Se = 0x3F;
    SOS.Ss = 0x00;
    SOS.Bf = 0x00;
    memcpy(pOut + nDataLen, &SOS, sizeof(SOS));
    return nDataLen + sizeof(SOS);
}

/**
 * 生成标准Huffman表
 */
void buildSTDHuffTab(BYTE *nrCodes, BYTE *stdTab, HUFFCODE *huffCode) {
    BYTE i;
    BYTE j;
    BYTE k = 0;
    unsigned short code = 0;

    for (i = 1; i <= 16; i++) {
        for (j = 1; j <= nrCodes[i]; j++) {
            huffCode[stdTab[k]].code = code;
            huffCode[stdTab[k]].length = i;
            ++k;
            ++code;
        }
        code <<= 1;//code *= 2;
    }

    for (i = 0; i < k; i++) {
        huffCode[i].val = stdTab[i];
    }
}

/**
 * 写入二进制流
 */
int writeBitsStream(JPEGINFO *pJpgInfo, unsigned short value, BYTE codeLen, BYTE *out,
                    int dataLen) {

    int posVal = codeLen - 1;//bit position in the bitString , should be<=15 and >=0

    while (posVal >= 0) {
        if (value & mask[posVal]) {
            pJpgInfo->bytenew |= mask[pJpgInfo->bytePos];
        }
        posVal--;
        pJpgInfo->bytePos--;

        if (pJpgInfo->bytePos < 0) {
            if (pJpgInfo->bytenew == 0xFF) {
                dataLen = writeByte(0xFF, out, dataLen);
                dataLen = writeByte(0x00, out, dataLen);
            } else {
                dataLen = writeByte(pJpgInfo->bytenew, out, dataLen);
            }
            pJpgInfo->bytePos = 7;
            pJpgInfo->bytenew = 0;
        }
    }
    return dataLen;
}

/**
 * 赫夫曼编码的值
 */
int writeBitsHuffmanCode(JPEGINFO *pJpgInfo, HUFFCODE huffCode, BYTE *pOut, int nDataLen) {
    return writeBitsStream(pJpgInfo, huffCode.code, huffCode.length, pOut, nDataLen);
}

/*
 * AC/DC信号的振幅
 */
int writeBitsAmplitude(JPEGINFO *pJpgInfo, SYM2 sym, BYTE *pOut, int nDataLen) {
    return writeBitsStream(pJpgInfo, (unsigned short) sym.amplitude, sym.codeLen, pOut, nDataLen);
}

/**
 * 写入一字节
 */
int writeByte(BYTE val, BYTE *pOut, int nDataLen) {
    pOut[nDataLen] = val;
    return nDataLen + 1;
}

/**
 * 快速求幂
 */
double quickPower(double a, int n) {
    if (n == 0) {
        return 1;
    }

    double half = quickPower(a, n >> 1);
    return half * half * ((n & 1) == 1 ? a : 1);
}

/**
 * 将信号的振幅VLI编码,返回编码长度和信号振幅的反码
 */
SYM2 buildSym2(short value) {
    SYM2 symbol;

    // 获取编码长度
    symbol.codeLen = computeVLI(value);
    // 计算反码 //quickPower(2, Symbol.codeLen); = 1<< symbol.codeLen
    symbol.amplitude = value >= 0 ? value : (short) ((1 << symbol.codeLen) + value - 1);

    return symbol;
}

/**
 * 行程编码
 */
void RLEComp(short *lpBuf, ACSYM *lpOutBuf, BYTE *resultLen) {
    BYTE MAX_ZERO_LEN = 15;
    BYTE zeroNum = 0;
    unsigned int EOBPos;
    unsigned int outIndex = 0;

    // EOB 出现的位置（行程编码中EOB表示编码结束，这里以0来做行程编码，末端全为零则编码结束）
    for (EOBPos = DCTBLOCKSIZE - 1; (EOBPos > 0) && (lpBuf[EOBPos] == 0); EOBPos--);

    for (unsigned int i = 1; i <= EOBPos; i++) {
        if (lpBuf[i] == 0 && zeroNum < MAX_ZERO_LEN) {
            // 分块最长为15个0一组
            ++zeroNum;
        } else {
            lpOutBuf[outIndex].zeroLen = zeroNum;
            lpOutBuf[outIndex].codeLen = computeVLI(lpBuf[i]);
            lpOutBuf[outIndex].amplitude = lpBuf[i];
            zeroNum = 0;
            ++(*resultLen);
            ++outIndex;
        }
    }
}

/**
 * 快速离散余弦变换
 * http://www.docin.com/p-346223864.html<br/>
 * http://wenku.baidu.com/view/f641852a915f804d2b16c183.html<br/>
 * http://blog.csdn.net/sshcx/article/details/1674224
 */
void fastDCT(float *lpBuff) {
    float tmp0, tmp1, tmp2, tmp3, tmp4, tmp5, tmp6, tmp7;
    float tmp10, tmp11, tmp12, tmp13;
    float z1, z2, z3, z4, z5, z11, z13;
    float *dataPtr;
    int ctr;

    // 第一部分，对行进行计算
    dataPtr = lpBuff;
    for (ctr = DCTSIZE - 1; ctr >= 0; ctr--) {
        tmp0 = dataPtr[0] + dataPtr[7];
        tmp7 = dataPtr[0] - dataPtr[7];
        tmp1 = dataPtr[1] + dataPtr[6];
        tmp6 = dataPtr[1] - dataPtr[6];
        tmp2 = dataPtr[2] + dataPtr[5];
        tmp5 = dataPtr[2] - dataPtr[5];
        tmp3 = dataPtr[3] + dataPtr[4];
        tmp4 = dataPtr[3] - dataPtr[4];

        // 对偶数项进行运算
        tmp10 = tmp0 + tmp3;
        tmp13 = tmp0 - tmp3;
        tmp11 = tmp1 + tmp2;
        tmp12 = tmp1 - tmp2;

        dataPtr[0] = tmp10 + tmp11; /* phase 3 */
        dataPtr[4] = tmp10 - tmp11;

        z1 = (float) ((tmp12 + tmp13) * (0.707106781)); /* c4 */
        dataPtr[2] = tmp13 + z1; /* phase 5 */
        dataPtr[6] = tmp13 - z1;

        // 对奇数项进行计算
        tmp10 = tmp4 + tmp5; /* phase 2 */
        tmp11 = tmp5 + tmp6;
        tmp12 = tmp6 + tmp7;

        z5 = (float) ((tmp10 - tmp12) * (0.382683433)); /* c6 */
        z2 = (float) ((0.541196100) * tmp10 + z5); /* c2-c6 */
        z4 = (float) ((1.306562965) * tmp12 + z5); /* c2+c6 */
        z3 = (float) (tmp11 * (0.707106781)); /* c4 */

        z11 = tmp7 + z3;
        z13 = tmp7 - z3;

        dataPtr[5] = z13 + z2; /* phase 6 */
        dataPtr[3] = z13 - z2;
        dataPtr[1] = z11 + z4;
        dataPtr[7] = z11 - z4;

        dataPtr += DCTSIZE;
    }

    // 第二部分，对列进行计算
    dataPtr = lpBuff;
    for (ctr = DCTSIZE - 1; ctr >= 0; ctr--) {
        tmp0 = dataPtr[DCTSIZE * 0] + dataPtr[DCTSIZE * 7];
        tmp7 = dataPtr[DCTSIZE * 0] - dataPtr[DCTSIZE * 7];
        tmp1 = dataPtr[DCTSIZE * 1] + dataPtr[DCTSIZE * 6];
        tmp6 = dataPtr[DCTSIZE * 1] - dataPtr[DCTSIZE * 6];
        tmp2 = dataPtr[DCTSIZE * 2] + dataPtr[DCTSIZE * 5];
        tmp5 = dataPtr[DCTSIZE * 2] - dataPtr[DCTSIZE * 5];
        tmp3 = dataPtr[DCTSIZE * 3] + dataPtr[DCTSIZE * 4];
        tmp4 = dataPtr[DCTSIZE * 3] - dataPtr[DCTSIZE * 4];

        // 对偶数项进行运算
        tmp10 = tmp0 + tmp3;
        tmp13 = tmp0 - tmp3;
        tmp11 = tmp1 + tmp2;
        tmp12 = tmp1 - tmp2;

        dataPtr[DCTSIZE * 0] = tmp10 + tmp11; /* phase 3 */
        dataPtr[DCTSIZE * 4] = tmp10 - tmp11;

        z1 = (float) ((tmp12 + tmp13) * (0.707106781)); /* c4 */
        dataPtr[DCTSIZE * 2] = tmp13 + z1; /* phase 5 */
        dataPtr[DCTSIZE * 6] = tmp13 - z1;

        // 对奇数项进行计算
        tmp10 = tmp4 + tmp5; /* phase 2 */
        tmp11 = tmp5 + tmp6;
        tmp12 = tmp6 + tmp7;

        z5 = (float) ((tmp10 - tmp12) * (0.382683433)); /* c6 */
        z2 = (float) ((0.541196100) * tmp10 + z5); /* c2-c6 */
        z4 = (float) ((1.306562965) * tmp12 + z5); /* c2+c6 */
        z3 = (float) (tmp11 * (0.707106781)); /* c4 */

        z11 = tmp7 + z3;  /* phase 5 */
        z13 = tmp7 - z3;

        dataPtr[DCTSIZE * 5] = z13 + z2; /* phase 6 */
        dataPtr[DCTSIZE * 3] = z13 - z2;
        dataPtr[DCTSIZE * 1] = z11 + z4;
        dataPtr[DCTSIZE * 7] = z11 - z4;

        ++dataPtr;
    }
}

long timeCount = 5;

/**
 * 对8*8的的矩阵进行编码，进行的操作有离散余弦变换，量化，Zigzag，赫夫曼编码，
 */
int encodeOne8x8Block(JPEGINFO *pJpgInfo, float *lpBuf, float *quantTab, HUFFCODE *dcHuffTab,
                      HUFFCODE *acHuffTab, short *DC, BYTE *pOut, int nDataLen) {
    BYTE i = 0;                //
    BYTE acLen = 0;            // 熵编码后AC中间符号的数量
    short diffVal = 0;         // DC差异值
    unsigned int j = 0;        //
    short sigBuf[DCTBLOCKSIZE];//量化后信号缓冲
    ACSYM acSym[DCTBLOCKSIZE]; //AC中间符号缓冲
    long start = clock();
    // 快速离散余弦变换
    fastDCT(lpBuf);
    if (timeCount > 0) {
        LOGI("fast dct %ld\n", clock() - start);
        start = clock();
    }

    // 量化与Zigzag排练。 todo +16384.5-16384是为了什么
    for (i = 0; i < DCTBLOCKSIZE; i++) {
        sigBuf[ZigZagTable[i]] = (short) ((lpBuf[i] * quantTab[i] + 16384.5) - 16384);
    }
    if (timeCount > 0) {
        LOGI("quatization %ld\n", clock() - start);
        start = clock();
    }

    //---------------------------DC编码---------------------------------
    //DPCM编码, 左上角为DC直流分量
    diffVal = sigBuf[0] - *DC;
    *DC = sigBuf[0];
    //搜索Huffman表，写入相应的码字
    if (diffVal == 0) {
        nDataLen = writeBitsHuffmanCode(pJpgInfo, dcHuffTab[0], pOut, nDataLen);
    } else {
        nDataLen = writeBitsHuffmanCode(
                pJpgInfo, dcHuffTab[pJpgInfo->pVLITAB[diffVal]], pOut, nDataLen);
        nDataLen = writeBitsAmplitude(pJpgInfo, buildSym2(diffVal), pOut, nDataLen);
    }
    if (timeCount > 0) {
        LOGI("dc encode %ld\n", clock() - start);
        start = clock();
    }
    //---------------------------AC编码---------------------------------
    // 判断AC信号是否全0
    for (i = DCTBLOCKSIZE - 1; (i > 0) && (sigBuf[i] == 0); i--);

    if (i == 0) {
        // 全为零，直接写入块结束标志
        nDataLen = writeBitsHuffmanCode(pJpgInfo, acHuffTab[0x00], pOut, nDataLen);
    } else {
        // 对AC运行长度编码
        RLEComp(sigBuf, &acSym[0], &acLen);
        // 依次对AC中间符号Huffman编码
        for (j = 0; j < acLen; j++) {
            if (acSym[j].codeLen == 0) {
                nDataLen = writeBitsHuffmanCode(pJpgInfo,
                                                acHuffTab[0xF0],
                                                pOut, nDataLen);//写入(15,0)
            }
            else {
                nDataLen = writeBitsHuffmanCode(pJpgInfo,
                                                acHuffTab[acSym[j].zeroLen * 16 + acSym[j].codeLen],
                                                pOut, nDataLen);
                nDataLen = writeBitsAmplitude(pJpgInfo,
                                              buildSym2(acSym[j].amplitude),
                                              pOut, nDataLen);
            }
        }
        if (i != 63) {
            //最后一位为0的话需要写入EOB
            nDataLen = writeBitsHuffmanCode(pJpgInfo, acHuffTab[0x00], pOut, nDataLen);
        }
    }
    if (timeCount > 0) {
        LOGI("ac %ld--------------------------------------------\n", clock() - start);
    }
    timeCount--;
    return nDataLen;
}

/**
 * 开始编码:先分块，再偏移128，再分别处理YUV通道的每一方块
 */
int startEncode(JPEGINFO *pJpgInfo, BYTE *lpYBuf, BYTE *lpUBuf, BYTE *lpVBuf,
                int width, int height, BYTE *pOut, int nDataLen) {

    float dctYBuf[DCTBLOCKSIZE], dctUBuf[DCTBLOCKSIZE], dctVBuf[DCTBLOCKSIZE];

    /**
     * 余弦变换后的直流分量
     */
    short yDC = 0, uDC = 0, vDC = 0;

    unsigned int i = 0;
    unsigned int bufPos = 0;
    unsigned int matrixCounter = 0;

    // 分块数目，8*8矩阵的数目
    int mcuNum = height * width / DCTBLOCKSIZE;

    for (matrixCounter = 0; matrixCounter < mcuNum; matrixCounter++) {
        /**
         * todo memset 的效率
         * 由于离散余弦变化要求定义域的对称，所以在编码时把RGB的数值范围从[0，255]统一减去128偏移成[-128，127]。
         */
        for (i = 0; i < DCTBLOCKSIZE; i++, bufPos++) {
            dctYBuf[i] = lpYBuf[bufPos] - 128;
            dctUBuf[i] = lpUBuf[bufPos] - 128;
            dctVBuf[i] = lpVBuf[bufPos] - 128;
        }

        /**
         * 分别处理YUV三通道的小色块（8*8）
         */
        nDataLen = encodeOne8x8Block(pJpgInfo, dctYBuf, pJpgInfo->YQT_DCT, pJpgInfo->STD_DC_Y_HT,
                                     pJpgInfo->STD_AC_Y_HT, &yDC, pOut, nDataLen);
        nDataLen = encodeOne8x8Block(pJpgInfo, dctUBuf, pJpgInfo->UVQT_DCT, pJpgInfo->STD_DC_UV_HT,
                                     pJpgInfo->STD_AC_UV_HT, &uDC, pOut, nDataLen);
        nDataLen = encodeOne8x8Block(pJpgInfo, dctVBuf, pJpgInfo->UVQT_DCT, pJpgInfo->STD_DC_UV_HT,
                                     pJpgInfo->STD_AC_UV_HT, &vDC, pOut, nDataLen);

    }
    return nDataLen;
}

/**
 * 接受的YUV通道输入，每通道长度均为 width * height
 * http://blog.chinaunix.net/uid-23065002-id-3999981.html
 */
int YUV2Jpg(BYTE *in_Y, BYTE *in_U, BYTE *in_V, int width, int height, int nStride,
            int quality, BYTE *pOut, int *pnOutSize) {

    long start = clock();
    //LOGI("start");
    BYTE *pYBuf = in_Y;
    BYTE *pUBuf = in_U;
    BYTE *pVBuf = in_V;

    //LOGI("init jpgInfo");
    int nDataLen;
    JPEGINFO JpgInfo;
    memset(&JpgInfo, 0, sizeof(JPEGINFO));
    JpgInfo.bytenew = 0;
    JpgInfo.bytePos = 7;

    // 数据分块
    divBufferInto8x8Matrix(pYBuf, width, height, nStride);
    divBufferInto8x8Matrix(pUBuf, width, height, nStride);
    divBufferInto8x8Matrix(pVBuf, width, height, nStride);
    LOGI("finish divide block, time: %ld\n", clock() - start);
    start = clock();

    // 根据输入的压缩系数重新计算量化表
    quality = qualityScaling(quality);
    scaleSTDQuantizationTable(JpgInfo.YQT, JpgInfo.UVQT, quality);
    LOGI("quality table, time: %ld\n", clock() - start);
    start = clock();

    initQuantizationTableForAANDCT(&JpgInfo);
    JpgInfo.pVLITAB = JpgInfo.VLI_TAB + 2048;
    buildVLITable(&JpgInfo);
    LOGI("build vli table, time: %ld\n", clock() - start);
    start = clock();

    nDataLen = 0;
    nDataLen = writeSOI(pOut, nDataLen);
    nDataLen = writeAPP0(pOut, nDataLen);
    nDataLen = writeDQT(&JpgInfo, pOut, nDataLen);
    nDataLen = writeSOF(pOut, nDataLen, width, height);
    nDataLen = writeDHT(pOut, nDataLen);
    nDataLen = writeSOS(pOut, nDataLen);
    LOGI("finish write, time: %ld\n", clock() - start);
    start = clock();

    buildSTDHuffTab(STD_DC_Y_NRCODES, STD_DC_Y_VALUES, JpgInfo.STD_DC_Y_HT);
    buildSTDHuffTab(STD_AC_Y_NRCODES, STD_AC_Y_VALUES, JpgInfo.STD_AC_Y_HT);
    buildSTDHuffTab(STD_DC_UV_NRCODES, STD_DC_UV_VALUES, JpgInfo.STD_DC_UV_HT);
    buildSTDHuffTab(STD_AC_UV_NRCODES, STD_AC_UV_VALUES, JpgInfo.STD_AC_UV_HT);
    LOGI("finish build hufman, time: %ld\n", clock() - start);
    start = clock();

    nDataLen = startEncode(&JpgInfo, pYBuf, pUBuf, pVBuf, width, height, pOut, nDataLen);
    LOGI("finish process yuv, time: %ld\n", clock() - start);

    nDataLen = writeEOI(pOut, nDataLen);
    //LOGI("write eoi");

    *pnOutSize = nDataLen;

    return 0;
}
