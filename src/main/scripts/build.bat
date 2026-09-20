javac -classpath "../lib/*" -d . ../src/main/java/mandala/*.java
cp ..\src\main\python\mandala_nn.py .
cp ..\src\main\python\mandala_rnn.py .
cp ..\src\main\python\mandala_attention.py .
jar cvfm ../bin/mandala.jar mandala.mf mandala mandala_nn.py mandala_rnn.py mandala_attention.py
pause