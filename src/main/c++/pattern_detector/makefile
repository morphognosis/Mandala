# Pattern detector makefile

CC = gcc

all: pattern_detector

pattern_detector: NeuralNetwork.o pattern_detector.o
	$(CC) -o pattern_detector NeuralNetwork.o pattern_detector.o -lstdc++

NeuralNetwork.o: NeuralNetwork.h NeuralNetwork.cpp
	$(CC) $(CCFLAGS) -c NeuralNetwork.cpp

pattern_detector.o: NeuralNetwork.h pattern_detector.cpp
	$(CC) $(CCFLAGS) -c pattern_detector.cpp

clean:
	/bin/rm -f *.o
