// For conditions of distribution and use, see copyright notice in Mandala.java

// World causation.
// Generate causation hierarchies and paths in the world.

package mandala;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;
import org.nd4j.linalg.primitives.Pair;

public class WorldCausation
{
	// Generation parameters.
	public static int NUM_CAUSATION_HIERARCHIES= 1;
	public static int NUM_NONTERMINALS = 10;
	public static int NUM_TERMINALS = 10;	
	public static int MIN_PRODUCTION_RHS_LENGTH = 2;
	public static int MAX_PRODUCTION_RHS_LENGTH = 5;
	public static float MIN_CAUSATION_PROBABILITY = 0.1f;
	public static float MAX_CAUSATION_PROBABILITY = 0.9f;
	public static float TERMINAL_PRODUCTION_PROBABILITY = 0.1f;
	
	// Feature dimensions.
	public static int NUM_NONTERMINAL_DIMENSIONS = 64;
	public static int NUM_NONTERMINAL_FEATURES = 3;	
	public static int NUM_TERMINAL_DIMENSIONS = 16;
	public static int NUM_TERMINAL_FEATURES = 3;
	
	// Causation.
	public static class Causation
	{
		public int id;
		public ArrayList<Boolean> features;
		public boolean terminal;		
		public Causation parent;
		public ArrayList<Causation> children;
		public ArrayList<Float> probabilities;
	};
	public static ArrayList<ArrayList<Causation>> causations;
	
	// Graph.
	public static String CAUSATIONS_GRAPH_FILENAME = "causations.dot";
		
	// Causation paths.
	public static int NUM_CAUSATION_PATHS = 5;
	public static ArrayList<Causation> causationPaths;

	// Dataset exports.
	public static String PATH_RNN_DATASET_FILENAME = "causation_paths_rnn_dataset.py";
	public static float PATH_RNN_DATASET_TRAIN_FRACTION = 0.75f;
	public static String PATH_TCN_DATASET_FILENAME = "causation_paths_tcn_dataset.py";
	public static float PATH_TCN_DATASET_TRAIN_FRACTION = 0.75f;	
	public static String PATH_NN_DATASET_FILENAME = "causation_paths_nn_dataset.csv";
	public static float PATH_NN_DATASET_TRAIN_FRACTION = 0.75f;
	
	// Random numbers.
	public static int RANDOM_SEED = 4517;
	public static Random randomizer = null;
	
	// Verbosity.
	public static boolean VERBOSE = true;
	
    // Usage.
    public static final String Usage =
      "Usage:\n" +
      "    java mandala.WorldCausation\n" +
      "      [-numCausationHierarchies <quantity> (default=" + NUM_CAUSATION_HIERARCHIES + ")]\n" +    		  
      "      [-numNonterminals <quantity> (default=" + NUM_NONTERMINALS + ")]\n" +
      "      [-numTerminals <quantity> (default=" + NUM_TERMINALS + ")]\n" +      
      "      [-minProductionRightHandSideLength <quantity> (default=" + MIN_PRODUCTION_RHS_LENGTH + ")]\n" +
      "      [-maxProductionRightHandSideLength <quantity> (default=" + MAX_PRODUCTION_RHS_LENGTH + ")]\n" +
      "      [-minCausationProbability <probability> (default=" + MIN_CAUSATION_PROBABILITY + ")]\n" +
      "      [-maxCausationProbability <probability> (default=" + MAX_CAUSATION_PROBABILITY + ")]\n" + 
      "      [-terminalProductionProbability <probability> (default=" + TERMINAL_PRODUCTION_PROBABILITY + ")]\n" +      
      "      [-numNonterminalDimensions <quantity> (default=" + NUM_NONTERMINAL_DIMENSIONS + ")]\n" +
      "      [-numNonterminalFeatures <quantity> (default=" + NUM_NONTERMINAL_FEATURES + ")]\n" +      
      "      [-numTerminalDimensions <quantity> (default=" + NUM_TERMINAL_DIMENSIONS + ")]\n" +       
      "      [-numTerminalFeatures <quantity> (default=" + NUM_TERMINAL_FEATURES + ")]\n" +      
      "      [-exportCausationsGraph [<file name> (Graphviz dot format, default=" + CAUSATIONS_GRAPH_FILENAME + ")]]\n" +      
      "      [-numCausationPaths <quantity> (default=" + NUM_CAUSATION_PATHS + ")]\n" +
      "      [-exportPathNNdataset [<file name (default=\"" + PATH_NN_DATASET_FILENAME + "\")>]\n" +     
      "          [-NNdatasetTrainFraction <fraction> (default=" + PATH_NN_DATASET_TRAIN_FRACTION + ")]]\n" +      
      "      [-exportPathRNNdataset [<file name (default=\"" + PATH_RNN_DATASET_FILENAME + "\")>]\n" +
      "          [-RNNdatasetTrainFraction <fraction> (default=" + PATH_RNN_DATASET_TRAIN_FRACTION + ")]]\n" +
      "      [-exportPathTCNdataset [<file name (default=\"" + PATH_TCN_DATASET_FILENAME + "\")>]\n" +
      "          [-TCNdatasetTrainFraction <fraction> (default=" + PATH_TCN_DATASET_TRAIN_FRACTION + ")]]\n" +           
      "      [-randomSeed <seed> (default=" + RANDOM_SEED + ")]\n" +      
      "      [-verbose <\"true\" | \"false\"> (verbosity, default=" + VERBOSE + ")]\n" +      
      "Exit codes:\n" +
      "  0=success\n" +
      "  1=error";
	
