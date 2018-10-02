//
// Created by arons on 2018. 02. 28..
//

#include <jni.h>
#include "FilterProcess.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

FilterProcess::FilterProcess(unsigned int samplerate) {
    this->samplerate = samplerate;
    aFilterNum = 0;
    cFilterNum = 0;
    for (int i = 0; i < MAX_FILTER_NUM; i++) {
        aFilterList[i] = NULL;
        cFilterList[i] = NULL;
    }
}

FilterProcess::~FilterProcess() {
    for (int i = 0; i < MAX_FILTER_NUM; i++) {
        if (aFilterList[i] != NULL) {
            delete aFilterList[i];
        }
        if (cFilterList[i] != NULL) {
            delete cFilterList[i];
        }
    }

}


void FilterProcess::filterProcessA(float *input, float *output, unsigned int numberOfSamples) {
    for (int i = 0; i < MAX_FILTER_NUM; i++) {
        if (aFilterList[i] != NULL) {
            aFilterList[i]->processMono(input, output, numberOfSamples);
            input = output;
        }
    }
}

void FilterProcess::filterProcessC(float *input, float *output, unsigned int numberOfSamples) {
    for (int i = 0; i < MAX_FILTER_NUM; i++) {
        if (cFilterList[i] != NULL) {
            cFilterList[i]->processMono(input, output, numberOfSamples);
            input = output;
        }
    }
}

static FilterProcess *fp = NULL;

extern "C" JNIEXPORT void
Java_wavrecorder_com_fabian_aron_wavrecorder_FilterPlugin_filterProcessDelete(
        JNIEnv *javaEnvironment,
        jobject __unused obj) {
    delete fp;
}

extern "C" JNIEXPORT void
Java_wavrecorder_com_fabian_aron_wavrecorder_FilterPlugin_filterProcessCreate(
        JNIEnv *javaEnvironment,
        jobject __unused obj, jint samplerate) {
    fp = new FilterProcess((unsigned int) samplerate);
}
extern "C" JNIEXPORT void
Java_wavrecorder_com_fabian_aron_wavrecorder_FilterPlugin_filterProcessingA(JNIEnv *javaEnvironment,
                                                                            jobject __unused obj,
                                                                            jfloatArray input,
                                                                            jfloatArray output,
                                                                            jint numberOfSamples) {
    jfloat *inputArr = javaEnvironment->GetFloatArrayElements(input, JNI_FALSE);
    jfloat *outputArr = javaEnvironment->GetFloatArrayElements(output, JNI_FALSE);
    fp->filterProcessA(inputArr, outputArr, (unsigned int) numberOfSamples);
    javaEnvironment->ReleaseFloatArrayElements(input, inputArr, JNI_FALSE);
    javaEnvironment->ReleaseFloatArrayElements(output, outputArr, JNI_FALSE);
}

extern "C" JNIEXPORT void
Java_wavrecorder_com_fabian_aron_wavrecorder_FilterPlugin_filterProcessingC(JNIEnv *javaEnvironment,
                                                                            jobject __unused obj,
                                                                            jfloatArray input,
                                                                            jfloatArray output,
                                                                            jint numberOfSamples) {
    jfloat *inputArr = javaEnvironment->GetFloatArrayElements(input, JNI_FALSE);
    jfloat *outputArr = javaEnvironment->GetFloatArrayElements(output, JNI_FALSE);
    fp->filterProcessC(inputArr, outputArr, (unsigned int) numberOfSamples);
    javaEnvironment->ReleaseFloatArrayElements(input, inputArr, JNI_FALSE);
    javaEnvironment->ReleaseFloatArrayElements(output, outputArr, JNI_FALSE);
}

