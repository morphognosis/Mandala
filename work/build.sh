#!/bin/bash
javac -classpath "../lib/nd4j-common-1.0.0-beta6.jar;../lib/nd4j-api-1.0.0-beta6.jar;../lib/nd4j-native-platform-1.0.0-beta6.jar;../lib/nd4j-buffer-1.0.0-beta6.jar;../lib/commons-lang3-3.9.jar;../lib/jackson-1.0.0-beta6.jar;../lib/deeplearning4j-nn-1.0.0-beta6.jar" -d . `find ../src -name '*.java' -print`
jar cvfm ../bin/mandala.jar mandala.mf mandala morphognosis rdtree
