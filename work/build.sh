#!/bin/bash
javac -classpath "../lib/*" -d . `find ../src -name '*.java' -print`
jar cvfm ../bin/mandala.jar mandala.mf mandala morphognosis rdtree