    // Main.
    public static void main(String[] args) 
    {
    	boolean gotExportCausationsGraph = false;
    	boolean gotExportPathNNdataset = false;
    	boolean gotExportPathRNNdataset = false;
    	boolean gotExportPathTCNdataset = false;
    	boolean gotNNdatasetTrainFraction = false;
    	boolean gotRNNdatasetTrainFraction = false;
    	boolean gotTCNdatasetTrainFraction = false;    	
        for (int i = 0; i < args.length; i++)
        {
            if (args[i].equals("-numCausationHierarchies"))
            {
               i++;
               if (i >= args.length)
               {
                  System.err.println("Invalid numCausationHierarchies option");
                  System.err.println(Usage);
                  System.exit(1);
               }
               try
               {
                  NUM_CAUSATION_HIERARCHIES = Integer.parseInt(args[i]);
               }
               catch (NumberFormatException e) {
                  System.err.println("Invalid numCausationHierarchies option");
                  System.err.println(Usage);
                  System.exit(1);
               }
               if (NUM_CAUSATION_HIERARCHIES < 0)
               {
                  System.err.println("Invalid numCausationHierarchies option");
                  System.err.println(Usage);
                  System.exit(1);
               }              
               continue;
            }        	
           if (args[i].equals("-numNonterminals"))
           {
              i++;
              if (i >= args.length)
              {
                 System.err.println("Invalid numNonterminals option");
                 System.err.println(Usage);
                 System.exit(1);
              }
              try
              {
                 NUM_NONTERMINALS = Integer.parseInt(args[i]);
              }
              catch (NumberFormatException e) {
                 System.err.println("Invalid numNonterminals option");
                 System.err.println(Usage);
                 System.exit(1);
              }
              if (NUM_NONTERMINALS < 0)
              {
                 System.err.println("Invalid numNonterminals option");
                 System.err.println(Usage);
                 System.exit(1);
              }              
              continue;
           }
           if (args[i].equals("-numTerminals"))
           {
              i++;
              if (i >= args.length)
              {
                 System.err.println("Invalid numTerminals option");
                 System.err.println(Usage);
                 System.exit(1);
              }
              try
              {
                 NUM_TERMINALS = Integer.parseInt(args[i]);
              }
              catch (NumberFormatException e) {
                 System.err.println("Invalid numTerminals option");
                 System.err.println(Usage);
                 System.exit(1);
              }
              if (NUM_TERMINALS < 0)
              {
                 System.err.println("Invalid numTerminals option");
                 System.err.println(Usage);
                 System.exit(1);
              }              
              continue;
           }
           if (args[i].equals("-minCausationProbability"))
           {
              i++;
              if (i >= args.length)
              {
                 System.err.println("Invalid minCausationProbability option");
                 System.err.println(Usage);
                 System.exit(1);
              }
              try
              {
                 MIN_CAUSATION_PROBABILITY = Float.parseFloat(args[i]);
              }
              catch (NumberFormatException e) {
                 System.err.println("Invalid minCausationProbability option");
                 System.err.println(Usage);
                 System.exit(1);
              }
              if (MIN_CAUSATION_PROBABILITY < 0.0f)
              {
                 System.err.println("Invalid minCausationProbability option");
                 System.err.println(Usage);
                 System.exit(1);
              }
              continue;
           }
           if (args[i].equals("-maxCausationProbability"))
           {
              i++;
              if (i >= args.length)
              {
                 System.err.println("Invalid maxCausationProbability option");
                 System.err.println(Usage);
                 System.exit(1);
              }
              try
              {
                 MAX_CAUSATION_PROBABILITY = Float.parseFloat(args[i]);
              }
              catch (NumberFormatException e) {
                 System.err.println("Invalid maxCausationProbability option");
                 System.err.println(Usage);
                 System.exit(1);
              }
              if (MAX_CAUSATION_PROBABILITY > 1.0f)
              {
                 System.err.println("Invalid maxCausationProbability option");
                 System.err.println(Usage);
                 System.exit(1);
              }
              continue;
           }
           if (args[i].equals("-terminalProductionProbability"))
           {
              i++;
              if (i >= args.length)
              {
                 System.err.println("Invalid terminalProductionProbability option");
                 System.err.println(Usage);
                 System.exit(1);
              }
              try
              {
                 TERMINAL_PRODUCTION_PROBABILITY = Float.parseFloat(args[i]);
              }
              catch (NumberFormatException e) {
                 System.err.println("Invalid terminalProductionProbability option");
                 System.err.println(Usage);
                 System.exit(1);
              }
              if (TERMINAL_PRODUCTION_PROBABILITY < 0.0f || TERMINAL_PRODUCTION_PROBABILITY > 1.0f)
              {
                 System.err.println("Invalid terminalProductionProbability option");
                 System.err.println(Usage);
                 System.exit(1);
              }
              continue;
           }
           if (args[i].equals("-numNonterminalDimensions"))
           {
              i++;
              if (i >= args.length)
              {
                 System.err.println("Invalid numNonterminalDimensions option");
                 System.err.println(Usage);
                 System.exit(1);
              }
              try
              {
                 NUM_NONTERMINAL_DIMENSIONS = Integer.parseInt(args[i]);
              }
              catch (NumberFormatException e) {
                 System.err.println("Invalid numNonterminalDimensions option");
                 System.err.println(Usage);
                 System.exit(1);
              }
              if (NUM_NONTERMINAL_DIMENSIONS < 1)
              {
                 System.err.println("Invalid numNonterminalDimensions option");
                 System.err.println(Usage);
                 System.exit(1);
              }              
              continue;
           }                     
           if (args[i].equals("-numNonterminalFeatures"))
           {
              i++;
              if (i >= args.length)
              {
                 System.err.println("Invalid numNonterminalFeatures option");
                 System.err.println(Usage);
                 System.exit(1);
              }
              try
              {
                 NUM_NONTERMINAL_FEATURES = Integer.parseInt(args[i]);
              }
              catch (NumberFormatException e) {
                 System.err.println("Invalid numNonterminalFeatures option");
                 System.err.println(Usage);
                 System.exit(1);
              }
              if (NUM_NONTERMINAL_FEATURES < 1)
              {
                 System.err.println("Invalid numNonterminalFeatures option");
                 System.err.println(Usage);
                 System.exit(1);
              }              
              continue;
           }                     
           if (args[i].equals("-numTerminalDimensions"))
           {
              i++;
              if (i >= args.length)
              {
                 System.err.println("Invalid numTerminalDimensions option");
                 System.err.println(Usage);
                 System.exit(1);
              }
              try
              {
                 NUM_TERMINAL_DIMENSIONS = Integer.parseInt(args[i]);
              }
              catch (NumberFormatException e) {
                 System.err.println("Invalid numTerminalDimensions option");
                 System.err.println(Usage);
                 System.exit(1);
              }
              if (NUM_TERMINAL_DIMENSIONS < 1)
              {
                 System.err.println("Invalid numTerminalDimensions option");
                 System.err.println(Usage);
                 System.exit(1);
              }              
              continue;
           }                     
           if (args[i].equals("-numTerminalFeatures"))
           {
              i++;
              if (i >= args.length)
              {
                 System.err.println("Invalid numTerminalFeatures option");
                 System.err.println(Usage);
                 System.exit(1);
              }
              try
              {
                 NUM_TERMINAL_FEATURES = Integer.parseInt(args[i]);
              }
              catch (NumberFormatException e) {
                 System.err.println("Invalid numTerminalFeatures option");
                 System.err.println(Usage);
                 System.exit(1);
              }
              if (NUM_TERMINAL_FEATURES < 1)
              {
                 System.err.println("Invalid numTerminalFeatures option");
                 System.err.println(Usage);
                 System.exit(1);
              }              
              continue;
           }                                 
           if (args[i].equals("-exportCausationsGraph"))
           {
              i++;
              if (i < args.length && !args[i].startsWith("-"))
              {
            	  CAUSATIONS_GRAPH_FILENAME = args[i];
              }
              gotExportCausationsGraph = true;
              continue;
           } 
           if (args[i].equals("-numCausationPaths"))
           {
              i++;
              if (i >= args.length)
              {
                 System.err.println("Invalid numCausationPaths option");
                 System.err.println(Usage);
                 System.exit(1);
              }
              try
              {
            	  NUM_CAUSATION_PATHS = Integer.parseInt(args[i]);
              }
              catch (NumberFormatException e) {
                 System.err.println("Invalid numCausationPaths option");
                 System.err.println(Usage);
                 System.exit(1);
              }
              if (NUM_CAUSATION_PATHS < 0)
              {
                 System.err.println("Invalid numCausationPaths option");
                 System.err.println(Usage);
                 System.exit(1);
              }              
              continue;
           }
           if (args[i].equals("-exportPathNNdataset"))
           {
        	  i++;
              if (i < args.length && !args[i].startsWith("-"))
              {
            	 PATH_NN_DATASET_FILENAME = args[i];
              }
              gotExportPathNNdataset = true;
              continue;
           }                   
           if (args[i].equals("-pathNNdatasetTrainFraction"))
           {
              i++;
              if (i >= args.length)
              {
                 System.err.println("Invalid pathNNdatasetTrainFraction option");
                 System.err.println(Usage);
                 System.exit(1);
              }
              try
              {
            	  PATH_NN_DATASET_TRAIN_FRACTION = Float.parseFloat(args[i]);
              }
              catch (NumberFormatException e) {
                 System.err.println("Invalid pathNNdatasetTrainFraction option");
                 System.err.println(Usage);
                 System.exit(1);
              }
              if (PATH_NN_DATASET_TRAIN_FRACTION < 0.0f || PATH_NN_DATASET_TRAIN_FRACTION > 1.0f)
              {
                 System.err.println("Invalid pathNNdatasetTrainFraction option");
                 System.err.println(Usage);
                 System.exit(1);
              }
          	  gotNNdatasetTrainFraction = true;              
              continue;
           }                  
           if (args[i].equals("-exportPathRNNdataset"))
           {
         	  i++;
              if (i < args.length && !args[i].startsWith("-"))
              {
            	 PATH_RNN_DATASET_FILENAME = args[i];
              }
              gotExportPathRNNdataset = true;              
              continue;
           }           
           if (args[i].equals("-pathRNNdatasetTrainFraction"))
           {
              i++;
              if (i >= args.length)
              {
                 System.err.println("Invalid pathRNNdatasetTrainFraction option");
                 System.err.println(Usage);
                 System.exit(1);
              }
              try
              {
            	  PATH_RNN_DATASET_TRAIN_FRACTION = Float.parseFloat(args[i]);
              }
              catch (NumberFormatException e) {
                 System.err.println("Invalid pathRNNdatasetTrainFraction option");
                 System.err.println(Usage);
                 System.exit(1);
              }
              if (PATH_RNN_DATASET_TRAIN_FRACTION < 0.0f || PATH_RNN_DATASET_TRAIN_FRACTION > 1.0f)
              {
                 System.err.println("Invalid pathRNNdatasetTrainFraction option");
                 System.err.println(Usage);
                 System.exit(1);
              }
          	  gotRNNdatasetTrainFraction = true;               
              continue;
           }
           if (args[i].equals("-exportPathTCNdataset"))
           {
         	  i++;
              if (i < args.length && !args[i].startsWith("-"))
              {
            	 PATH_RNN_DATASET_FILENAME = args[i];
              }
              gotExportPathTCNdataset = true;              
              continue;
           }           
           if (args[i].equals("-pathTCNdatasetTrainFraction"))
           {
              i++;
              if (i >= args.length)
              {
                 System.err.println("Invalid pathTCNdatasetTrainFraction option");
                 System.err.println(Usage);
                 System.exit(1);
              }
              try
              {
            	  PATH_TCN_DATASET_TRAIN_FRACTION = Float.parseFloat(args[i]);
              }
              catch (NumberFormatException e) {
                 System.err.println("Invalid pathTCNdatasetTrainFraction option");
                 System.err.println(Usage);
                 System.exit(1);
              }
              if (PATH_TCN_DATASET_TRAIN_FRACTION < 0.0f || PATH_TCN_DATASET_TRAIN_FRACTION > 1.0f)
              {
                 System.err.println("Invalid pathTCNdatasetTrainFraction option");
                 System.err.println(Usage);
                 System.exit(1);
              }
          	  gotTCNdatasetTrainFraction = true;               
              continue;
           }                          
           if (args[i].equals("-randomSeed"))
           {
              i++;
              if (i >= args.length)
              {
                 System.err.println("Invalid randomSeed option");
                 System.err.println(Usage);
                 System.exit(1);
              }
              try
              {
                 RANDOM_SEED = Integer.parseInt(args[i]);
              }
              catch (NumberFormatException e) {
                 System.err.println("Invalid randomSeed option");
                 System.err.println(Usage);
                 System.exit(1);
              }
              continue;
           }
           if (args[i].equals("-help"))
           {
                 System.out.println(Usage);
                 System.exit(0);
           }                        
           System.err.println("Invalid option: " + args[i]);
           System.err.println(Usage);
           System.exit(1);
        }
        
        // Validate options.              
        if (MIN_PRODUCTION_RHS_LENGTH > MAX_PRODUCTION_RHS_LENGTH)
        {
            System.err.println(Usage);
            System.exit(1);           	
        } 
        if (MIN_CAUSATION_PROBABILITY > MAX_CAUSATION_PROBABILITY)
        {
            System.err.println(Usage);
            System.exit(1);           	
        }
        if (NUM_NONTERMINAL_FEATURES > NUM_NONTERMINAL_DIMENSIONS)
        {
            System.err.println(Usage);
            System.exit(1);           	
        }
        if (NUM_TERMINAL_FEATURES > NUM_TERMINAL_DIMENSIONS)
        {
            System.err.println(Usage);
            System.exit(1);           	
        }         
        if (!gotExportPathNNdataset && gotNNdatasetTrainFraction)
    	{
            System.err.println(Usage);
            System.exit(1);    	
    	}        
        if (!gotExportPathRNNdataset && gotRNNdatasetTrainFraction)
    	{
            System.err.println(Usage);
            System.exit(1);    	
    	}
        if (!gotExportPathTCNdataset && gotTCNdatasetTrainFraction)
    	{
            System.err.println(Usage);
            System.exit(1);    	
    	}        
        
        // Initialize random numbers.
        randomizer = new Random(RANDOM_SEED);
        
        // Generate causations.
    	causations = new ArrayList<ArrayList<Causation>>();         
        for (int i = 0; i < NUM_CAUSATION_HIERARCHIES; i++)
        {
        	causations.add(new ArrayList<Causation>());
        	if (NUM_NONTERMINALS > 0)
        	{
        		Causation causation = new Causation();
        		causation.id = randomizer.nextInt(NUM_NONTERMINALS);
        		causation.features = encodeFeatures(causation.id, NUM_NONTERMINAL_DIMENSIONS, NUM_NONTERMINAL_FEATURES);
        		causation.terminal = false;
        		causation.parent = null;
        		causation.children = new ArrayList<Causation>();
        		causation.probabilities = new ArrayList<Float>();
        		causations.get(i).add(causation);
        		generateCausation(causation);
        	} else if (NUM_TERMINALS > 0)
        	{
        		Causation causation = new Causation();
        		causation.id = randomizer.nextInt(NUM_TERMINALS);        		
        		causation.features = encodeFeatures(causation.id, NUM_TERMINAL_DIMENSIONS, NUM_TERMINAL_FEATURES);
        		causation.terminal = true;
        		causation.parent = null;
        		causation.children = null;
        		causation.probabilities = null;
        		causations.get(i).add(causation);
        	}        	
        }

        // Export causations graph.
        if (gotExportCausationsGraph)
        {
        	exportCausationsGraph(CAUSATIONS_GRAPH_FILENAME);
        }
        
        // Produce causation paths.
        produceCausationPaths(NUM_CAUSATION_PATHS);
        
        // Export datasets.
        if (gotExportPathNNdataset)
        {
        	exportPathNNdataset(PATH_NN_DATASET_FILENAME, PATH_NN_DATASET_TRAIN_FRACTION);
        }        
        if (gotExportPathRNNdataset)
        {
        	exportPathDataset(PATH_RNN_DATASET_FILENAME, PATH_RNN_DATASET_TRAIN_FRACTION);
        }
        if (gotExportPathTCNdataset)
        {
        	exportPathDataset(PATH_TCN_DATASET_FILENAME, PATH_TCN_DATASET_TRAIN_FRACTION);
        }        
        
        System.exit(0);
    }
    
