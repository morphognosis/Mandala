# Mandala: Goal-directed behavior using contextual causation learning.

In this project, the Morphognosis and Mona neural network models are combined into
Mandala, a comprehensive model for animal learning and behavior.
Mona features a contextual causation learning with goal-directed motivation.
Mophognosis features a contextual multilayer perceptron.

Mona:
A pair of cooperating nest-building and foraging birds. 
See: http://tom.portegys.com/research.html#nestingbirds

Morphognosis:
Honey bees forage for flower nectar cooperatively.
See: http://tom.portegys.com/research.html#honey_bees

Build with Eclipse project or build.sh/build.bat and export to bin/mandala.jar to run with scripts in the work directory.
Requires java, python, and the keras machine learning package.

```
Usage:
    java mandala.Mandala
      [-numCausationHierarchies <quantity> (default=1)]
      [-numNonterminals <quantity> (default=10)]
      [-numTerminals <quantity> (default=20)]
      [-numInterstitialTerminals <quantity> (default=20, if 0 resort to non-interstitial terminals)]
      [-maxInterstitialTerminalSequence <length> (default=10)]
      [-minProductionRightHandSideLength <quantity> (default=2)]
      [-maxProductionRightHandSideLength <quantity> (default=2)]
      [-terminalProductionProbability <probability> (default=0.5)]
      [-numDimensions <quantity> (default=64)]
      [-numFeatures <quantity> (default=3)]
      [-exportCausationsGraph [<file name> (Graphviz dot format, default=mandala_causations.dot)]
          [-treeFormat "true" | "false" (default=true)]]
      [-numCausationPaths <quantity> (default=10)]
      [-maxContextFeatureTier <value> (default=10)]
      [-updateInterstitialContexts "true" | "false" (default=false)]
      [-featureValueDurationType "minimum" | "expected" | "maximum" (default=maximum)]
      [-NNdatasetTrainFraction <fraction> (default=0.75)]
      [-NNneurons<number of neurons> (comma-separated for additional layers) (default=128,128,128)]
      [-NNepochs <number of epochs> (default=500)]
      [-RNNdatasetTrainFraction <fraction> (default=0.75)]
      [-RNNneurons <number of neurons> (comma-separated for additional layers) (default=128)]
      [-RNNepochs <number of epochs> (default=500)]
      [-randomSeed <seed> (default=4517)]
      [-quiet]
  Help:
    java mandala.Mandala -help
Exit codes:
  0=success
  1=error
```

