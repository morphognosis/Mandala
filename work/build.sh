#!/bin/bash
javac -classpath "../lib/*" -d . `find ../src -name '*.java' -print`
cp ../src/main/python/causations_nn.py .
jar cvfm ../bin/mandala.jar mandala.mf mandala morphognosis rdtree causations_nn.py