    // Encode sparse features.
	public static ArrayList<Boolean> encodeFeatures(int id, int numDimensions, int numFeatures)
	{
		Random r = new Random(id);
		ArrayList<Boolean> features = new ArrayList<Boolean>();
		for (int i = 0; i < numDimensions; i++)
		{
			features.add(false);
		}
		ArrayList<Integer> idxs = new ArrayList<Integer>();
		for (int i = 0; i < numFeatures; i++)
		{
			while(true)
			{
				int n = r.nextInt() % numDimensions;
				int j = 0;
				for (; j < idxs.size(); j++)
				{
					if (n == idxs.get(j))
					{
						break;
					}
				}
				if (j == idxs.size()) 
				{
					idxs.add(n);
					break;
				}
			}
		}
		for (int n : idxs)
		{
			features.set(n, true);
		}
		return features;		
	}
	
	// Generate causation.
	public static void generateCausation(Causation causation)
	{
		if (causation.terminal) 
		{
			return;
		}
		if (randomizer.nextFloat() < TERMINAL_PRODUCTION_PROBABILITY)
		{
			int n = randomizer.nextInt(MAX_PRODUCTION_RHS_LENGTH - MIN_PRODUCTION_RHS_LENGTH + 1) + MIN_PRODUCTION_RHS_LENGTH;
			for (int i = 0; i < n; i++)
			{
				Causation child = new Causation();
				child.id = randomizer.nextInt() % NUM_TERMINALS;
				child.features = encodeFeatures(child.id, NUM_TERMINAL_DIMENSIONS, NUM_TERMINAL_FEATURES);
				child.terminal = true;				
				child.parent = causation;
				child.children = null;
				child.probabilities = null;				
				causation.children.add(child);
				if (i < n - 1)
				{
					float p = MIN_CAUSATION_PROBABILITY + (randomizer.nextFloat() * (MAX_CAUSATION_PROBABILITY - MIN_CAUSATION_PROBABILITY));
					causation.probabilities.add(p);
				}
			}
		} else {
			
		}
	}
    
