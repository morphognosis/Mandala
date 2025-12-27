// For conditions of distribution and use, see copyright notice in Mandala.java

// Generate causation hierarchies to measure their properties.

package mandala;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;

public class CausationsMetrics
{
   // Generation parameters.
   public static int   NUM_CAUSATION_HIERARCHIES       = 1;
   public static int   NUM_NONTERMINALS                = 10;
   public static int   NUM_TERMINALS                   = 20;
   public static int   MIN_PRODUCTION_RHS_LENGTH       = 2;
   public static int   MAX_PRODUCTION_RHS_LENGTH       = 2;
   public static float TERMINAL_PRODUCTION_PROBABILITY = 0.5f;
   public static int   MAX_INTERSTITIAL_TERMINALS      = 10;

   // Dimensions.
   public static int NUM_DIMENSIONS = 64;

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
   };

   // Terminal causation.
   public static class TerminalCausation extends Causation
   {
      public int feature;

      public TerminalCausation(int hierarchy, int id)
      {
         super(hierarchy, id);
         encodeFeature();
      }


      // Encode feature.
      public void encodeFeature()
      {
         String seedString = id + "_";
         long   seed       = seedString.hashCode();
         Random r          = new Random(seed);

         feature = r.nextInt(NUM_DIMENSIONS);
      }


      public void print()
      {
         System.out.print("hierarchy=" + hierarchy);
         System.out.print(", id=" + id);
         System.out.print(", feature=" + feature);
         System.out.print(", parents:");
         for (Causation p : parents)
         {
            System.out.print(" " + p.id);
         }
         System.out.println();
      }


      public void printHierarchical(String indent, String childNum)
      {
         System.out.print(indent);
         System.out.print("terminal id=" + id);
         System.out.print(", features=" + feature);
         if (childNum != null)
         {
            System.out.print(", child number=" + childNum);
         }
         System.out.println();
      }
   };

   // Nonterminal causation.
   public static class NonterminalCausation extends Causation
   {
      public ArrayList<Causation> children;

      public NonterminalCausation(int hierarchy, int id)
      {
         super(hierarchy, id);
         children = new ArrayList<Causation>();
      }


      public void print()
      {
         System.out.print("hierarchy=" + hierarchy);
         System.out.print(", id=" + id);
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
         System.out.println();
      }


      public void printHierarchical(String indent, String childNum, boolean recursive)
      {
         System.out.print(indent);
         System.out.print("nonterminal id=" + id);
         if (childNum != null)
         {
            System.out.print(", child number=" + childNum);
         }
         System.out.println();
         if (recursive)
         {
            for (int i = 0, j = children.size(); i < j; i++)
            {
               Causation child = children.get(i);
               if (child instanceof TerminalCausation)
               {
                  TerminalCausation terminal = (TerminalCausation)child;
                  terminal.printHierarchical(indent + "  ", i + "");
               }
               else
               {
                  NonterminalCausation nonterminal = (NonterminalCausation)child;
                  nonterminal.printHierarchical(indent + "  ", i + "", true);
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
   public static class CausationTier
   {
      public Causation causation;
      public int       currentChild;

      public CausationTier(Causation causation, int currentChild)
      {
         this.causation    = causation;
         this.currentChild = currentChild;
      }


      public void print()
      {
         if (causation instanceof TerminalCausation)
         {
            ((TerminalCausation)causation).printHierarchical("", null);
         }
         else
         {
            NonterminalCausation nonterminalCausation = (NonterminalCausation)causation;
            String               childInfo            = currentChild + "/" + nonterminalCausation.children.size();
            nonterminalCausation.printHierarchical("", childInfo, false);
         }
      }
   };
   public static int   NUM_CAUSATION_PATHS = 10;
   public static class CausationPath
   {
      int hierarchy;
      int id;
      ArrayList < ArrayList < CausationTier >> steps;

      public CausationPath(int hierarchy, int id)
      {
         this.hierarchy = hierarchy;
         this.id        = id;
         steps          = new ArrayList < ArrayList < CausationTier >> ();
      }


      public void add(ArrayList<CausationTier> step)
      {
         steps.add(step);
      }


      public void print()
      {
         System.out.println("path: hierarchy=" + hierarchy + "/root id=" + id + "\nsteps: ");
         for (int i = 0, j = steps.size(); i < j; i++)
         {
            System.out.println("step " + i + ": ");
            ArrayList<CausationTier> tiers = steps.get(i);
            for (CausationTier tier : tiers)
            {
               tier.print();
            }
         }
      }
   };
   public static ArrayList<CausationPath> causationPaths;

   // Causation attenuation.
   public static float CAUSATION_FEATURE_ATTENUATION = 0.5f;
   public static float CAUSATION_TIER_ATTENUATION    = 1.0f;

   // Context features.
   public static class ContextFeature
   {
      public int   feature;
      public int   tier;
      public float value;
      public int   begin;
      public int   end;

      // Constructors.
      public ContextFeature(int feature, int tier, float value, int begin, int end)
      {
         this.feature = feature;
         this.tier    = tier;
         this.value   = value;
         this.begin   = begin;
         this.end     = end;
      }


      public ContextFeature(int source1, int source2, int tier, float value, int begin, int end)
      {
         setFeature(source1, source2);
         this.tier  = tier;
         this.value = value;
         this.begin = begin;
         this.end   = end;
      }


      // Set feature from source features.
      public void setFeature(int source1, int source2)
      {
         String s = source1 + "_" + source2;
         Random r = new Random(s.hashCode());

         feature = r.nextInt(NUM_DIMENSIONS);
      }


      // Attenuate value.
      public boolean attentuate()
      {
         if (tier == 0)
         {
            value -= 0.5f;
         }
         else
         {
            value -= CAUSATION_FEATURE_ATTENUATION / Math.pow(tier, CAUSATION_TIER_ATTENUATION);
         }
         if (value > 0.0f)
         {
            return(true);
         }
         else
         {
            value = 0.0f;
            return(false);
         }
      }


      // Print.
      public void print()
      {
         System.out.println("feature=" + feature + ", tier=" + tier + ", value=" + value + ", begin=" + begin + ", end=" + end);
      }
   };
   public static ArrayList < ArrayList < ContextFeature >> contextFeatures;

   // Random numbers.
   public static int    RANDOM_SEED = 4517;
   public static Random randomizer  = null;

   // Verbosity.
   public static boolean VERBOSE = true;

   // Usage.
   public static final String Usage =
      "Usage:\n" +
      "    java mandala.CausationsMetrics\n" +
      "      [-numCausationHierarchies <quantity> (default=" + NUM_CAUSATION_HIERARCHIES + ")]\n" +
      "      [-numNonterminals <quantity> (default=" + NUM_NONTERMINALS + ")]\n" +
      "      [-numTerminals <quantity> (default=" + NUM_TERMINALS + ")]\n" +
      "      [-minProductionRightHandSideLength <quantity> (default=" + MIN_PRODUCTION_RHS_LENGTH + ")]\n" +
      "      [-maxProductionRightHandSideLength <quantity> (default=" + MAX_PRODUCTION_RHS_LENGTH + ")]\n" +
      "      [-terminalProductionProbability <probability> (default=" + TERMINAL_PRODUCTION_PROBABILITY + ")]\n" +
      "      [-maxInterstitialTerminals <quantity> (default=" + MAX_INTERSTITIAL_TERMINALS + ")]\n" +
      "      [-numDimensions <quantity> (default=" + NUM_DIMENSIONS + ")]\n" +
      "      [-exportCausationsGraph [<file name> (Graphviz dot format, default=" + CAUSATIONS_GRAPH_FILENAME + ")]\n" +
      "          [-treeFormat \"true\" | \"false\" (default=" + TREE_FORMAT + ")]]\n" +
      "      [-numCausationPaths <quantity> (default=" + NUM_CAUSATION_PATHS + ")]\n" +
      "      [-causationFeatureAttenuation <multiplier> (default=" + CAUSATION_FEATURE_ATTENUATION + ")]\n" +
      "      [-causationTierAttenuation <divisor> (default=" + CAUSATION_TIER_ATTENUATION + ")]\n" +
      "      [-randomSeed <seed> (default=" + RANDOM_SEED + ")]\n" +
      "      [-quiet]\n" +
      "  Help:\n" +
      "    java mandala.CausationsMetrics -help\n" +
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
         if (args[i].equals("-maxInterstitialTerminals"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid maxInterstitialTerminals option");
               System.err.println(Usage);
               System.exit(1);
            }
            try
            {
               MAX_INTERSTITIAL_TERMINALS = Integer.parseInt(args[i]);
            }
            catch (NumberFormatException e) {
               System.err.println("Invalid maxInterstitialTerminals option");
               System.err.println(Usage);
               System.exit(1);
            }
            if (MAX_INTERSTITIAL_TERMINALS < 0)
            {
               System.err.println("Invalid maxInterstitialTerminals option");
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
         if (args[i].equals("-causationFeatureAttenuation"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid causationFeatureAttenuation option");
               System.err.println(Usage);
               System.exit(1);
            }
            try
            {
               CAUSATION_FEATURE_ATTENUATION = Float.parseFloat(args[i]);
            }
            catch (NumberFormatException e) {
               System.err.println("Invalid causationFeatureAttenuation option");
               System.err.println(Usage);
               System.exit(1);
            }
            if (CAUSATION_FEATURE_ATTENUATION < 0.0f)
            {
               System.err.println("Invalid causationFeatureAttenuation option");
               System.err.println(Usage);
               System.exit(1);
            }
            continue;
         }
         if (args[i].equals("-causationTierAttenuation"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid causationTierAttenuation option");
               System.err.println(Usage);
               System.exit(1);
            }
            try
            {
               CAUSATION_TIER_ATTENUATION = Float.parseFloat(args[i]);
            }
            catch (NumberFormatException e) {
               System.err.println("Invalid causationTierAttenuation option");
               System.err.println(Usage);
               System.exit(1);
            }
            if (CAUSATION_TIER_ATTENUATION < 0.0f)
            {
               System.err.println("Invalid causationTierAttenuation option");
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
         if (args[i].equals("-quiet"))
         {
            VERBOSE = false;
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

      // Analyze causations.
      analyzeCausations();

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

      // Generate causation dataset.
      generateNNdataset(RANDOM_SEED);

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
                  parent.children.add(child);
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
               parent.children = new ArrayList<Causation>();
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
         parent.children.add(child);
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
               terminal.printHierarchical("    ", null);
            }
            else
            {
               NonterminalCausation nonterminal = (NonterminalCausation)root;
               nonterminal.printHierarchical("    ", null, true);
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
         String            feature  = "(" + terminal.feature + ")";
         vertices.add("h" + hierarchy + "_t" + pathPrefix + vertex.id + " [label=\"" + vertex.id + feature + "\", shape=square];");
      }
      else
      {
         vertices.add("h" + hierarchy + "_nt" + pathPrefix + vertex.id + " [label=\"" + vertex.id + "\", shape=circle];");
         NonterminalCausation nonTerminal = (NonterminalCausation)vertex;
         for (int i = 0; i < nonTerminal.children.size(); i++)
         {
            Causation child           = nonTerminal.children.get(i);
            String    childPathPrefix = "";
            if (TREE_FORMAT)
            {
               childPathPrefix = new String(pathPrefix) + vertex.id + "_" + i + "_";
            }
            if (child instanceof TerminalCausation)
            {
               edges.add("h" + hierarchy + "_nt" + pathPrefix + vertex.id + " -> h" + hierarchy + "_t" + childPathPrefix + child.id + " [label=\"" + i + "\"];");
            }
            else
            {
               edges.add("h" + hierarchy + "_nt" + pathPrefix + vertex.id + " -> h" + hierarchy + "_nt" + childPathPrefix + child.id + " [label=\"" + i + "\"];");
            }
            listGraph(hierarchy, child, childPathPrefix, vertices, edges);
         }
      }
   }


   // Analyze causations.
   public static void analyzeCausations()
   {
      for (ArrayList<Causation> caucations : causationHierarchies)
      {
         for (Causation causation : caucations)
         {
            if (causation instanceof TerminalCausation)
            {
               ((TerminalCausation)causation).printHierarchical("", null);
            }
            else
            {
               ((NonterminalCausation)causation).printHierarchical("", null, true);
            }
         }
      }
      System.exit(0);      // flibber
   }


   // Generate causation paths.
   public static void generateCausationPaths(int numPaths)
   {
      // Generate causation hierarchy paths.
      causationPaths = new ArrayList<CausationPath> ();
      if (causationHierarchies.size() == 0)
      {
         return;
      }
      for (int i = 0; i < numPaths; i++)
      {
         int hierarchy = randomizer.nextInt(causationHierarchies.size());
         ArrayList<Causation> causationHierarchy = causationHierarchies.get(hierarchy);
         Causation            root = causationHierarchy.get(randomizer.nextInt(causationHierarchy.size()));
         CausationPath        path = new CausationPath(hierarchy, root.id);
         causationPaths.add(path);
         ArrayList<CausationTier> step = new ArrayList<CausationTier>();
         step.add(new CausationTier(root, 0));
         while (root instanceof NonterminalCausation)
         {
            NonterminalCausation nonterminal = (NonterminalCausation)root;
            root = nonterminal.children.get(0);
            step.add(new CausationTier(root, 0));
         }
         path.add(step);
         while (stepCausationPath(path, 0)) {}
         for (ArrayList<CausationTier> s : path.steps)
         {
            Collections.reverse(s);
         }
      }

      if (VERBOSE)
      {
         System.out.println("causation paths:");
         for (int i = 0; i < numPaths; i++)
         {
            CausationPath path = causationPaths.get(i);
            System.out.println("path=" + i + ", hierarchy=" + path.hierarchy + ", id=" + path.id);
            for (int j = 0; j < path.steps.size(); j++)
            {
               System.out.println("step=" + j);
               ArrayList<CausationTier> step = path.steps.get(j);
               for (int k = 0; k < step.size(); k++)
               {
                  CausationTier tier = step.get(k);
                  tier.print();
               }
            }
         }
      }
   }


   // Step along causation path.
   public static boolean stepCausationPath(CausationPath path, int context)
   {
      ArrayList<CausationTier> currentStep   = path.steps.get(path.steps.size() - 1);
      CausationTier            CausationTier = currentStep.get(context);
      if (CausationTier.causation instanceof TerminalCausation)
      {
         return(false);
      }
      if (stepCausationPath(path, context + 1))
      {
         return(true);
      }
      NonterminalCausation nonterminalCausation = (NonterminalCausation)CausationTier.causation;
      if (CausationTier.currentChild == nonterminalCausation.children.size() - 1)
      {
         return(false);
      }
      ArrayList<CausationTier> nextStep = new ArrayList<CausationTier>();
      for (int i = 0; i < context; i++)
      {
         CausationTier tier = currentStep.get(i);
         tier = new CausationTier(tier.causation, tier.currentChild);
         nextStep.add(tier);
      }
      CausationTier tier = currentStep.get(context);
      tier = new CausationTier(tier.causation, tier.currentChild);
      tier.currentChild++;
      nextStep.add(tier);
      do
      {
         NonterminalCausation parent = (NonterminalCausation)tier.causation;
         Causation            child  = parent.children.get(tier.currentChild);
         tier = new CausationTier(child, 0);
         nextStep.add(tier);
      } while (tier.causation instanceof NonterminalCausation);
      path.add(nextStep);
      return(true);
   }


   // Generate NN dataset.
   public static void generateNNdataset(int randomSeed)
   {
      if (VERBOSE)
      {
         System.out.println("generate NN dataset");
      }
      Random randomID = new Random(randomSeed);
      int    numPaths = causationPaths.size();
      int    maxTiers = 0;
      for (int i = 0; i < numPaths; i++)
      {
         CausationPath path = causationPaths.get(i);
         for (int j = 0; j < path.steps.size(); j++)
         {
            ArrayList<CausationTier> step = path.steps.get(j);
            if (step.size() > maxTiers)
            {
               maxTiers = step.size();
            }
         }
      }

      ArrayList < ArrayList < Float >> X_train = new ArrayList < ArrayList < Float >> ();
      ArrayList < ArrayList < Float >> y_train = new ArrayList < ArrayList < Float >> ();
      int tick     = 0;
      int numTrain = numPaths;
      for (int i = 0; i < numTrain; i++)
      {
         CausationPath path = causationPaths.get(i);
         if (VERBOSE)
         {
            path.print();
            System.out.println("data:");
         }
         contextFeatures = new ArrayList < ArrayList < ContextFeature >> ();
         for (int j = 0, k = maxTiers - 1; j < k; j++)
         {
            contextFeatures.add(new ArrayList<ContextFeature>());
         }
         int pathLength = 0;
         int p          = path.steps.size() - 1;
         for (int j = 0; j < p; j++)
         {
            ArrayList<CausationTier> xstep              = path.steps.get(j);
            ArrayList<CausationTier> ystep              = path.steps.get(j + 1);
            Causation                xcausation         = xstep.get(0).causation;
            TerminalCausation        xterminalCausation = (TerminalCausation)xcausation;
            Causation                ycausation         = ystep.get(0).causation;
            TerminalCausation        yterminalCausation = (TerminalCausation)ycausation;
            if ((xstep.size() > 1) && (xstep.get(1).currentChild == 0))
            {
               int xid = randomID.nextInt(NUM_TERMINALS);
               for (int n = 0; xid != xcausation.id && n < MAX_INTERSTITIAL_TERMINALS; n++)
               {
                  ArrayList<Float>  X_train_step     = new ArrayList<Float>();
                  ArrayList<Float>  y_train_step     = new ArrayList<Float>();
                  TerminalCausation xrandomCausation = new TerminalCausation(xcausation.hierarchy, xid);
                  TerminalCausation yrandomCausation = new TerminalCausation(xcausation.hierarchy, randomID.nextInt(NUM_TERMINALS));
                  if (VERBOSE)
                  {
                     System.out.print("X: *");
                     xrandomCausation.print();
                     System.out.print("y: ");
                     yrandomCausation.print();
                  }
                  for (int k = 0; k < maxTiers; k++)
                  {
                     if (k == 0)
                     {
                        for (int q = 0; q < NUM_DIMENSIONS; q++)
                        {
                           if (q == xrandomCausation.feature)
                           {
                              X_train_step.add(1.0f);
                           }
                           else
                           {
                              X_train_step.add(0.0f);
                           }
                        }
                        for (int q = 0; q < NUM_DIMENSIONS; q++)
                        {
                           if (q == yrandomCausation.feature)
                           {
                              y_train_step.add(1.0f);
                           }
                           else
                           {
                              y_train_step.add(0.0f);
                           }
                        }
                     }
                     else
                     {
                        ArrayList<Float> X_context = getTierContext(k - 1);
                        for (int q = 0; q < NUM_DIMENSIONS; q++)
                        {
                           X_train_step.add(X_context.get(q));
                           y_train_step.add(0.0f);
                        }
                     }
                  }
                  X_train.add(X_train_step);
                  y_train.add(y_train_step);
                  pathLength++;
                  updateContexts(xrandomCausation.feature, tick++);
                  xid = yrandomCausation.id;
               }
            }
            if (VERBOSE)
            {
               System.out.print("X: ");
               xterminalCausation.print();
               System.out.print("y: ");
               yterminalCausation.print();
            }
            ArrayList<Float> X_train_step = new ArrayList<Float>();
            ArrayList<Float> y_train_step = new ArrayList<Float>();
            for (int k = 0; k < maxTiers; k++)
            {
               if (k == 0)
               {
                  for (int q = 0; q < NUM_DIMENSIONS; q++)
                  {
                     if (q == xterminalCausation.feature)
                     {
                        X_train_step.add(1.0f);
                     }
                     else
                     {
                        X_train_step.add(0.0f);
                     }
                  }
                  for (int q = 0; q < NUM_DIMENSIONS; q++)
                  {
                     if (q == yterminalCausation.feature)
                     {
                        y_train_step.add(1.0f);
                     }
                     else
                     {
                        y_train_step.add(0.0f);
                     }
                  }
               }
               else
               {
                  ArrayList<Float> X_context = getTierContext(k - 1);
                  for (int q = 0; q < NUM_DIMENSIONS; q++)
                  {
                     X_train_step.add(X_context.get(q));
                     y_train_step.add(0.0f);
                  }
               }
            }
            X_train.add(X_train_step);
            y_train.add(y_train_step);
            updateContexts(xterminalCausation.feature, tick++);
            pathLength++;
         }
         if (VERBOSE)
         {
            System.out.println("path length=" + pathLength);
         }
      }
   }


   // Get tier context.
   static ArrayList<Float> getTierContext(int tier)
   {
      ArrayList<Float> features = new ArrayList<Float>();
      for (int q = 0; q < NUM_DIMENSIONS; q++)
      {
         features.add(0.0f);
      }
      ArrayList<ContextFeature> context = contextFeatures.get(tier);
      for (ContextFeature contextFeature : context)
      {
         if (features.get(contextFeature.feature) < contextFeature.value)
         {
            features.set(contextFeature.feature, contextFeature.value);
         }
      }
      return(features);
   }


   // Update feature contexts.
   static void updateContexts(int feature, int tick)
   {
      // Attenuate.
      for (int i = 0, j = contextFeatures.size(); i < j; i++)
      {
         ArrayList<ContextFeature> contexts    = contextFeatures.get(i);
         ArrayList<ContextFeature> tmpContexts = new ArrayList<ContextFeature>();
         for (ContextFeature context : contexts)
         {
            if (context.attentuate())
            {
               tmpContexts.add(context);
            }
         }
         contextFeatures.set(i, tmpContexts);
      }

      // Add contexts.
      ContextFeature contextFeature = new ContextFeature(feature, 0, 1.0f, tick, tick);
      addContextFeature(contextFeature, 0);
   }


   // Recursively add context feature.
   static void addContextFeature(ContextFeature contextFeature, int tier)
   {
      ArrayList<ContextFeature> contexts = contextFeatures.get(tier);
      for (ContextFeature feature : contexts)
      {
         if ((feature.feature == contextFeature.feature) && (feature.begin == contextFeature.begin) && (feature.end == contextFeature.end))
         {
            return;
         }
      }
      if (tier < contextFeatures.size() - 1)
      {
         contexts = contextFeatures.get(tier);
         ArrayList<ContextFeature> sources = new ArrayList<ContextFeature>();
         for (ContextFeature feature : contexts)
         {
            if (contextFeature.begin > feature.end)
            {
               boolean replaced = false;
               for (int i = 0, j = sources.size(); i < j; i++)
               {
                  ContextFeature source = sources.get(i);
                  if ((source.feature == feature.feature) && (source.value < feature.value))
                  {
                     sources.set(i, feature);
                     replaced = true;
                     break;
                  }
               }
               if (!replaced)
               {
                  sources.add(feature);
               }
            }
         }
         for (ContextFeature feature : sources)
         {
            float          value = (contextFeature.value + feature.value) / 2.0f;
            ContextFeature nextContextFeature = new ContextFeature(contextFeature.feature, feature.feature, tier + 1, value, feature.begin, contextFeature.end);
            addContextFeature(nextContextFeature, tier + 1);
         }
      }
      contextFeatures.get(tier).add(contextFeature);
   }
}
