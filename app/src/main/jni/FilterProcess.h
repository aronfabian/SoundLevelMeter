//
// Created by arons on 2018. 02. 28..
//

#ifndef FILTERPROCESS_H
#define FILTERPROCESS_H


#include "FilterProcess.h"
#include <SuperpoweredFilter.h>


class FilterProcess {
public:
    static const int MAX_FILTER_NUM = 15;
    SuperpoweredFilter *aFilterList[MAX_FILTER_NUM], *cFilterList[MAX_FILTER_NUM];
    unsigned int samplerate;
    int aFilterNum, cFilterNum;
    FilterProcess *fp;


    FilterProcess(unsigned int samplerate);

    ~FilterProcess();

    void filterProcessA(float *input, float *output, unsigned int numberOfSamples);

    void filterProcessC(float *input, float *output, unsigned int numberOfSamples);


};


#endif //FILTERPROCESS_H