    // Export causations graph (Graphviz dot format).
    public static void exportCausationsGraph(String filename)
    {
    	if (grammar == null)
    	{
    		System.err.println("No grammar");
    		System.exit(1);
    	}    	    	
    	try
    	{
            FileWriter fileWriter = new FileWriter(filename);
            PrintWriter printWriter = new PrintWriter(fileWriter);
        	printWriter.println("digraph grammar {");
        	HashSet<String> visited = new HashSet<String>();
        	printNode("A", printWriter, visited);
        	printWriter.println("}");            
            printWriter.close();
    	} catch (IOException e)
    	{
    		System.err.println("Cannot save grammar graph to file " + filename);
    		System.exit(1);
    	}    	
    }
    
    // Print graph node recursively.
    static void printNode(String name, PrintWriter printWriter, HashSet<String> visited)
    {
    	if (!visited.contains(name))
    	{
    		visited.add(name);
	    	for (int i = 0; i < name.length(); i++)
	    	{
	    		String lhs = name.substring(i, i + 1);
		    	List<String> values = grammar.get(lhs);
		    	if (values != null)
		    	{
			        for (String rhs : values)
			        {
			        	printWriter.println(name + " -> " + rhs + " [label=\"" + lhs + "\"]");
			        	printNode(rhs, printWriter, visited);
			        }
		    	}
	    	}
    	}
    }
    