extern "C" JNIEXPORT jint
Java_wavrecorder_com_fabian_aron_wavrecorder_FilterPlugin_addParametricFilterA(
        JNIEnv *javaEnvironment,
        jobject __unused obj,
        jfloat frequency,
        jfloat octaveWidth,
        jfloat dbGain) {
    if (fp->aFilterNum == fp->MAX_FILTER_NUM) {
        return fp->aFilterNum;
    }
    SuperpoweredFilter *paramFilter = new SuperpoweredFilter(SuperpoweredFilter_Parametric,
                                                             fp->samplerate);
    paramFilter->setParametricParameters(frequency, octaveWidth, dbGain);
    paramFilter->enable(true);


    fp->aFilterList[fp->aFilterNum] = paramFilter;
    if (fp->aFilterNum < fp->MAX_FILTER_NUM) {
        fp->aFilterNum++;
    }

    return fp->aFilterNum;
}

enum FILTER_TYPES {
    HPF, LPF, Parametric
};

extern "C" JNIEXPORT jint
Java_wavrecorder_com_fabian_aron_wavrecorder_FilterPlugin_addResonantFilterA(
        JNIEnv *javaEnvironment,
        jobject __unused obj,
        jint filterType,      // see enum FILTER_TYPES
        jfloat cutOffFreqency,   // cut-off frequency
        jfloat resonance) { // resonance at cut-off frequency


    if (fp->aFilterNum == fp->MAX_FILTER_NUM) {
        return fp->aFilterNum;
    }

    SuperpoweredFilter *res_filt = nullptr;

    switch (filterType) {
        case HPF:
            res_filt = new SuperpoweredFilter(SuperpoweredFilter_Resonant_Highpass, fp->samplerate);
            break;
        case LPF:
            res_filt = new SuperpoweredFilter(SuperpoweredFilter_Resonant_Lowpass, fp->samplerate);
            break;
        default :
            return 0;
    }

    res_filt->setResonantParameters(cutOffFreqency, resonance);
    res_filt->enable(true);

    fp->aFilterList[fp->aFilterNum] = res_filt;
    if (fp->aFilterNum < fp->MAX_FILTER_NUM) {
        fp->aFilterNum++;
    }

    return fp->aFilterNum;
}

extern "C" JNIEXPORT jint
Java_wavrecorder_com_fabian_aron_wavrecorder_FilterPlugin_addParametricFilterC(
        JNIEnv *javaEnvironment,
        jobject __unused obj,
        jfloat frequency,
        jfloat octaveWidth,
        jfloat dbGain) {
    if (fp->cFilterNum == fp->MAX_FILTER_NUM) {
        return fp->cFilterNum;
    }
    SuperpoweredFilter *paramFilter = new SuperpoweredFilter(SuperpoweredFilter_Parametric,
                                                             fp->samplerate);
    paramFilter->setParametricParameters(frequency, octaveWidth, dbGain);
    paramFilter->enable(true);

    fp->cFilterList[fp->cFilterNum] = paramFilter;
    if (fp->cFilterNum < fp->MAX_FILTER_NUM) {
        fp->cFilterNum++;
    }
    return fp->cFilterNum;
}

extern "C" JNIEXPORT jint
Java_wavrecorder_com_fabian_aron_wavrecorder_FilterPlugin_addResonantFilterC(
        JNIEnv *javaEnvironment,
        jobject __unused obj,
        jint filterType,      // see enum FILTER_TYPES
        jfloat cutOffFreqency,   // cut-off frequency
        jfloat resonance) { // resonance at cut-off frequency


    if (fp->cFilterNum == fp->MAX_FILTER_NUM) {
        return fp->cFilterNum;
    }

    SuperpoweredFilter *res_filt = nullptr;

    switch (filterType) {
        case HPF:
            res_filt = new SuperpoweredFilter(SuperpoweredFilter_Resonant_Highpass, fp->samplerate);
            break;
        case LPF:
            res_filt = new SuperpoweredFilter(SuperpoweredFilter_Resonant_Lowpass, fp->samplerate);
            break;
        default:
            return 0;
    }

    res_filt->setResonantParameters(cutOffFreqency, resonance);
    res_filt->enable(true);

    fp->cFilterList[fp->cFilterNum] = res_filt;
    if (fp->cFilterNum < fp->MAX_FILTER_NUM) {
        fp->cFilterNum++;
    }

    return fp->cFilterNum;
}

