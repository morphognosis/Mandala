// For conditions of distribution and use, see copyright notice in Mandala.java

// Generate causation hierarchies and learn paths through the hierarchies.

package mandala;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import org.json.JSONException;
import org.json.JSONObject;

public class Causations
{
   // Generation parameters.
   public static int   NUM_CAUSATION_HIERARCHIES       = 1;
   public static int   NUM_NONTERMINALS                = 5;
   public static int   NUM_TERMINALS                   = 10;
   public static int   MIN_PRODUCTION_RHS_LENGTH       = 2;
   public static int   MAX_PRODUCTION_RHS_LENGTH       = 5;
   public static float MIN_CAUSATION_PROBABILITY       = 1.0f;
   public static float MAX_CAUSATION_PROBABILITY       = 1.0f;
   public static float TERMINAL_PRODUCTION_PROBABILITY = 0.5f;

   // Feature dimensions.
   public static int NUM_DIMENSIONS = 16;
   public static int NUM_FEATURES   = 3;

   // Causation.
   public static class Causation
   {
      public int                             hierarchy;
      public int                             id;
      public ArrayList<Boolean>              features;
      public ArrayList<NonterminalCausation> parents;

      public Causation(int hierarchy, int id)
      {
         this.hierarchy = hierarchy;
         this.id        = id;
         encodeFeatures();
         parents = new ArrayList<NonterminalCausation>();
      }


      // Encode features.
      public void encodeFeatures()
      {
         String seedString = hierarchy + "_" + id;
         long   seed       = seedString.hashCode();
         Random r          = new Random(seed);

         features = new ArrayList<Boolean>();
         for (int i = 0; i < NUM_DIMENSIONS; i++)
         {
            features.add(false);
         }
         ArrayList<Integer> idxs = new ArrayList<Integer>();
         for (int i = 0; i < NUM_FEATURES; i++)
         {
            while (true)
            {
               int n = r.nextInt(NUM_DIMENSIONS);
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
      }


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
         System.out.print(", parents:");
         for (Causation p : parents)
         {
            System.out.print(" " + p.id);
         }
         System.out.println();
      }
   };

