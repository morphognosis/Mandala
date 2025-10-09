// For conditions of distribution and use, see copyright notice in Mandala.java

// Generate causation hierarchies and learn paths through the hierarchies.

package mandala;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

public class Causations
{
   // Generation parameters.
   public static int   NUM_CAUSATION_HIERARCHIES       = 1;
   public static int   NUM_NONTERMINALS                = 5;
   public static int   NUM_TERMINALS                   = 10;
   public static int   MIN_PRODUCTION_RHS_LENGTH       = 2;
   public static int   MAX_PRODUCTION_RHS_LENGTH       = 5;
   public static float MIN_CAUSATION_PROBABILITY       = 0.1f;
   public static float MAX_CAUSATION_PROBABILITY       = 0.9f;
   public static float TERMINAL_PRODUCTION_PROBABILITY = 0.5f;

   // Feature dimensions.
   public static int NUM_DIMENSIONS = 16;
   public static int NUM_FEATURES   = 3;

   // Causation.
   public static class Causation
   {
      public int hierarchy;
      public int id;
      public ArrayList<NonterminalCausation> parents;

      public Causation(int hierarchy, int id)
      {
         this.hierarchy = hierarchy;
         this.id        = id;
         parents        = new ArrayList<NonterminalCausation>();
      }


      public void print()
      {
         System.out.print("hierarchy=" + hierarchy);
         System.out.print(", id=" + id);
         if (parents != null)
         {
            System.out.print(", parents:");
            for (Causation p : parents)
            {
               System.out.print(" " + p.id);
            }
         }
         System.out.println();
      }
   };

   // Terminal causation.
   public static class TerminalCausation extends Causation
   {
      public ArrayList<Boolean> features;

      public TerminalCausation(int hierarchy, int id)
      {
         super(hierarchy, id);
         features = encodeFeatures(hierarchy, id, NUM_DIMENSIONS, NUM_FEATURES);
      }


