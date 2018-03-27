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
    SuperpoweredFilter *filterList[MAX_FILTER_NUM];
    unsigned int samplerate;
    int filterNum;


    FilterProcess(unsigned int samplerate);

    ~FilterProcess();

    void filterProcess(float *input, float *output, unsigned int numberOfSamples);


};


#endif //FILTERPROCESS_H