   // Terminal causation.
   public static class TerminalCausation extends Causation
   {
      public TerminalCausation(int hierarchy, int id)
      {
         super(hierarchy, id);
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


      // Add child.
      public void addChild(Causation child)
      {
         children.add(child);
         if (children.size() > 1)
         {
            float p = MIN_CAUSATION_PROBABILITY + (randomizer.nextFloat() * (MAX_CAUSATION_PROBABILITY - MIN_CAUSATION_PROBABILITY));
            probabilities.add(p);
         }
      }


      @Override
      public void print()
      {
         System.out.print("hierarchy=" + hierarchy);
         System.out.print(", id=" + id);
         for (int i = 0; i < features.size(); i++)
         {
            if (features.get(i))
            {
               System.out.print(" " + i);
            }
         }
         System.out.print(", parents:");
         for (Causation p : parents)
         {
            System.out.print(" " + p.id);
         }
         System.out.print(", children:");
         for (Causation c : children)
         {
            System.out.print(" " + c.id);
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
      public boolean   valid;

      public CausationState(Causation causation, int currentChild)
      {
         this.causation    = causation;
         this.currentChild = currentChild;
         valid             = false;
      }


      public void print()
      {
         System.out.print("valid=" + valid + ", ");
         if (causation instanceof TerminalCausation)
         {
            ((TerminalCausation)causation).printHierarchical("", null, -1.0f);
         }
         else
         {
            NonterminalCausation nonterminalCausation = (NonterminalCausation)causation;
            String               childInfo            = currentChild + "/" + nonterminalCausation.children.size();
            float                probability          = -1.0f;
            if (currentChild < nonterminalCausation.probabilities.size())
            {
               probability = nonterminalCausation.probabilities.get(currentChild);
            }
            nonterminalCausation.printHierarchical("", childInfo, probability, false);
         }
      }
   };
   public static int NUM_CAUSATION_PATHS = 20;
   public static     ArrayList < ArrayList < ArrayList < CausationState >>> causationPaths;

   // Datasets.
   public static String NN_DATASET_FILENAME        = "causations_nn_dataset.py";
   public static float  NN_DATASET_TRAIN_FRACTION  = 0.75f;
   public static String RNN_DATASET_FILENAME       = "causations_rnn_dataset.py";
   public static float  RNN_DATASET_TRAIN_FRACTION = 0.75f;
   public static String TCN_DATASET_FILENAME       = "causations_tcn_dataset.py";
   public static float  TCN_DATASET_TRAIN_FRACTION = 0.75f;

   // Learners.
   public static String NN_FILENAME         = "causations_nn.py";
   public static String NN_RESULTS_FILENAME = "causations_nn_results.json";
   public static String NN_HIDDEN           = "128,128,128";
   public static int    NN_EPOCHS           = 500;
   public static String RNN_FILENAME        = "causations_rnn.py";
   public static String TCN_FILENAME        = "causations_tcn.py";

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
      "      [-NNdatasetTrainFraction <fraction> (default=" + NN_DATASET_TRAIN_FRACTION + ")]\n" +
      "      [-NNresultsFilename <file name> (default=\"" + NN_RESULTS_FILENAME + "\")]\n" +
      "      [-NNnumHidden <number of hidden neurons> (comma-separated for additional layers) (default=" + NN_HIDDEN + ")]\n" +
      "      [-NNnumEpochs <number of epochs> (default=" + NN_EPOCHS + ")]\n" +
      "      [-RNNdatasetTrainFraction <fraction> (default=" + RNN_DATASET_TRAIN_FRACTION + ")]\n" +
      "      [-TCNdatasetTrainFraction <fraction> (default=" + TCN_DATASET_TRAIN_FRACTION + ")]\n" +
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
      boolean gotExportCausationsGraph = false;
      boolean gotTreeFormat            = false;

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
            if (NUM_TERMINALS < 1)
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
         if (args[i].equals("-NNdatasetTrainFraction"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid NNdatasetTrainFraction option");
               System.err.println(Usage);
               System.exit(1);
            }
            try
            {
               NN_DATASET_TRAIN_FRACTION = Float.parseFloat(args[i]);
            }
            catch (NumberFormatException e) {
               System.err.println("Invalid NNdatasetTrainFraction option");
               System.err.println(Usage);
               System.exit(1);
            }
            if ((NN_DATASET_TRAIN_FRACTION < 0.0f) || (NN_DATASET_TRAIN_FRACTION > 1.0f))
            {
               System.err.println("Invalid NNdatasetTrainFraction option");
               System.err.println(Usage);
               System.exit(1);
            }
            continue;
         }
         if (args[i].equals("-NNresultsFilename"))
         {
            if (((i + 1) < args.length) && !args[(i + 1)].startsWith("-"))
            {
               i++;
               NN_RESULTS_FILENAME = args[i];
            }
            continue;
         }
         if (args[i].equals("-NNnumHidden"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid NNnumHidden option");
               System.err.println(Usage);
               System.exit(1);
            }
            NN_HIDDEN = args[i].replaceAll("\\s", "");
            if (NN_HIDDEN.isEmpty())
            {
               System.err.println("Invalid NNnumHidden option");
               System.err.println(Usage);
               System.exit(1);
            }
            continue;
         }
         if (args[i].equals("-NNnumEpochs"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid NNnumEpochs option");
               System.err.println(Usage);
               System.exit(1);
            }
            try
            {
               NN_EPOCHS = Integer.parseInt(args[i]);
            }
            catch (NumberFormatException e) {
               System.err.println("Invalid NNnumEpochs option");
               System.err.println(Usage);
               System.exit(1);
            }
            if (NN_EPOCHS < 0)
            {
               System.err.println("Invalid NNnumEpochs option");
               System.err.println(Usage);
               System.exit(1);
            }
            continue;
         }
         if (args[i].equals("-RNNdatasetTrainFraction"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid RNNdatasetTrainFraction option");
               System.err.println(Usage);
               System.exit(1);
            }
            try
            {
               RNN_DATASET_TRAIN_FRACTION = Float.parseFloat(args[i]);
            }
            catch (NumberFormatException e) {
               System.err.println("Invalid RNNdatasetTrainFraction option");
               System.err.println(Usage);
               System.exit(1);
            }
            if ((RNN_DATASET_TRAIN_FRACTION < 0.0f) || (RNN_DATASET_TRAIN_FRACTION > 1.0f))
            {
               System.err.println("Invalid RNNdatasetTrainFraction option");
               System.err.println(Usage);
               System.exit(1);
            }
            continue;
         }
         if (args[i].equals("-TCNdatasetTrainFraction"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid TCNdatasetTrainFraction option");
               System.err.println(Usage);
               System.exit(1);
            }
            try
            {
               TCN_DATASET_TRAIN_FRACTION = Float.parseFloat(args[i]);
            }
            catch (NumberFormatException e) {
               System.err.println("Invalid TCNdatasetTrainFraction option");
               System.err.println(Usage);
               System.exit(1);
            }
            if ((TCN_DATASET_TRAIN_FRACTION < 0.0f) || (TCN_DATASET_TRAIN_FRACTION > 1.0f))
            {
               System.err.println("Invalid TCNdatasetTrainFraction option");
               System.err.println(Usage);
               System.exit(1);
            }
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


      // Initialize random numbers.
      randomizer = new Random(RANDOM_SEED);

      // Generate causations.
      causationHierarchies = new ArrayList < ArrayList < Causation >> ();
      for (int i = 0; i < NUM_CAUSATION_HIERARCHIES; i++)
      {
         ArrayList<Causation> causations = new ArrayList<Causation>();
         if (NUM_NONTERMINALS > 0)
         {
            ArrayList<NonterminalCausation> nonterminalInstances = generateCausationHierarchy(i);
            for (NonterminalCausation causation : nonterminalInstances)
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

      // Export causation datasets.
      exportNNdataset(NN_DATASET_FILENAME, NN_DATASET_TRAIN_FRACTION);
      exportDataset(RNN_DATASET_FILENAME, RNN_DATASET_TRAIN_FRACTION);
      exportDataset(TCN_DATASET_FILENAME, TCN_DATASET_TRAIN_FRACTION);

      // Learn causations.
      learnCausationsNN(NN_DATASET_FILENAME);
      learnCausationsRNN(RNN_DATASET_FILENAME);
      learnCausationsTCN(TCN_DATASET_FILENAME);

      System.exit(0);
   }


   // Generate causation hierarchy.
   public static ArrayList<NonterminalCausation> generateCausationHierarchy(int hierarchy)
   {
      NonterminalCausation root = new NonterminalCausation(hierarchy, 0);

      ArrayList<NonterminalCausation> open = new ArrayList<NonterminalCausation>();
      open.add(root);
      ArrayList<NonterminalCausation> nonterminalInstances = new ArrayList<NonterminalCausation>();
      nonterminalInstances.add(root);
      for (int i = 1; i < NUM_NONTERMINALS; i++)
      {
         nonterminalInstances.add(null);
      }
      ArrayList<TerminalCausation> terminalInstances = new ArrayList<TerminalCausation>();
      for (int i = 0; i < NUM_TERMINALS; i++)
      {
         terminalInstances.add(null);
      }
      Graph graph = new Graph(NUM_NONTERMINALS);
      expandNonterminal(open, graph, nonterminalInstances, terminalInstances);
      return(nonterminalInstances);
   }


   // Expand nonterminal causation.
   public static void expandNonterminal(ArrayList<NonterminalCausation> open, Graph graph,
                                        ArrayList<NonterminalCausation> nonterminalInstances, ArrayList<TerminalCausation> terminalInstances)
   {
      int n = randomizer.nextInt(open.size());
      NonterminalCausation parent = open.get(n);

      open.remove(n);
      if (randomizer.nextFloat() < TERMINAL_PRODUCTION_PROBABILITY)
      {
         expandTerminal(parent, terminalInstances);
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
                  child = nonterminalInstances.get(k);
                  if (child == null)
                  {
                     child = new NonterminalCausation(parent.hierarchy, k);
                     nonterminalInstances.set(k, child);
                     newChildren.add(child);
                     open.add(child);
                  }
                  child.parents.add(parent);
                  parent.addChild(child);
               }
            }
            if (child == null)
            {
               for (int j = 0, k = newChildren.size(); j < k; j++)
               {
                  Causation newChild = newChildren.get(j);
                  graph.removeEdge(parent.id, newChild.id);
                  nonterminalInstances.set(newChild.id, null);
                  open.remove(newChild);
               }
               parent.children      = new ArrayList<Causation>();
               parent.probabilities = new ArrayList<Float>();
               expandTerminal(parent, terminalInstances);
               return;
            }
         }
      }
      if (open.size() > 0)
      {
         expandNonterminal(open, graph, nonterminalInstances, terminalInstances);
      }
   }


   // Expand causation to terminals.
   public static void expandTerminal(NonterminalCausation parent, ArrayList<TerminalCausation> terminalInstances)
   {
      int n = randomizer.nextInt(MAX_PRODUCTION_RHS_LENGTH - MIN_PRODUCTION_RHS_LENGTH + 1) + MIN_PRODUCTION_RHS_LENGTH;

      for (int i = 0; i < n; i++)
      {
         int               j     = randomizer.nextInt(NUM_TERMINALS);
         TerminalCausation child = terminalInstances.get(j);
         if (child == null)
         {
            child = new TerminalCausation(parent.hierarchy, j);
            terminalInstances.set(j, child);
         }
         child.parents.add(parent);
         parent.addChild(child);
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
      // Generate causation hierarchy paths.
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
         while (stepCausationPath(path, 0)) {}
         for (ArrayList<CausationState> s : path)
         {
            Collections.reverse(s);
         }
      }

      // Probabilistically validate causation paths.
      for (int i = 0; i < numPaths; i++)
      {
         ArrayList < ArrayList < CausationState >> path = causationPaths.get(i);
         ArrayList<CausationState> step  = path.get(0);
         CausationState            state = step.get(0);
         state.valid = true;
         boolean pathValid = true;
         for (int j = 1; j < path.size() && pathValid; j++)
         {
            step      = path.get(j);
            pathValid = false;
            for (int k = 1; k < step.size(); k++)
            {
               state = step.get(k);
               NonterminalCausation nonterminalCausation = (NonterminalCausation)state.causation;
               if (state.currentChild > 0)
               {
                  float probability = nonterminalCausation.probabilities.get(state.currentChild - 1);
                  if (randomizer.nextFloat() < probability)
                  {
                     pathValid = true;
                  }
                  break;
               }
            }
            if (pathValid)
            {
               boolean stepValid = false;
               for (int k = step.size() - 1; k >= 0; k--)
               {
                  state = step.get(k);
                  if (state.currentChild > 0)
                  {
                     stepValid = true;
                  }
                  state.valid = stepValid;
               }
            }
         }
      }

      if (VERBOSE)
      {
         System.out.println("causation paths:");
         for (int i = 0; i < numPaths; i++)
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
                  state.print();
               }
            }
         }
      }
   }