      // Encode features.
      public ArrayList<Boolean> encodeFeatures(int hierarchy, int id, int numDimensions, int numFeatures)
      {
         String seedString = hierarchy + "_" + id;
         long   seed       = seedString.hashCode();
         Random r          = new Random(seed);

         ArrayList<Boolean> features = new ArrayList<Boolean>();
         for (int i = 0; i < numDimensions; i++)
         {
            features.add(false);
         }
         ArrayList<Integer> idxs = new ArrayList<Integer>();
         for (int i = 0; i < numFeatures; i++)
         {
            while (true)
            {
               int n = r.nextInt(numDimensions);
               int j = 0;
               for ( ; j < idxs.size(); j++)
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
         return(features);
      }


      @Override
      public void print()
      {
         System.out.print("hierarchy=" + hierarchy);
         System.out.print(", id=" + id);
         System.out.print(", features:");
         for (int i = 0; i < features.size(); i++)
         {
            if (features.get(i))
            {
               System.out.print(" " + i);
            }
         }
         if (parents != null)
         {
            System.out.print(", parents:");
            for (Causation p : parents)
            {
               System.out.print(" " + p.id);
            }
         }
         System.out.println();
      }


      public void printHierarchical(String indent, String childNum, float probability)
      {
         System.out.print(indent);
         System.out.print("terminal id=" + id);
         if (childNum != null)
         {
            System.out.print(", child number=" + childNum);
         }
         if (probability >= 0.0f)
         {
            System.out.print(", probability=" + probability);
         }
         System.out.print(", features:");
         for (int i = 0; i < features.size(); i++)
         {
            if (features.get(i))
            {
               System.out.print(" " + i);
            }
         }
         System.out.println();
      }
   };

   // Nonterminal causation.
   public static class NonterminalCausation extends Causation
   {
      public ArrayList<Causation> children;
      public ArrayList<Float>     probabilities;

      public NonterminalCausation(int hierarchy, int id)
      {
         super(hierarchy, id);
         children      = new ArrayList<Causation>();
         probabilities = new ArrayList<Float>();
      }


      @Override
      public void print()
      {
         System.out.print("hierarchy=" + hierarchy);
         System.out.print(", id=" + id);
         if (parents != null)
         {
            System.out.print(", parents:");
            for (Causation p : parents)
            {
               System.out.print(" " + p.id);
            }
         }
         if (children != null)
         {
            System.out.print(", children:");
            for (Causation c : children)
            {
               System.out.print(" " + c.id);
            }
         }
         if (probabilities != null)
         {
            System.out.print(", probabilities:");
            for (float p : probabilities)
            {
               System.out.print(" " + p);
            }
         }
         System.out.println();
      }


      public void printHierarchical(String indent, String childNum, float probability, boolean recursive)
      {
         System.out.print(indent);
         System.out.print("nonterminal id=" + id);
         if (childNum != null)
         {
            System.out.print(", child number=" + childNum);
         }
         if (probability >= 0.0f)
         {
            System.out.print(", probability=" + probability);
         }
         System.out.println();
         if (recursive)
         {
            for (int i = 0, j = children.size(); i < j; i++)
            {
               float p = -1.0f;
               if (i < j - 1)
               {
                  p = probabilities.get(i);
               }
               Causation child = children.get(i);
               if (child instanceof TerminalCausation)
               {
                  TerminalCausation terminal = (TerminalCausation)child;
                  terminal.printHierarchical(indent + "  ", i + "", p);
               }
               else
               {
                  NonterminalCausation nonterminal = (NonterminalCausation)child;
                  nonterminal.printHierarchical(indent + "  ", i + "", p, true);
               }
            }
         }
      }
   };
   public static ArrayList < ArrayList < Causation >> causationHierarchies;

   // Graph.
   public static String  CAUSATIONS_GRAPH_FILENAME = "causations.dot";
   public static boolean TREE_FORMAT = true;

   // Causation paths.
   public static class CausationState
   {
      public Causation causation;
      public int       currentChild;

      public CausationState(Causation causation, int currentChild)
      {
         this.causation    = causation;
         this.currentChild = currentChild;
      }
   };
   public static int NUM_CAUSATION_PATHS = 5;
   public static     ArrayList < ArrayList < ArrayList < CausationState >>> causationPaths;

   // Dataset exports.
   public static String PATH_RNN_DATASET_FILENAME       = "causation_paths_rnn_dataset.py";
   public static float  PATH_RNN_DATASET_TRAIN_FRACTION = 0.75f;
   public static String PATH_TCN_DATASET_FILENAME       = "causation_paths_tcn_dataset.py";
   public static float  PATH_TCN_DATASET_TRAIN_FRACTION = 0.75f;
   public static String PATH_NN_DATASET_FILENAME        = "causation_paths_nn_dataset.csv";
   public static float  PATH_NN_DATASET_TRAIN_FRACTION  = 0.75f;

   // Random numbers.
   public static int    RANDOM_SEED = 4517;
   public static Random randomizer  = null;

   // Verbosity.
   public static boolean VERBOSE = false;

   // Usage.
   public static final String Usage =
      "Usage:\n" +
      "    java mandala.Causations\n" +
      "      [-numCausationHierarchies <quantity> (default=" + NUM_CAUSATION_HIERARCHIES + ")]\n" +
      "      [-numNonterminals <quantity> (default=" + NUM_NONTERMINALS + ")]\n" +
      "      [-numTerminals <quantity> (default=" + NUM_TERMINALS + ")]\n" +
      "      [-minProductionRightHandSideLength <quantity> (default=" + MIN_PRODUCTION_RHS_LENGTH + ")]\n" +
      "      [-maxProductionRightHandSideLength <quantity> (default=" + MAX_PRODUCTION_RHS_LENGTH + ")]\n" +
      "      [-minCausationProbability <probability> (default=" + MIN_CAUSATION_PROBABILITY + ")]\n" +
      "      [-maxCausationProbability <probability> (default=" + MAX_CAUSATION_PROBABILITY + ")]\n" +
      "      [-terminalProductionProbability <probability> (default=" + TERMINAL_PRODUCTION_PROBABILITY + ")]\n" +
      "      [-numDimensions <quantity> (default=" + NUM_DIMENSIONS + ")]\n" +
      "      [-numFeatures <quantity> (default=" + NUM_FEATURES + ")]\n" +
      "      [-exportCausationsGraph [<file name> (Graphviz dot format, default=" + CAUSATIONS_GRAPH_FILENAME + ")]\n" +
      "          [-treeFormat \"true\" | \"false\" (default=" + TREE_FORMAT + ")]]\n" +
      "      [-numCausationPaths <quantity> (default=" + NUM_CAUSATION_PATHS + ")]\n" +
      "      [-exportPathNNdataset [<file name> (default=\"" + PATH_NN_DATASET_FILENAME + "\")]\n" +
      "          [-NNdatasetTrainFraction <fraction> (default=" + PATH_NN_DATASET_TRAIN_FRACTION + ")]]\n" +
      "      [-exportPathRNNdataset [<file name> (default=\"" + PATH_RNN_DATASET_FILENAME + "\")]\n" +
      "          [-RNNdatasetTrainFraction <fraction> (default=" + PATH_RNN_DATASET_TRAIN_FRACTION + ")]]\n" +
      "      [-exportPathTCNdataset [<file name> (default=\"" + PATH_TCN_DATASET_FILENAME + "\")]\n" +
      "          [-TCNdatasetTrainFraction <fraction> (default=" + PATH_TCN_DATASET_TRAIN_FRACTION + ")]]\n" +
      "      [-randomSeed <seed> (default=" + RANDOM_SEED + ")]\n" +
      "      [-verbose]\n" +
      "  Help:\n" +
      "    java mandala.Causations -help\n" +
      "Exit codes:\n" +
      "  0=success\n" +
      "  1=error";

   // Main.
   public static void main(String[] args)
   {
      boolean gotExportCausationsGraph   = false;
      boolean gotTreeFormat              = false;
      boolean gotExportPathNNdataset     = false;
      boolean gotExportPathRNNdataset    = false;
      boolean gotExportPathTCNdataset    = false;
      boolean gotNNdatasetTrainFraction  = false;
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
         if (args[i].equals("-minProductionRightHandSideLength"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid minProductionRightHandSideLength option");
               System.err.println(Usage);
               System.exit(1);
            }
            try
            {
               MIN_PRODUCTION_RHS_LENGTH = Integer.parseInt(args[i]);
            }
            catch (NumberFormatException e) {
               System.err.println("Invalid minProductionRightHandSideLength option");
               System.err.println(Usage);
               System.exit(1);
            }
            if (MIN_PRODUCTION_RHS_LENGTH < 1)
            {
               System.err.println("Invalid minProductionRightHandSideLength option");
               System.err.println(Usage);
               System.exit(1);
            }
            continue;
         }
         if (args[i].equals("-maxProductionRightHandSideLength"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid maxProductionRightHandSideLength option");
               System.err.println(Usage);
               System.exit(1);
            }
            try
            {
               MAX_PRODUCTION_RHS_LENGTH = Integer.parseInt(args[i]);
            }
            catch (NumberFormatException e) {
               System.err.println("Invalid maxProductionRightHandSideLength option");
               System.err.println(Usage);
               System.exit(1);
            }
            if (MAX_PRODUCTION_RHS_LENGTH < 1)
            {
               System.err.println("Invalid maxProductionRightHandSideLength option");
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
            if ((TERMINAL_PRODUCTION_PROBABILITY < 0.0f) || (TERMINAL_PRODUCTION_PROBABILITY > 1.0f))
            {
               System.err.println("Invalid terminalProductionProbability option");
               System.err.println(Usage);
               System.exit(1);
            }
            continue;
         }
         if (args[i].equals("-numDimensions"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid numDimensions option");
               System.err.println(Usage);
               System.exit(1);
            }
            try
            {
               NUM_DIMENSIONS = Integer.parseInt(args[i]);
            }
            catch (NumberFormatException e) {
               System.err.println("Invalid numDimensions option");
               System.err.println(Usage);
               System.exit(1);
            }
            if (NUM_DIMENSIONS < 1)
            {
               System.err.println("Invalid numDimensions option");
               System.err.println(Usage);
               System.exit(1);
            }
            continue;
         }
         if (args[i].equals("-numFeatures"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid numFeatures option");
               System.err.println(Usage);
               System.exit(1);
            }
            try
            {
               NUM_FEATURES = Integer.parseInt(args[i]);
            }
            catch (NumberFormatException e) {
               System.err.println("Invalid numFeatures option");
               System.err.println(Usage);
               System.exit(1);
            }
            if (NUM_FEATURES < 1)
            {
               System.err.println("Invalid numFeatures option");
               System.err.println(Usage);
               System.exit(1);
            }
            continue;
         }
         if (args[i].equals("-exportCausationsGraph"))
         {
            if (((i + 1) < args.length) && !args[(i + 1)].startsWith("-"))
            {
               i++;
               CAUSATIONS_GRAPH_FILENAME = args[i];
            }
            gotExportCausationsGraph = true;
            continue;
         }
         if (args[i].equals("-treeFormat"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid treeFormat option");
               System.err.println(Usage);
               System.exit(1);
            }
            if (args[i].equals("true"))
            {
               TREE_FORMAT = true;
            }
            else if (args[i].equals("false"))
            {
               TREE_FORMAT = false;
            }
            else
            {
               System.err.println("Invalid treeFormat option");
               System.err.println(Usage);
               System.exit(1);
            }
            gotTreeFormat = true;
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
            if (((i + 1) < args.length) && !args[(i + 1)].startsWith("-"))
            {
               i++;
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
            if ((PATH_NN_DATASET_TRAIN_FRACTION < 0.0f) || (PATH_NN_DATASET_TRAIN_FRACTION > 1.0f))
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
            if (((i + 1) < args.length) && !args[(i + 1)].startsWith("-"))
            {
               i++;
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
            if ((PATH_RNN_DATASET_TRAIN_FRACTION < 0.0f) || (PATH_RNN_DATASET_TRAIN_FRACTION > 1.0f))
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
            if (((i + 1) < args.length) && !args[(i + 1)].startsWith("-"))
            {
               i++;
               PATH_TCN_DATASET_FILENAME = args[i];
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
            if ((PATH_TCN_DATASET_TRAIN_FRACTION < 0.0f) || (PATH_TCN_DATASET_TRAIN_FRACTION > 1.0f))
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
         if (args[i].equals("-verbose"))
         {
            VERBOSE = true;
            continue;
         }
         if (args[i].equals("-help") || args[i].equals("-h") || args[i].equals("-?"))
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
      if (NUM_FEATURES > NUM_DIMENSIONS)
      {
         System.err.println(Usage);
         System.exit(1);
      }
      if (!gotExportCausationsGraph && gotTreeFormat)
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
      causationHierarchies = new ArrayList < ArrayList < Causation >> ();
      for (int i = 0; i < NUM_CAUSATION_HIERARCHIES; i++)
      {
         ArrayList<Causation> causations = new ArrayList<Causation>();
         if (NUM_NONTERMINALS > 0)
         {
            ArrayList<NonterminalCausation> nonterminalCausations = generateCausationHierarchy(i);
            for (NonterminalCausation causation : nonterminalCausations)
            {
               if (causation != null)
               {
                  causations.add((Causation)causation);
               }
            }
         }
         else if (NUM_TERMINALS > 0)
         {
            causations.add(new TerminalCausation(i, 0));
         }
         causationHierarchies.add(causations);
      }

      // Print causations?
      if (VERBOSE)
      {
         printCausations();
      }

      // Export causations graph.
      if (gotExportCausationsGraph)
      {
         exportCausationsGraph(CAUSATIONS_GRAPH_FILENAME, TREE_FORMAT);
      }

      // Generate causation paths.
      generateCausationPaths(NUM_CAUSATION_PATHS);

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


   // Generate causation hierarchy.
   public static ArrayList<NonterminalCausation> generateCausationHierarchy(int hierarchy)
   {
      NonterminalCausation root = new NonterminalCausation(hierarchy, 0);

      ArrayList<NonterminalCausation> open = new ArrayList<NonterminalCausation>();
      open.add(root);
      ArrayList<NonterminalCausation> instances = new ArrayList<NonterminalCausation>();
      instances.add(root);
      for (int i = 1; i < NUM_NONTERMINALS; i++)
      {
         instances.add(null);
      }
      Graph graph = new Graph(NUM_NONTERMINALS);
      expandNonterminal(open, instances, graph);
      return(instances);
   }


   // Expand nonterminal causation.
   public static void expandNonterminal(ArrayList<NonterminalCausation> open, ArrayList<NonterminalCausation> instances, Graph graph)
   {
      int n = randomizer.nextInt(open.size());
      NonterminalCausation parent = open.get(n);

      open.remove(n);
      if (randomizer.nextFloat() < TERMINAL_PRODUCTION_PROBABILITY)
      {
         expandTerminal(parent);
      }
      else
      {
         n = randomizer.nextInt(MAX_PRODUCTION_RHS_LENGTH - MIN_PRODUCTION_RHS_LENGTH + 1) + MIN_PRODUCTION_RHS_LENGTH;
         for (int i = 0; i < n; i++)
         {
            ArrayList<Causation> newChildren = new ArrayList<Causation>();
            ArrayList<Integer>   available   = new ArrayList<Integer>();
            for (int j = 0; j < NUM_NONTERMINALS; j++)
            {
               available.add(j);
            }
            NonterminalCausation child = null;
            for (int j = 0; j < NUM_NONTERMINALS && child == null; j++)
            {
               int a = randomizer.nextInt(available.size());
               int k = available.get(a);
               graph.addEdge(parent.id, k);
               if (graph.hasCycle())
               {
                  graph.removeEdge(parent.id, k);
                  available.remove(a);
               }
               else
               {
                  child = instances.get(k);
                  if (child == null)
                  {
                     child = new NonterminalCausation(parent.hierarchy, k);
                     instances.set(k, child);
                     newChildren.add(child);
                     open.add(child);
                  }
                  child.parents.add(parent);
                  parent.children.add(child);
                  if (i < n - 1)
                  {
                     float p = MIN_CAUSATION_PROBABILITY + (randomizer.nextFloat() * (MAX_CAUSATION_PROBABILITY - MIN_CAUSATION_PROBABILITY));
                     parent.probabilities.add(p);
                  }
               }
            }
            if (child == null)
            {
               for (int j = 0, k = newChildren.size(); j < k; j++)
               {
                  Causation newChild = newChildren.get(j);
                  graph.removeEdge(parent.id, newChild.id);
                  instances.set(newChild.id, null);
                  open.remove(newChild);
               }
               parent.children      = new ArrayList<Causation>();
               parent.probabilities = new ArrayList<Float>();
               expandTerminal(parent);
               return;
            }
         }
      }
      if (open.size() > 0)
      {
         expandNonterminal(open, instances, graph);
      }
   }


   // Expand causation to terminals.
   public static void expandTerminal(NonterminalCausation parent)
   {
      int n = randomizer.nextInt(MAX_PRODUCTION_RHS_LENGTH - MIN_PRODUCTION_RHS_LENGTH + 1) + MIN_PRODUCTION_RHS_LENGTH;

      for (int i = 0; i < n; i++)
      {
         TerminalCausation child = new TerminalCausation(parent.hierarchy, randomizer.nextInt(NUM_TERMINALS));
         child.parents.add(parent);
         parent.children.add(child);
         if (i < n - 1)
         {
            float p = MIN_CAUSATION_PROBABILITY + (randomizer.nextFloat() * (MAX_CAUSATION_PROBABILITY - MIN_CAUSATION_PROBABILITY));
            parent.probabilities.add(p);
         }
      }
   }


   // Print causations.
   public static void printCausations()
   {
      System.out.println("hierarchies");
      for (int i = 0; i < causationHierarchies.size(); i++)
      {
         System.out.println("  hierarchy_" + i);
         Causation root = causationHierarchies.get(i).get(0);
         if (root != null)
         {
            if (root instanceof TerminalCausation)
            {
               TerminalCausation terminal = (TerminalCausation)root;
               terminal.printHierarchical("    ", null, -1.0f);
            }
            else
            {
               NonterminalCausation nonterminal = (NonterminalCausation)root;
               nonterminal.printHierarchical("    ", null, -1.0f, true);
            }
         }
      }
   }


   // Export causations graph (Graphviz dot format).
   public static void exportCausationsGraph(String filename, boolean treeFormat)
   {
      try
      {
         FileWriter  fileWriter  = new FileWriter(filename);
         PrintWriter printWriter = new PrintWriter(fileWriter);
         printWriter.println("digraph causations {");
         printWriter.println("hierarchies [label=\"hierarchies\", shape=triangle];");
         for (int i = 0; i < causationHierarchies.size(); i++)
         {
            Causation       root     = causationHierarchies.get(i).get(0);
            HashSet<String> vertices = new HashSet<String>();
            vertices.add("h" + i + " [label=\"hierarchy_" + i + "\", shape=triangle];");
            HashSet<String> edges = new HashSet<String>();
            edges.add("hierarchies -> h" + i);
            if (root instanceof TerminalCausation)
            {
               edges.add("h" + i + " -> h" + i + "_t" + root.id);
            }
            else
            {
               edges.add("h" + i + " -> h" + i + "_nt" + root.id);
            }
            listGraph(i, root, "", vertices, edges);
            for (String vertex : vertices)
            {
               printWriter.println(vertex);
            }
            for (String edge : edges)
            {
               printWriter.println(edge);
            }
         }
         printWriter.println("}");
         printWriter.close();
      }
      catch (IOException e)
      {
         System.err.println("Cannot save causations graph to file " + filename);
         System.exit(1);
      }
   }


   // List graph elements.
   private static void listGraph(int hierarchy, Causation vertex, String pathPrefix, HashSet<String> vertices, HashSet<String> edges)
   {
      if (vertex instanceof TerminalCausation)
      {
         TerminalCausation terminal = (TerminalCausation)vertex;
         String            s        = "(";
         for (int i = 0, j = terminal.features.size(); i < j; i++)
         {
            if (terminal.features.get(i))
            {
               s += i + ",";
            }
         }
         if (s.length() > 0)
         {
            s = s.substring(0, s.length() - 1);
         }
         s += ")";
         vertices.add("h" + hierarchy + "_t" + pathPrefix + vertex.id + " [label=\"" + vertex.id + s + "\", shape=square];");
      }
      else
      {
         vertices.add("h" + hierarchy + "_nt" + pathPrefix + vertex.id + " [label=\"" + vertex.id + "\", shape=circle];");
         NonterminalCausation nonTerminal = (NonterminalCausation)vertex;
         for (int i = 0; i < nonTerminal.children.size(); i++)
         {
            Causation child = nonTerminal.children.get(i);
            String    p     = "";
            if (i < nonTerminal.probabilities.size())
            {
               p = "(" + String.format("%.2f", nonTerminal.probabilities.get(i)) + ")";
            }
            String childPathPrefix = "";
            if (TREE_FORMAT)
            {
               childPathPrefix = new String(pathPrefix) + vertex.id + "_" + i + "_";
            }
            if (child instanceof TerminalCausation)
            {
               edges.add("h" + hierarchy + "_nt" + pathPrefix + vertex.id + " -> h" + hierarchy + "_t" + childPathPrefix + child.id + " [label=\"" + i + p + "\"];");
            }
            else
            {
               edges.add("h" + hierarchy + "_nt" + pathPrefix + vertex.id + " -> h" + hierarchy + "_nt" + childPathPrefix + child.id + " [label=\"" + i + p + "\"];");
            }
            listGraph(hierarchy, child, childPathPrefix, vertices, edges);
         }
      }
   }


   // Generate causation paths.
   public static void generateCausationPaths(int numPaths)
   {
      causationPaths = new ArrayList < ArrayList < ArrayList < CausationState >>> ();
      if (causationHierarchies.size() == 0)
      {
         return;
      }
      for (int i = 0; i < numPaths; i++)
      {
         ArrayList < ArrayList < CausationState >> path = new ArrayList < ArrayList < CausationState >> ();
         causationPaths.add(path);
         int hierarchy = randomizer.nextInt(causationHierarchies.size());
         ArrayList<Causation>      causationHierarchy = causationHierarchies.get(hierarchy);
         Causation                 root = causationHierarchy.get(randomizer.nextInt(causationHierarchy.size()));
         ArrayList<CausationState> step = new ArrayList<CausationState>();
         step.add(new CausationState(root, 0));
         path.add(step);
         while (root instanceof NonterminalCausation)
         {
            NonterminalCausation nonterminalRoot = (NonterminalCausation)root;
            root = nonterminalRoot.children.get(0);
            step.add(new CausationState(root, 0));
         }
         while (stepPath(path, 0)) {}
      }

      if (VERBOSE)
      {
         System.out.println("causation paths:");
         for (int i = 0; i < causationPaths.size(); i++)
         {
            ArrayList < ArrayList < CausationState >> path = causationPaths.get(i);
            System.out.println("path=" + i);
            for (int j = 0; j < path.size(); j++)
            {
               System.out.println("step=" + j);
               ArrayList<CausationState> step = path.get(j);
               for (int k = 0; k < step.size(); k++)
               {
                  CausationState state = step.get(k);
                  if (state.causation instanceof TerminalCausation)
                  {
                     ((TerminalCausation)state.causation).printHierarchical("", null, -1.0f);
                  }
                  else
                  {
                     NonterminalCausation nonterminalCausation = (NonterminalCausation)state.causation;
                     String               childInfo            = state.currentChild + "/" + nonterminalCausation.children.size();
                     float                probability          = -1.0f;
                     if (state.currentChild < nonterminalCausation.probabilities.size())
                     {
                        probability = nonterminalCausation.probabilities.get(state.currentChild);
                     }
                     nonterminalCausation.printHierarchical("", childInfo, probability, false);
                  }
               }
            }
         }
      }
   }


   // Step along path.
   public static boolean stepPath(ArrayList < ArrayList < CausationState >> path, int context)
   {
      ArrayList<CausationState> currentStep    = path.get(path.size() - 1);
      CausationState            causationState = currentStep.get(context);
      if (causationState.causation instanceof TerminalCausation)
      {
         return(false);
      }
      if (stepPath(path, context + 1))
      {
         return(true);
      }
      NonterminalCausation nonterminalCausation = (NonterminalCausation)causationState.causation;
      if (causationState.currentChild == nonterminalCausation.children.size() - 1)
      {
         return(false);
      }
      ArrayList<CausationState> nextStep = new ArrayList<CausationState>();
      for (int i = 0; i < context; i++)
      {
         CausationState state = currentStep.get(i);
         state = new CausationState(state.causation, state.currentChild);
         nextStep.add(state);
      }
      CausationState state = currentStep.get(context);
      state = new CausationState(state.causation, state.currentChild);
      state.currentChild++;
      nextStep.add(state);
      do
      {
         NonterminalCausation parent = (NonterminalCausation)state.causation;
         Causation            child  = parent.children.get(state.currentChild);
         state = new CausationState(child, 0);
         nextStep.add(state);
      } while (state.causation instanceof NonterminalCausation);
      path.add(nextStep);
      return(true);
   }


   // Export path NN dataset.
   public static void exportPathNNdataset()
   {
      exportPathNNdataset(PATH_NN_DATASET_FILENAME, PATH_NN_DATASET_TRAIN_FRACTION);
   }


   public static void exportPathNNdataset(String filename, float trainFraction)
   {
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
   }


   // One-hot coding of terminal.
   public String oneHot(char terminal)
   {
      String code = "";
      int    t    = -1;

      if (terminal >= 'a')
      {
         t = terminal - 'a';
      }
      for (int i = 0; i < 26; i++)
      {
         if (i == t)
         {
            code += "1";
         }
         else
         {
            code += "0";
         }
         if (i < 25)
         {
            code += ", ";
         }
      }
      return(code);
   }
}
