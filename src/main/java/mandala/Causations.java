// For conditions of distribution and use, see copyright notice in Mandala.java

// World causation.
// Generate causation hierarchies and paths through the hierarchies.

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
   public static int   NUM_NONTERMINALS                = 10;
   public static int   NUM_TERMINALS                   = 10;
   public static int   MIN_PRODUCTION_RHS_LENGTH       = 2;
   public static int   MAX_PRODUCTION_RHS_LENGTH       = 5;
   public static float MIN_CAUSATION_PROBABILITY       = 0.1f;
   public static float MAX_CAUSATION_PROBABILITY       = 0.9f;
   public static float TERMINAL_PRODUCTION_PROBABILITY = 0.5f;

   // Feature dimensions.
   public static int NUM_NONTERMINAL_DIMENSIONS = 64;
   public static int NUM_NONTERMINAL_FEATURES   = 3;
   public static int NUM_TERMINAL_DIMENSIONS    = 16;
   public static int NUM_TERMINAL_FEATURES      = 3;

   // Causation.
   public static class Causation
   {
      public int                  id;
      public ArrayList<Boolean>   features;
      public boolean              terminal;
      public ArrayList<Causation> parents;
      public ArrayList<Causation> children;
      public ArrayList<Float>     probabilities;

      public Causation(int id, boolean terminal)
      {
         this.id       = id;
         this.terminal = terminal;
         parents       = new ArrayList<Causation>();
         if (terminal)
         {
            features      = encodeFeatures(id, NUM_TERMINAL_DIMENSIONS, NUM_TERMINAL_FEATURES);
            children      = null;
            probabilities = null;
         }
         else
         {
            features      = encodeFeatures(id, NUM_NONTERMINAL_DIMENSIONS, NUM_NONTERMINAL_FEATURES);
            children      = new ArrayList<Causation>();
            probabilities = new ArrayList<Float>();
         }
      }


      public void print()
      {
         System.out.print("id=" + id);
         System.out.print(", terminal=" + terminal);
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
   };
   public static ArrayList<Causation> causations;

   // Graph.
   public static String  CAUSATIONS_GRAPH_FILENAME = "causations.dot";
   public static boolean TREE_FORMAT = false;

   // Causation paths.
   public static int NUM_CAUSATION_PATHS = 5;
   public static ArrayList<Causation> causationPaths;

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
   public static boolean VERBOSE = true;

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
      "      [-numNonterminalDimensions <quantity> (default=" + NUM_NONTERMINAL_DIMENSIONS + ")]\n" +
      "      [-numNonterminalFeatures <quantity> (default=" + NUM_NONTERMINAL_FEATURES + ")]\n" +
      "      [-numTerminalDimensions <quantity> (default=" + NUM_TERMINAL_DIMENSIONS + ")]\n" +
      "      [-numTerminalFeatures <quantity> (default=" + NUM_TERMINAL_FEATURES + ")]\n" +
      "      [-exportCausationsGraph [<file name> (Graphviz dot format, default=" + CAUSATIONS_GRAPH_FILENAME + ")]\n" +
      "          [-treeFormat true | false (default=" + TREE_FORMAT + ")]]\n" +
      "      [-numCausationPaths <quantity> (default=" + NUM_CAUSATION_PATHS + ")]\n" +
      "      [-exportPathNNdataset [<file name (default=\"" + PATH_NN_DATASET_FILENAME + "\")>]\n" +
      "          [-NNdatasetTrainFraction <fraction> (default=" + PATH_NN_DATASET_TRAIN_FRACTION + ")]]\n" +
      "      [-exportPathRNNdataset [<file name (default=\"" + PATH_RNN_DATASET_FILENAME + "\")>]\n" +
      "          [-RNNdatasetTrainFraction <fraction> (default=" + PATH_RNN_DATASET_TRAIN_FRACTION + ")]]\n" +
      "      [-exportPathTCNdataset [<file name (default=\"" + PATH_TCN_DATASET_FILENAME + "\")>]\n" +
      "          [-TCNdatasetTrainFraction <fraction> (default=" + PATH_TCN_DATASET_TRAIN_FRACTION + ")]]\n" +
      "      [-randomSeed <seed> (default=" + RANDOM_SEED + ")]\n" +
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
      causations = new ArrayList<Causation>();
      for (int i = 0; i < NUM_CAUSATION_HIERARCHIES; i++)
      {
         if (NUM_NONTERMINALS > 0)
         {
            causations.add(generateCausationHierarchy());
         }
         else if (NUM_TERMINALS > 0)
         {
            causations.add(new Causation(0, true));
         }
      }

      // Export causations graph.
      if (gotExportCausationsGraph)
      {
         exportCausationsGraph(CAUSATIONS_GRAPH_FILENAME, TREE_FORMAT);
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


   // Encode features.
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


   // Generate causation hierarchy.
   public static Causation generateCausationHierarchy()
   {
      Causation root = new Causation(0, false);

      ArrayList<Causation> open = new ArrayList<Causation>();
      open.add(root);
      ArrayList<Causation> instances = new ArrayList<Causation>();
      instances.add(root);
      for (int i = 1; i < NUM_NONTERMINALS; i++)
      {
         instances.add(null);
      }
      Graph graph = new Graph(NUM_NONTERMINALS);
      expandNonterminal(open, instances, graph);
      return(root);
   }


   // Expand nonterminal causation.
   public static void expandNonterminal(ArrayList<Causation> open, ArrayList<Causation> instances, Graph graph)
   {
      int       n      = randomizer.nextInt(open.size());
      Causation parent = open.get(n);

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
            Causation child = null;
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
                     child = new Causation(k, false);
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
   public static void expandTerminal(Causation parent)
   {
      int n = randomizer.nextInt(MAX_PRODUCTION_RHS_LENGTH - MIN_PRODUCTION_RHS_LENGTH + 1) + MIN_PRODUCTION_RHS_LENGTH;

      for (int i = 0; i < n; i++)
      {
         Causation child = new Causation(randomizer.nextInt(NUM_TERMINALS), true);
         child.parents.add(parent);
         parent.children.add(child);
         if (i < n - 1)
         {
            float p = MIN_CAUSATION_PROBABILITY + (randomizer.nextFloat() * (MAX_CAUSATION_PROBABILITY - MIN_CAUSATION_PROBABILITY));
            parent.probabilities.add(p);
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
         for (int i = 0; i < causations.size(); i++)
         {
            Causation       root     = causations.get(i);
            HashSet<String> vertices = new HashSet<String>();
            vertices.add("h" + i + " [label=\"hierarchy_" + i + "\", shape=triangle];");
            HashSet<String> edges = new HashSet<String>();
            edges.add("hierarchies -> h" + i);
            if (root.terminal)
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
      if (vertex.terminal)
      {
         vertices.add("h" + hierarchy + "_t" + pathPrefix + vertex.id + " [label=\"" + vertex.id + "\", shape=square];");
      }
      else
      {
         vertices.add("h" + hierarchy + "_nt" + pathPrefix + vertex.id + " [label=\"" + vertex.id + "\", shape=circle];");
         for (int i = 0; i < vertex.children.size(); i++)
         {
            Causation child = vertex.children.get(i);
            String    p     = "";
            if (i < vertex.probabilities.size())
            {
               p = "/" + String.format("%.2f", vertex.probabilities.get(i));
            }
            String childPathPrefix = "";
            if (TREE_FORMAT)
            {
               childPathPrefix = new String(pathPrefix) + vertex.id + "_" + i + "_";
            }
            if (child.terminal)
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


   // Produce causation paths.
   public static void produceCausationPaths(int numPaths)
   {
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