    // Produce causation paths.
    public static void produceCausationPaths(int numPaths)
    {
    	if (grammar == null)
    	{
    		System.err.println("No grammar");
    		System.exit(1);
    	}    	
    	worldPaths = new ArrayList<String>();
    	for (int pathnum = 0; pathnum < NUM_PATHS; pathnum++)
    	{
    		if (verbose) System.out.println("Path #" + pathnum + ":");
    		String path = new String(initialWorldPath);
	        if (verbose) System.out.println("Initial world path: " + path);
	        HashMap<String, String> expandedNonterminals = new HashMap<String, String>();
	        ArrayList<Pair<String, Integer>> open = new ArrayList<Pair<String, Integer>>();
        	char[] symbols = path.toCharArray();
        	for (int i = 0; i < symbols.length; i++)
        	{
        		String symbol = String.valueOf(symbols[i]);
        		if (grammar.containsKey(symbol))
        		{
        			open.add(new Pair<String, Integer>(symbol, 0));
        		}
        	}
	        while (open.size() > 0)
	        {	        			
	        	Pair<String, Integer> pair = open.get(0);
	        	String lhs = pair.getFirst();
	        	int pass = pair.getSecond();
	        	if (pass >= NUM_PATH_EXPANSIONS) break;
	        	open.remove(0);
	        	String rhs = "";
	        	if (expandedNonterminals.containsKey(lhs))
	        	{
	        		rhs = expandedNonterminals.get(lhs);
	        	} else {
		        	List<String> productions = grammar.get(lhs);
		        	int n = randomizer.nextInt(productions.size());
		        	rhs = productions.get(n);
	        		expandedNonterminals.put(lhs, rhs);		        	
	        	}
	        	int n = path.indexOf(lhs);
	        	path = path.substring(0, n) + rhs + path.substring(n + 1);
	        	if (verbose) System.out.println("Expansion: " + lhs + " ::= " + rhs + 
	        			", position " + n + ": " + path);
	        	symbols = rhs.toCharArray();
	        	for (int i = 0; i < symbols.length; i++)
	        	{
	        		String symbol = String.valueOf(symbols[i]);
	        		if (grammar.containsKey(symbol))
	        		{
	        			open.add(new Pair<String, Integer>(symbol, pass + 1));
	        		}
	        	}	        	
	        }
	        symbols = path.toCharArray();
	        path = "";
	    	for (int j = 0; j < symbols.length; j++)
	    	{
	    		path += Character.toLowerCase(symbols[j]);
	    	}
	    	worldPaths.add(path);	    	
	    	if (verbose) System.out.println("Final world path: " + path);
    	}
    }
    
