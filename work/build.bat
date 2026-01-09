javac -classpath "../lib/*" -d . ../src/main/java/mandala/*.java
cp ..\src\main\python\mandala_nn.py .
cp ..\src\main\python\mandala_rnn.py .
jar cvfm ../bin/mandala.jar mandala.mf mandala mandala_nn.py mandala_rnn.py
pause