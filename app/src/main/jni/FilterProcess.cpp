//
// Created by arons on 2018. 02. 28..
//

#include <jni.h>
#include "FilterProcess.h"
#include <stdio.h>
#include <stdlib.h>

FilterProcess::FilterProcess(unsigned int samplerate) {
    this->samplerate = samplerate;
    filterNum = 0;
    for (int i = 0; i < MAX_FILTER_NUM; i++) {
        filterList[i] = NULL;
    }
}

FilterProcess::~FilterProcess() {
    for (int i = 0; i < MAX_FILTER_NUM; i++) {
        if (filterList[i] != NULL) {
            delete filterList[i];
        }
    }

}


void FilterProcess::filterProcess(float *input, float *output, unsigned int numberOfSamples) {
    for (int i = 0; i < MAX_FILTER_NUM; i++) {
        if (filterList[i] != NULL) {
            filterList[i]->processMono(input, output, numberOfSamples);
            input = output;
        }
    }


}

static FilterProcess *fp = NULL;

extern "C" JNIEXPORT void
Java_wavrecorder_com_fabian_aron_wavrecorder_RecorderService_FilterProcess(JNIEnv *javaEnvironment,
                                                        jobject __unused obj, jint samplerate) {
    fp = new FilterProcess((unsigned int) samplerate);
}
extern "C" JNIEXPORT void
Java_wavrecorder_com_fabian_aron_wavrecorder_RecorderService_filterProcessing(JNIEnv *javaEnvironment,
                                                           jobject __unused obj, jfloatArray input,
                                                           jfloatArray output,
                                                           jint numberOfSamples) {
    jfloat *inputArr = javaEnvironment->GetFloatArrayElements(input, JNI_FALSE);
    jfloat *outputArr = javaEnvironment->GetFloatArrayElements(output, JNI_FALSE);
    fp->filterProcess(inputArr, outputArr, (unsigned int) numberOfSamples);
    javaEnvironment->ReleaseFloatArrayElements(input, inputArr, JNI_FALSE);
    javaEnvironment->ReleaseFloatArrayElements(output, outputArr, JNI_FALSE);
}

extern "C" JNIEXPORT jint
Java_wavrecorder_com_fabian_aron_wavrecorder_RecorderService_addParametricFilter(JNIEnv *javaEnvironment,
                                                              jobject __unused obj,
                                                              jfloat frequency, jfloat octaveWidth,
                                                              jfloat dbGain) {
    if (fp->filterNum == fp->MAX_FILTER_NUM) {
        return fp->filterNum;
    }
    SuperpoweredFilter *paramFilter = new SuperpoweredFilter(SuperpoweredFilter_Parametric,
                                                             fp->samplerate);
    paramFilter->setParametricParameters(frequency, octaveWidth, dbGain);
    paramFilter->enable(true);
    fp->filterList[fp->filterNum] = paramFilter;
    if (fp->filterNum < fp->MAX_FILTER_NUM) {
        fp->filterNum++;
    }
    return fp->filterNum;
}