    // Export path NN dataset.
    public static void exportPathNNdataset()
    {
    	exportPathNNdataset(PATH_NN_DATASET_FILENAME, PATH_NN_DATASET_TRAIN_FRACTION);
    }
    
    public static void exportPathNNdataset(String filename, float trainFraction)
    {
    	if (worldPaths == null)
    	{
    		System.err.println("No world paths");
    		System.exit(1);
    	}
    	try
    	{
            FileWriter fileWriter = new FileWriter(filename);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            int numPaths = worldPaths.size();
            int pathLength = 0;
            for (String s : worldPaths)
            {
            	if (pathLength < s.length())
            	{
            		pathLength = s.length();
            	}
            }
            int n = (int)((float)numPaths * trainFraction);
            printWriter.println("X_train_shape, " + (n * pathLength) + ", " + (26 * pathLength));
            for (int i = 0; i < n; i++)
            {
            	char[] terminals = worldPaths.get(i).toCharArray(); 
        		char[] terminalFrame = new char[pathLength];
        		for (int j = 0; j < pathLength; j++)
        		{
        			terminalFrame[j] = ' ';
        		}            	
            	for (int j = 0; j < pathLength; j++)
            	{
	            	for (int k = 0; k < terminals.length; k++)
	        		{
	            		int idx = k + pathLength - j - 1;
	            		if (idx < pathLength)
	            		{
	            			terminalFrame[idx] = terminals[k]; 
	            		}
	        		}
	            	for (int k = 0; k < pathLength; k++)
	        		{
	            		printWriter.print(oneHot(terminalFrame[k]));
	                	if (k < pathLength - 1)
	                	{
	                		printWriter.print(", ");
	                	}            		
	        		}
	            	printWriter.println();    	
            	}
            }
            printWriter.println("y_train_shape, " + (n * pathLength) + ", " + 26);
            for (int i = 0; i < n; i++)
            {
            	char[] terminals = worldPaths.get(i).toCharArray();
            	for (int j = 1; j < pathLength; j++)        		
            	{            		
            		if (j < terminals.length) 
            		{
            			printWriter.print(oneHot(terminals[j]));          			
            		} else {
            			printWriter.print(oneHot('g'));             			
            		}
                	printWriter.println();                	
        		}
    			printWriter.println(oneHot('g'));
            }
            printWriter.println("X_test_shape, " + ((numPaths - n) * pathLength) + ", " + (26 * pathLength));
            for (int i = n; i < numPaths; i++)
            {
            	char[] terminals = worldPaths.get(i).toCharArray(); 
        		char[] terminalFrame = new char[pathLength];
        		for (int j = 0; j < pathLength; j++)
        		{
        			terminalFrame[j] = ' ';
        		}            	
            	for (int j = 0; j < pathLength; j++)
            	{
	            	for (int k = 0; k < terminals.length; k++)
	        		{
	            		int idx = k + pathLength - j - 1;
	            		if (idx < pathLength)
	            		{
	            			terminalFrame[idx] = terminals[k]; 
	            		}
	        		}
	            	for (int k = 0; k < pathLength; k++)
	        		{
	            		printWriter.print(oneHot(terminalFrame[k]));
	                	if (k < pathLength - 1)
	                	{
	                		printWriter.print(", ");
	                	}            		
	        		}
	            	printWriter.println();         	
            	}
            }
            printWriter.println("y_test_shape, " + ((numPaths - n) * pathLength) + ", " + 26);
            for (int i = n; i < numPaths; i++)
            {
            	char[] terminals = worldPaths.get(i).toCharArray();
            	for (int j = 1; j < pathLength; j++)
        		{
            		if (j < terminals.length) 
            		{
            			printWriter.print(oneHot(terminals[j]));
            		} else {
            			printWriter.print(oneHot('g'));          			
            		}
                	printWriter.println();          		
        		}
    			printWriter.println(oneHot('g'));            	
            }               
            printWriter.close();
    	} catch (IOException e)
    	{
    		System.err.println("Cannot write path dataset to file " + filename);
    		System.exit(1);
    	}    	
    }
        
