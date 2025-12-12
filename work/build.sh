#!/bin/bash
javac -classpath "../lib/*" -d . `find ../src -name '*.java' -print`
cp ../src/main/python/causations_nn.py .
cp ../src/main/python/causations_rnn.py .
cp ../src/main/python/attention.py .
jar cvfm ../bin/mandala.jar mandala.mf mandala morphognosis rdtree causations_nn.py causations_rnn.py attention.py
