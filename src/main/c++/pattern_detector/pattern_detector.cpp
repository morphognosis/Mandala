/*
Pattern detector.
Detect patterns in input.
Reference: "How to measure importance of inputs" by Warren S. Sarle, SAS Institute Inc., Cary, NC, USA 
     ftp://ftp.sas.com/pub/neural/importance.html
*/

/*
#include <iostream>
#include <vector>
#include <set>
#include <string>
#include <fstream>
#include <regex>
#include <iterator>
#include <map>
#include <numeric>
#include <cmath>
#include "NeuralNetwork.h"
using namespace std;

// Dimensions.
int input_dim = 8;
int hidden_dim = 32;

// Patterns.
vector<vector<int>> pattern_idxs = { {1, 4}, {3} };

// Signal quantizer.
float signal_quantizer_min = .5f;
float signal_quantizer_incr = .6f;

// Noise probability.
float noise_probability = .05f;

// Dataset size.
int dataset_size = 20;

// Learning rate.
float learning_rate = 0.3f;

// Training epochs.
int epochs = 500;

// Random seed.
int random_seed = 4517;

// Declarations.
float accuracy_metric(std::vector<int> expect, std::vector<int> predict);
int split(string const& str, const char delim, vector<string>& out);

// Usage.
const char* usage = "pattern_detector [--pattern_dimensions <input_dimension>, <hidden_dimension>] [--pattern_indexes <indexes> :: = <pattern>; <pattern>; ... where <pattern> :: = <index>, <index>, ...] [--signal_quantizer <quantizer> :: = <minimum>, <increment>] [--noise_probability <probability>] [--dataset_size <size>] [--learning_rate <rate>] [--epochs <epochs>] [--random_seed <seed>]\n";

// Main.
int main3(int argc, char* args[])
{
    // Get options.
    for (int i = 1; i < argc; i++)
    {
        if (strcmp(args[i], "-?") == 0 || strcmp(args[i], "--help") == 0)
        {
            printf(usage);
            exit(0);
        }
        if (strcmp(args[i], "-d") == 0 || strcmp(args[i], "--pattern_dimensions") == 0)
        {
            i++;
            if (i >= argc)
            {
                fprintf(stderr, "Invalid pattern_dimensions option\n");
                fprintf(stderr, usage);
                exit(1);
            }
            vector<string> dimensions;
            if (split(string(args[i]), ',', dimensions) != 2)
            {
                fprintf(stderr, "invalid pattern_dimensions");
                fprintf(stderr, usage);
                exit(1);
            }
            input_dim = atoi(dimensions[0].c_str());
            if (input_dim <= 0)
            {
                fprintf(stderr, "invalid input dimension");
                exit(1);
            }
            hidden_dim = atoi(dimensions[1].c_str());
            if (hidden_dim <= 0)
            {
                fprintf(stderr, "invalid hidden dimension");
                exit(1);
            }
            continue;
        }
        if (strcmp(args[i], "-i") == 0 || strcmp(args[i], "--pattern_indexes") == 0)
        {
            i++;
            if (i >= argc)
            {
                fprintf(stderr, "Invalid pattern_indexes option\n");
                fprintf(stderr, usage);
                exit(1);
            }
            pattern_idxs.clear();
            vector<string> patterns;
            split(string(args[i]), ';', patterns);
            for (int j = 0; j < patterns.size(); j++)
            {
                vector<string> sidxs;
                vector<int> idxs;
                split(patterns[j], ',', sidxs);
                for (string idx : sidxs)
                {
                    idxs.push_back(atoi(idx.c_str()));
                }
                pattern_idxs.push_back(idxs);
            }
            continue;
        }
        if (strcmp(args[i], "-q") == 0 || strcmp(args[i], "--signal_quantizer") == 0)
        {
            i++;
            if (i >= argc)
            {
                fprintf(stderr, "Invalid signal_quantizer option\n");
                fprintf(stderr, usage);
                exit(1);
            }
            vector<string> quantizers;
            if (split(string(args[i]), ',', quantizers) != 2)
            {
                fprintf(stderr, "invalid signal_quantizer");
                fprintf(stderr, usage);
                exit(1);
            }
            signal_quantizer_min = (float)atof(quantizers[0].c_str());
            if (signal_quantizer_min < 0.0f || signal_quantizer_min > 1.0f)
            {
                fprintf(stderr, "invalid signal_quantizer_min");
                exit(1);
            }
            signal_quantizer_incr = (float)atof(quantizers[1].c_str());
            if (signal_quantizer_incr <= 0.0f)
            {
                fprintf(stderr, "signal_quantizer increment must be > 0");
                exit(1);
            }
            continue;
        }
        if (strcmp(args[i], "-p") == 0 || strcmp(args[i], "--noise_probability") == 0)
        {
            i++;
            if (i >= argc)
            {
                fprintf(stderr, "Invalid noise_probability option\n");
                fprintf(stderr, usage);
                exit(1);
            }
            noise_probability = (float)atof(args[i]);
            if (noise_probability < 0.0f || noise_probability > 1.0f)
            {
                fprintf(stderr, "invalid noise_probability");
                exit(1);
            }
            continue;
        }
        if (strcmp(args[i], "-n") == 0 || strcmp(args[i], "--dataset_size") == 0)
        {
            i++;
            if (i >= argc)
            {
                fprintf(stderr, "Invalid dataset_size option\n");
                fprintf(stderr, usage);
                exit(1);
            }
            dataset_size = atoi(args[i]);
            if (dataset_size < 0)
            {
                fprintf(stderr, "invalid dataset_size");
                exit(1);
            }
            continue;
        }
        if (strcmp(args[i], "-e") == 0 || strcmp(args[i], "--epochs") == 0)
        {
            i++;
            if (i >= argc)
            {
                fprintf(stderr, "Invalid epochs option\n");
                fprintf(stderr, usage);
                exit(1);
            }
            epochs = atoi(args[i]);
            if (epochs < 0)
            {
                fprintf(stderr, "invalid epochs");
                exit(1);
            }
            continue;
        }
        if (strcmp(args[i], "-l") == 0 || strcmp(args[i], "--learning_rate") == 0)
        {
            i++;
            if (i >= argc)
            {
                fprintf(stderr, "Invalid learning_rate option\n");
                fprintf(stderr, usage);
                exit(1);
            }
            learning_rate = (float)atof(args[i]);
            if (learning_rate <= 0.0f)
            {
                fprintf(stderr, "invalid learning_rate");
                exit(1);
            }
            continue;
        }
        if (strcmp(args[i], "-r") == 0 || strcmp(args[i], "--random_seed") == 0)
        {
            i++;
            if (i >= argc)
            {
                fprintf(stderr, "Invalid random_seed option\n");
                fprintf(stderr, usage);
                exit(1);
            }
            random_seed = atoi(args[i]);
            continue;
        }
        printf(usage);
        exit(1);
    }
    if (pattern_idxs.size() == 0)
    {
        printf("pattern index lengths must be > 0");
        printf(usage);
        exit(1);
    }
    int n_outputs = pattern_idxs.size();

    // Seed random numbers.
    srand(random_seed);

    // Generate pattern dataset.
    // off = 0.0, on = 1.0
    vector<vector<float>> dataset;
    for (int i = 0; i < dataset_size; i++)
    {
        vector<float> row;
        for (int j = 0; j < input_dim; j++)
        {
            if ((rand() % 100) < (int)(noise_probability * 100.0f))
            {
                row.push_back(1.0f);
            }
            else {
                row.push_back(0.0f);
            }
        }
        int idx = rand() % (int)(pattern_idxs.size());
        for (int j : pattern_idxs[idx])
        {
            row[j] = 1.0f;
        }
        row.push_back((float)idx);
        dataset.push_back(row);
    }

    // Train the network.
    //Network* network = new Network();
    //network->initialize_network(input_dim, hidden_dim, n_outputs);
    //network->train(dataset, learning_rate, epochs, n_outputs);

    // Predict.
    vector<int> expected;
    for (auto& row : dataset)
    {
        expected.push_back(static_cast<int>(row.back()));
        //row.back() = 42;
    }
    vector<int> predicted;
    for (const auto& row : dataset)
    {
        vector<float> outputs;
        //outputs = network->forward_propagate(row);
        for (float i : row)
        {
            printf("%f ", i);
        }
        printf("\n");
        for (float o : outputs)
        {
            printf("%f ", o);
        }
        printf("\n");
        int n = std::max_element(outputs.begin(), outputs.end()) - outputs.begin();
        predicted.push_back(n);
        printf("%d/%d\n", static_cast<int>(row.back()), n);
    }
    printf("Accuracy=%f\n", accuracy_metric(expected, predicted));

                            # Detect patterns.
                            print('patterns:')
                            for i in range(dataset_size) :
                                print('pattern #', i, ':', sep = '')
                                threshold = signal_quantizer_min
                                while threshold <= 1.0 :
                                    print('signal threshold=', threshold, sep = '')
                                    input_pattern = np.array([input_data[i]])
                                    predicted_pattern = pattern_model.predict(input_pattern)
                                    print('input:', input_pattern[0])
                                    print('prediction:', predicted_pattern[0])
                                    print('pattern: [', end = '')
                                    for j in range(output_dim) :
                                        if predicted_pattern[0][j] >= threshold :
                                            print('1', end = '')
                                        else :
                                            print('0', end = '')
                                            if j < output_dim - 1 :
                                                print(', ', end = '')
                                                print(']')
                                                print('input importance detection:')
                                                idx = 0
                                                max = predicted_pattern[0][0]
                                                for j in range(output_dim) :
                                                    if predicted_pattern[0][j] > max:
    idx = j
        max = predicted_pattern[0][j]
        for j in range(input_dim) :
            if input_pattern[0][j] == 1 :
                print('input idx=', j, sep = '')
                input_pattern[0][j] = 0
                predicted_pattern = pattern_model.predict(input_pattern)
                print('input:', input_pattern[0])
                print('prediction:', predicted_pattern[0])
                print('delta=', (max - predicted_pattern[0][idx]), sep = '')
                input_pattern[0][j] = 1
                threshold += signal_quantizer_incr
 
    return 0;
}

// Split string.
int split(string const& str, const char delim, vector<string>& out)
{
    size_t start;
    size_t end = 0;

    out.clear();
    while ((start = str.find_first_not_of(delim, end)) != string::npos)
    {
        end = str.find(delim, start);
        out.push_back(str.substr(start, end - start));
    }
    return (int)out.size();
}
*/