    // Export path RNN dataset.
    public static void exportPathRNNdataset()
    {
    	exportPathDataset(PATH_RNN_DATASET_FILENAME, PATH_RNN_DATASET_TRAIN_FRACTION);
    }
    
    // Export path TCN dataset.
    public static void exportPathTCNdataset()
    {
    	exportPathDataset(PATH_TCN_DATASET_FILENAME, PATH_TCN_DATASET_TRAIN_FRACTION);
    }
    
    public static void exportPathDataset(String filename, float trainFraction)
    {
    	if (worldPaths == null)
    	{
    		System.err.println("No world paths");
    		System.exit(1);
    	}
    	try
    	{
            FileWriter fileWriter = new FileWriter(filename);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            int numPaths = worldPaths.size();
            int pathLength = 0;
            for (String s : worldPaths)
            {
            	if (pathLength < s.length())
            	{
            		pathLength = s.length();
            	}
            }
            int n = (int)((float)numPaths * trainFraction);
            printWriter.println("X_train_shape = [ " + n + ", " + pathLength + ", " + 26 + " ]");
            printWriter.print("X_train_seq = [ ");
            for (int i = 0; i < n; i++)
            {
            	char[] terminals = worldPaths.get(i).toCharArray();
            	for (int j = 0; j < pathLength; j++)
        		{
            		if (j < terminals.length) 
            		{
            			printWriter.print(oneHot(terminals[j]));
            		} else {
            			printWriter.print(oneHot('g'));          			
            		}
                	if (j < pathLength - 1)
                	{
                		printWriter.print(", ");
                	}            		
        		}
            	if (i < n - 1)
            	{
            		printWriter.print(", ");
            	}
            }
            printWriter.println(" ]");
            printWriter.println("y_train_shape = [ " + n + ", " + pathLength + ", " + 26 + " ]");
            printWriter.print("y_train_seq = [ ");
            for (int i = 0; i < n; i++)
            {
            	char[] terminals = worldPaths.get(i).toCharArray();
            	for (int j = 1; j < pathLength; j++)
        		{
            		if (j < terminals.length) 
            		{
            			printWriter.print(oneHot(terminals[j]));
            		} else {
            			printWriter.print(oneHot('g'));          			
            		}
                	printWriter.print(", ");          		
        		}
    			printWriter.print(oneHot('g'));            	
            	if (i < n - 1)
            	{
            		printWriter.print(", ");
            	}
            }
            printWriter.println(" ]");
            printWriter.println("X_test_shape = [ " + (numPaths - n) + ", " + pathLength + ", " + 26 + " ]");
            printWriter.print("X_test_seq = [ ");
            for (int i = n; i < numPaths; i++)
            {
            	char[] terminals = worldPaths.get(i).toCharArray();
            	for (int j = 0; j < pathLength; j++)
        		{
            		if (j < terminals.length) 
            		{
            			printWriter.print(oneHot(terminals[j]));
            		} else {
            			printWriter.print(oneHot('g'));          			
            		}
                	if (j < pathLength - 1)
                	{
                		printWriter.print(", ");
                	}            		
        		}
            	if (i < numPaths - 1)
            	{
            		printWriter.print(", ");
            	}
            }
            printWriter.println(" ]");
            printWriter.println("y_test_shape = [ " + (numPaths - n) + ", " + pathLength + ", " + 26 + " ]");
            printWriter.print("y_test_seq = [ ");
            for (int i = n; i < numPaths; i++)
            {
            	char[] terminals = worldPaths.get(i).toCharArray();
            	for (int j = 1; j < pathLength; j++)
        		{
            		if (j < terminals.length) 
            		{
            			printWriter.print(oneHot(terminals[j]));
            		} else {
            			printWriter.print(oneHot('g'));          			
            		}
                	printWriter.print(", ");          		
        		}
    			printWriter.print(oneHot('g'));            	
            	if (i < numPaths - 1)
            	{
            		printWriter.print(", ");
            	}
            }
            printWriter.println(" ]");               
            printWriter.close();
    	} catch (IOException e)
    	{
    		System.err.println("Cannot write path dataset to file " + filename);
    		System.exit(1);
    	}    	
    }

    // One-hot coding of terminal.
    public String oneHot(char terminal)
    {
    	String code = "";
    	int t = -1;
    	if (terminal >= 'a')
    	{
    		t = terminal - 'a';
    	}
    	for (int i = 0; i < 26; i++)
    	{
    		if (i == t)
    		{
    			code += "1";
    		} else {
    			code += "0";
    		}
    		if (i < 25)
    		{
    			code += ", ";
    		}
    	}
    	return code;
    }
}