   // Step along causation path.
   public static boolean stepCausationPath(ArrayList < ArrayList < CausationState >> path, int context)
   {
      ArrayList<CausationState> currentStep    = path.get(path.size() - 1);
      CausationState            causationState = currentStep.get(context);
      if (causationState.causation instanceof TerminalCausation)
      {
         return(false);
      }
      if (stepCausationPath(path, context + 1))
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


   // Export NN dataset.
   public static void exportNNdataset()
   {
      exportNNdataset(NN_DATASET_FILENAME, NN_DATASET_TRAIN_FRACTION);
   }


   public static void exportNNdataset(String filename, float trainFraction)
   {
      int numPaths      = causationPaths.size();
      int maxPathLength = 0;
      int maxStepSize   = 0;

      for (int i = 0; i < numPaths; i++)
      {
         ArrayList < ArrayList < CausationState >> path = causationPaths.get(i);
         if (path.size() > maxPathLength)
         {
            maxPathLength = path.size();
         }
         for (int j = 0; j < path.size(); j++)
         {
            ArrayList<CausationState> step = path.get(j);
            if (step.size() > maxStepSize)
            {
               maxStepSize = step.size();
            }
         }
      }

      try
      {
         FileWriter  fileWriter  = new FileWriter(filename);
         PrintWriter printWriter = new PrintWriter(fileWriter);
         int         numTrain    = (int)((float)numPaths * NN_DATASET_TRAIN_FRACTION);
         int         stateSize   = NUM_DIMENSIONS + MAX_PRODUCTION_RHS_LENGTH;
         printWriter.println("X_train_shape = [ " + (numTrain * (maxPathLength - 1)) + ", " + (maxStepSize * stateSize) + " ]");
         printWriter.print("X_train = [ ");
         String X_train = "";
         for (int i = 0; i < numTrain; i++)
         {
            ArrayList < ArrayList < CausationState >> path = causationPaths.get(i);
            int p = path.size() - 1;
            for (int j = 0; j < p; j++)
            {
               ArrayList<CausationState> step = path.get(j);
               int s = step.size();
               int k = 0;
               for ( ; k < s; k++)
               {
                  CausationState state = step.get(k);
                  if (!state.valid) { break; }
                  ArrayList<Boolean> features = null;
                  features = state.causation.features;
                  for (int q = 0; q < NUM_DIMENSIONS; q++)
                  {
                     if (features.get(q))
                     {
                        X_train += "1,";
                     }
                     else
                     {
                        X_train += "0,";
                     }
                  }
                  if (state.causation instanceof TerminalCausation)
                  {
                     for (int q = 0; q < MAX_PRODUCTION_RHS_LENGTH; q++)
                     {
                        X_train += "0,";
                     }
                  }
                  else
                  {
                     for (int q = 0; q < MAX_PRODUCTION_RHS_LENGTH; q++)
                     {
                        if (q == state.currentChild)
                        {
                           X_train += "1,";
                        }
                        else
                        {
                           X_train += "0,";
                        }
                     }
                  }
               }
               s = maxStepSize - k;
               for (k = 0; k < s; k++)
               {
                  for (int q = 0; q < NUM_DIMENSIONS; q++)
                  {
                     X_train += "0,";
                  }
                  for (int q = 0; q < MAX_PRODUCTION_RHS_LENGTH; q++)
                  {
                     X_train += "0,";
                  }
               }
               X_train += "\n";
            }
            p = (maxPathLength - 1) - p;
            for (int j = 0; j < p; j++)
            {
               for (int k = 0; k < maxStepSize; k++)
               {
                  for (int q = 0; q < NUM_DIMENSIONS; q++)
                  {
                     X_train += "0,";
                  }
                  for (int q = 0; q < MAX_PRODUCTION_RHS_LENGTH; q++)
                  {
                     X_train += "0,";
                  }
               }
               X_train += "\n";
            }
         }
         if (X_train.endsWith(","))
         {
            X_train = X_train.substring(0, X_train.length() - 1);
         }
         printWriter.println(X_train + " ]");
         printWriter.println("y_train_shape = [ " + (numTrain * (maxPathLength - 1)) + ", " + (maxStepSize * stateSize) + " ]");
         printWriter.print("y_train = [ ");
         String y_train = "";
         for (int i = 0; i < numTrain; i++)
         {
            ArrayList < ArrayList < CausationState >> path = causationPaths.get(i);
            int p = path.size();
            for (int j = 1; j < p; j++)
            {
               ArrayList<CausationState> step = path.get(j);
               int s = step.size();
               int k = 0;
               for ( ; k < s; k++)
               {
                  CausationState state = step.get(k);
                  if (!state.valid) { break; }
                  ArrayList<Boolean> features = null;
                  features = state.causation.features;
                  for (int q = 0; q < NUM_DIMENSIONS; q++)
                  {
                     if (features.get(q))
                     {
                        y_train += "1,";
                     }
                     else
                     {
                        y_train += "0,";
                     }
                  }
                  if (state.causation instanceof TerminalCausation)
                  {
                     for (int q = 0; q < MAX_PRODUCTION_RHS_LENGTH; q++)
                     {
                        y_train += "0,";
                     }
                  }
                  else
                  {
                     for (int q = 0; q < MAX_PRODUCTION_RHS_LENGTH; q++)
                     {
                        if (q == state.currentChild)
                        {
                           y_train += "1,";
                        }
                        else
                        {
                           y_train += "0,";
                        }
                     }
                  }
               }
               s = maxStepSize - k;
               for (k = 0; k < s; k++)
               {
                  for (int q = 0; q < NUM_DIMENSIONS; q++)
                  {
                     y_train += "0,";
                  }
                  for (int q = 0; q < MAX_PRODUCTION_RHS_LENGTH; q++)
                  {
                     y_train += "0,";
                  }
               }
               y_train += "\n";
            }
            p = maxPathLength - p;
            for (int j = 0; j < p; j++)
            {
               for (int k = 0; k < maxStepSize; k++)
               {
                  for (int q = 0; q < NUM_DIMENSIONS; q++)
                  {
                     y_train += "0,";
                  }
                  for (int q = 0; q < MAX_PRODUCTION_RHS_LENGTH; q++)
                  {
                     y_train += "0,";
                  }
               }
               y_train += "\n";
            }
         }
         if (y_train.endsWith(","))
         {
            y_train = y_train.substring(0, y_train.length() - 1);
         }
         printWriter.println(y_train + " ]");
         int numTest = numPaths - numTrain;
         printWriter.println("X_test_shape = [ " + (numTest * (maxPathLength - 1)) + ", " + (maxStepSize * stateSize) + " ]");
         printWriter.print("X_test = [ ");
         String X_test = "";
         for (int i = numTrain; i < numPaths; i++)
         {
            ArrayList < ArrayList < CausationState >> path = causationPaths.get(i);
            int p = path.size() - 1;
            for (int j = 0; j < p; j++)
            {
               ArrayList<CausationState> step = path.get(j);
               int s = step.size();
               int k = 0;
               for ( ; k < s; k++)
               {
                  CausationState state = step.get(k);
                  if (!state.valid) { break; }
                  ArrayList<Boolean> features = null;
                  features = state.causation.features;
                  for (int q = 0; q < NUM_DIMENSIONS; q++)
                  {
                     if (features.get(q))
                     {
                        X_test += "1,";
                     }
                     else
                     {
                        X_test += "0,";
                     }
                  }
                  if (state.causation instanceof TerminalCausation)
                  {
                     for (int q = 0; q < MAX_PRODUCTION_RHS_LENGTH; q++)
                     {
                        X_test += "0,";
                     }
                  }
                  else
                  {
                     for (int q = 0; q < MAX_PRODUCTION_RHS_LENGTH; q++)
                     {
                        if (q == state.currentChild)
                        {
                           X_test += "1,";
                        }
                        else
                        {
                           X_test += "0,";
                        }
                     }
                  }
               }
               s = maxStepSize - k;
               for (k = 0; k < s; k++)
               {
                  for (int q = 0; q < NUM_DIMENSIONS; q++)
                  {
                     X_test += "0,";
                  }
                  for (int q = 0; q < MAX_PRODUCTION_RHS_LENGTH; q++)
                  {
                     X_test += "0,";
                  }
               }
               X_test += "\n";
            }
            p = (maxPathLength - 1) - p;
            for (int j = 0; j < p; j++)
            {
               for (int k = 0; k < maxStepSize; k++)
               {
                  for (int q = 0; q < NUM_DIMENSIONS; q++)
                  {
                     X_test += "0,";
                  }
                  for (int q = 0; q < MAX_PRODUCTION_RHS_LENGTH; q++)
                  {
                     X_test += "0,";
                  }
               }
               X_test += "\n";
            }
         }
         if (X_test.endsWith(","))
         {
            X_test = X_test.substring(0, X_test.length() - 1);
         }
         printWriter.println(X_test + " ]");
         printWriter.println("y_test_shape = [ " + (numTest * (maxPathLength - 1)) + ", " + (maxStepSize * stateSize) + " ]");
         printWriter.print("y_test = [ ");
         String y_test = "";
         for (int i = numTrain; i < numPaths; i++)
         {
            ArrayList < ArrayList < CausationState >> path = causationPaths.get(i);
            int p = path.size();
            for (int j = 1; j < p; j++)
            {
               ArrayList<CausationState> step = path.get(j);
               int s = step.size();
               int k = 0;
               for ( ; k < s; k++)
               {
                  CausationState state = step.get(k);
                  if (!state.valid) { break; }
                  ArrayList<Boolean> features = null;
                  features = state.causation.features;
                  for (int q = 0; q < NUM_DIMENSIONS; q++)
                  {
                     if (features.get(q))
                     {
                        y_test += "1,";
                     }
                     else
                     {
                        y_test += "0,";
                     }
                  }
                  if (state.causation instanceof TerminalCausation)
                  {
                     for (int q = 0; q < MAX_PRODUCTION_RHS_LENGTH; q++)
                     {
                        y_test += "0,";
                     }
                  }
                  else
                  {
                     for (int q = 0; q < MAX_PRODUCTION_RHS_LENGTH; q++)
                     {
                        if (q == state.currentChild)
                        {
                           y_test += "1,";
                        }
                        else
                        {
                           y_test += "0,";
                        }
                     }
                  }
               }
               s = maxStepSize - k;
               for (k = 0; k < s; k++)
               {
                  for (int q = 0; q < NUM_DIMENSIONS; q++)
                  {
                     y_test += "0,";
                  }
                  for (int q = 0; q < MAX_PRODUCTION_RHS_LENGTH; q++)
                  {
                     y_test += "0,";
                  }
               }
               y_test += "\n";
            }
            p = maxPathLength - p;
            for (int j = 0; j < p; j++)
            {
               for (int k = 0; k < maxStepSize; k++)
               {
                  for (int q = 0; q < NUM_DIMENSIONS; q++)
                  {
                     y_test += "0,";
                  }
                  for (int q = 0; q < MAX_PRODUCTION_RHS_LENGTH; q++)
                  {
                     y_test += "0,";
                  }
               }
               y_test += "\n";
            }
         }
         if (y_test.endsWith(","))
         {
            y_test = y_test.substring(0, y_test.length() - 1);
         }
         printWriter.println(y_test + " ]");
         printWriter.close();
      }
      catch (IOException e)
      {
         System.err.println("Cannot write NN dataset to file " + filename);
         System.exit(1);
      }
   }


   // Export RNN dataset.
   public static void exportRNNdataset()
   {
      exportDataset(RNN_DATASET_FILENAME, RNN_DATASET_TRAIN_FRACTION);
   }


   // Export TCN dataset.
   public static void exportTCNdataset()
   {
      exportDataset(TCN_DATASET_FILENAME, TCN_DATASET_TRAIN_FRACTION);
   }


   public static void exportDataset(String filename, float trainFraction)
   {
   }


   // Learn causations with NN.
   public static void learnCausationsNN(String filename)
   {
      if (VERBOSE)
      {
         System.out.println("Learn NN");
      }
      try
      {
         InputStream in = ClassLoader.getSystemClassLoader().getResourceAsStream(NN_FILENAME);
         if (in == null)
         {
            System.err.println("Cannot access " + NN_FILENAME);
            System.exit(1);
         }
         File             pythonScript = new File(NN_FILENAME);
         FileOutputStream out          = new FileOutputStream(pythonScript);
         byte[] buffer = new byte[1024];
         int bytesRead;
         while ((bytesRead = in.read(buffer)) != -1)
         {
            out.write(buffer, 0, bytesRead);
         }
         out.close();
      }
      catch (Exception e)
      {
         System.err.println("Cannot create " + NN_FILENAME);
         System.exit(1);
      }
      new File(NN_RESULTS_FILENAME).delete();
      ArrayList<String> commandList = new ArrayList<>();
      commandList.add("python");
      commandList.add(NN_FILENAME);
      commandList.add("-f");
      commandList.add(NN_RESULTS_FILENAME + "");
      String[] hidden = NN_HIDDEN.split(",");
      for (String neurons : hidden)
      {
         commandList.add("-h");
         commandList.add(neurons);
      }
      commandList.add("-e");
      commandList.add(NN_EPOCHS + "");
      if (!VERBOSE)
      {
         commandList.add("-q");
      }
      ProcessBuilder processBuilder = new ProcessBuilder(commandList);
      processBuilder.inheritIO();
      Process process;
      try
      {
         process = processBuilder.start();
         process.waitFor();
      }
      catch (InterruptedException e) {}
      catch (IOException e)
      {
         System.err.println("Cannot run " + NN_FILENAME + ":" + e.getMessage());
         System.exit(1);
      }
      if (VERBOSE)
      {
         System.out.println("Results written to " + NN_RESULTS_FILENAME);
      }

      // Fetch the results.
      try
      {
         BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(NN_RESULTS_FILENAME)));
         String         json;
         if ((json = br.readLine()) != null)
         {
            JSONObject jObj = null;
            try
            {
               jObj = new JSONObject(json);
            }
            catch (JSONException e)
            {
               System.err.println("Error parsing results file " + NN_RESULTS_FILENAME);
               System.exit(1);
            }
            String train_prediction_errors = jObj.getString("train_prediction_errors");
            if ((train_prediction_errors == null) || train_prediction_errors.isEmpty())
            {
               System.err.println("Error parsing results file " + NN_RESULTS_FILENAME);
               System.exit(1);
            }
            String train_total_predictions = jObj.getString("train_total_predictions");
            if ((train_total_predictions == null) || train_total_predictions.isEmpty())
            {
               System.err.println("Error parsing results file " + NN_RESULTS_FILENAME);
               System.exit(1);
            }
            String train_error_pct = jObj.getString("train_error_pct");
            if ((train_error_pct == null) || train_error_pct.isEmpty())
            {
               System.err.println("Error parsing results file " + NN_RESULTS_FILENAME);
               System.exit(1);
            }
            String test_prediction_errors = jObj.getString("test_prediction_errors");
            if ((test_prediction_errors == null) || test_prediction_errors.isEmpty())
            {
               System.err.println("Error parsing results file " + NN_RESULTS_FILENAME);
               System.exit(1);
            }
            String test_total_predictions = jObj.getString("test_total_predictions");
            if ((test_total_predictions == null) || test_total_predictions.isEmpty())
            {
               System.err.println("Error parsing results file " + NN_RESULTS_FILENAME);
               System.exit(1);
            }
            String test_error_pct = jObj.getString("test_error_pct");
            if ((test_error_pct == null) || test_error_pct.isEmpty())
            {
               System.err.println("Error parsing results file " + NN_RESULTS_FILENAME);
               System.exit(1);
            }
            if (VERBOSE)
            {
               System.out.print("Train prediction errors/total = " + train_prediction_errors + "/" + train_total_predictions);
               System.out.println(" (" + train_error_pct + "%)");
               System.out.print("Test prediction errors/total = " + test_prediction_errors + "/" + test_total_predictions);
               System.out.println(" (" + test_error_pct + "%)");
            }
         }
         else
         {
            System.err.println("Cannot read results file " + NN_RESULTS_FILENAME);
            System.exit(1);
         }
         br.close();
      }
      catch (Exception e)
      {
         System.err.println("Cannot read results file " + NN_RESULTS_FILENAME + ":" + e.getMessage());
         System.exit(1);
      }
   }


   // Learn causations with RNN.
   public static void learnCausationsRNN(String filename)
   {
   }


   // Learn causations with TCN.
   public static void learnCausationsTCN(String filename)
   {
   }
}
