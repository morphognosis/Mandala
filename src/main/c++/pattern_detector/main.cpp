// main.cpp : This file contains the 'main' function. Program execution begins and ends there.
//

#include <iostream>
#include "NeuralNetwork.h"

// Dimensions.
int input_dim = 8;
int hidden_dim = 32;
int output_dim;

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

int split(string const& str, const char delim, vector<string>& out);

// Usage.
const char* usage = "pattern_detector [--network_dimensions <input_dimension>, <hidden_dimension>] [--pattern_indexes <indexes> :: = <pattern>; <pattern>; ... where <pattern> :: = <index>, <index>, ...] [--signal_quantizer <quantizer> :: = <minimum>, <increment>] [--noise_probability <probability>] [--dataset_size <size>] [--learning_rate <rate>] [--epochs <epochs>] [--random_seed <seed>]\n";

double unary(double x) 
{
	return x > .8 ? 1 : x < .2 ? 0 : x;
}

void test(NeuralNetwork& net, vector<RowVector *>& input, vector<RowVector *>& output) 
{
	cout << "Testing:" << endl;

	for (int num = 0; num < dataset_size; num++) {

		net.test(*input[num], *output[num]);

		double mse = net.mse();
		cout << "In [" << *input[num] << "] "
			<< " Desired [" << *output[num] << "] "
			<< " Out [" << net.mNeurons.back()->unaryExpr(ptr_fun(unary)) << "] "
			<< " MSE [" << mse << "]" << endl;
	}
}

void train(NeuralNetwork& net, vector<RowVector *>& input, vector<RowVector *>& output)
{
	cout << "Training:" << endl;

    int stop = 0;
	for (int i = 0; stop < 8 && i < epochs; i++) {
		cout << i + 1 << endl;
		for (int num = 0; stop < 8 && num < dataset_size; num++) {
			net.train(*input[num], *output[num]);
			double mse = net.mse();
			cout << "In [" << *input[num] << "] "
				<< " Desired [" << *output[num] << "] "
				<< " Out [" << net.mNeurons.back()->unaryExpr(ptr_fun(unary)) << "] "
				<< " MSE [" << mse << "]" << endl;
			stop = mse < 0.2 ? stop + 1 : 0;
		}
	}
}

int main(int argc, char *args[]) 
{
    // Get options.
    for (int i = 1; i < argc; i++)
    {
        if (strcmp(args[i], "-h") == 0 || strcmp(args[i], "--help") == 0)
        {
            printf(usage);
            exit(0);
        }
        if (strcmp(args[i], "-d") == 0 || strcmp(args[i], "--network_dimensions") == 0)
        {
            i++;
            if (i >= argc)
            {
                fprintf(stderr, "Invalid network_dimensions option\n");
                fprintf(stderr, usage);
                exit(1);
            }
            vector<string> dimensions;
            if (split(string(args[i]), ',', dimensions) != 2)
            {
                fprintf(stderr, "invalid network_dimensions");
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
    output_dim = (int)pattern_idxs.size();

    // Seed random numbers.
    srand(random_seed);

    // Generate pattern dataset.
    // off = 0.0, on = 1.0
    vector<RowVector *> input, output;
    for (int i = 0; i < dataset_size; i++)
    {  
        RowVector *rowIn = new RowVector(input_dim);
        for (int j = 0; j < input_dim; j++)
        {
            if ((rand() % 100) < (int)(noise_probability * 100.0f))
            {
                rowIn->coeffRef(j) = 1.0f;
            }
            else {
                rowIn->coeffRef(j) = 0.0f;
            }
        }
        int idx = rand() % (int)(pattern_idxs.size());
        for (int j : pattern_idxs[idx])
        {
            rowIn->coeffRef(j) = 1.0f;
        }
        input.push_back(rowIn);
        RowVector *rowOut = new RowVector(output_dim);
        for (int j = 0; j < output_dim; j++)
        {
            if (j == idx)
            {
                rowOut->coeffRef(j) = 1.0f;
            }
            else {
                rowOut->coeffRef(j) = 0.0f;
            }
        }
        output.push_back(rowOut);
    }

	NeuralNetwork net({ input_dim, hidden_dim, output_dim }, 0.05, NeuralNetwork::Activation::TANH);
	
	train(net, input, output);
	test(net, input, output);
	net.save("params.txt");

	cout << endl << "Neurons:" << endl;
	for(int i = 0; i < net.mNeurons.size(); i++)
		cout << *net.mNeurons[i] << endl;
	cout << endl << "Weights:" << endl;
	for (int i = 0; i < net.mWeights.size(); i++)
		cout << *net.mWeights[i] << endl;

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

