# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Mandala is a research neural network model combining ideas from Morphognosis (contextual MLP
learning) and Mona (contextual causation learning with goal-directed motivation). The codebase
generates synthetic "causation hierarchies" (tree-structured cause/effect grammars with terminal
and nonterminal causations), derives paths through them, and trains both an MLP ("NN") and a
recurrent network ("RNN") to predict the next step in a path — the point being that Mandala's
tiered-context MLP approach tolerates interrupting/overlaid causation chains better than a
conventional RNN. See `README.md` for the full description and `doc/` for the associated papers.

## Build and run

The Java side is Maven-configured (`pom.xml`, Java 8 target, deeplearning4j/nd4j deps) but the
actual dev workflow uses the plain shell/batch scripts in `src/main/scripts/`, run from a working
directory (conventionally `work/`, sibling to `bin/` and `lib/`) since they use relative paths
(`../lib`, `../bin`, `../src`). The scripts live in `src/main/scripts/` (`.sh` and `.bat` variants);
`mandala_test.sh <runs>` is the parameter-sweep harness, not a unit test suite.

Requires Java, Python, and Keras (`mandala_nn.py`/`mandala_rnn.py`, invoked as subprocesses — see
below). There are no unit tests; `mandala_test.sh` is the only "test" harness and it's a parameter
sweep that reports aggregate prediction error percentages, not pass/fail assertions.

Full CLI usage (all `mandala.Mandala` flags) is documented in `README.md` — consult it rather than
re-deriving from `Usage` in `Mandala.java`.

## Architecture

Almost all Java logic lives in one large file, `src/main/java/mandala/Mandala.java` (~3400 lines,
single class with static state and nested static classes). The pipeline, driven from `main()`,
is:

1. **Generate or load** causation hierarchies (`generateCausationHierarchy`) — either fresh
   (`-numCausationHierarchies` trees of `NonterminalCausation`/`TerminalCausation` built from
   `-numNonterminals`/`-numTerminals`/`-terminalProductionProbability`/interstitial-terminal
   params) or deserialized from a `-load` save file. `Graph.java` is used to detect cycles when
   building hierarchies.
2. **Optionally export** the hierarchy as a Graphviz `.dot` file (`exportCausationsGraph`,
   `-exportCausationsGraph`).
3. **Generate causation paths** (`generateCausationPaths`) — sampled traversals through each
   hierarchy (`-numCausationPaths` per hierarchy), each step carrying a stack of
   `CausationTier`/`ContextFeatures` (the accumulated multi-tier context, up to
   `-maxContextTier`, with tier values expiring per `-contextTierValueDurationType`).
4. **Export datasets** for both learners: `exportNNdataset` writes `mandala_nn_dataset.py`
   (flat/tiered-context examples for the MLP) and `exportRNNdataset` writes
   `mandala_rnn_dataset.py` (sequential examples for the RNN). Both are plain Python files
   containing embedded data, imported by the learner scripts.
5. **Train + evaluate** via subprocess: `learnCausationsNN`/`learnCausationsRNN` extract the
   bundled `mandala_nn.py`/`mandala_rnn.py` from the jar's classpath resources, write them to the
   working directory, then shell out (`ProcessBuilder`, `python <script> --dimensions --features
   --neurons --epochs ...`) to train a Keras model and write results as one JSON line to
   `mandala_nn_results.json`/`mandala_rnn_results.json`, which Java parses back into a
   `LearningResults` (train/test prediction error counts and percentages).

Key nested types in `Mandala.java` worth knowing before editing: `Causation` (base, with
feature-encoding via seeded `SplittableRandom` hashing so features are deterministic per
hierarchy/id), `TerminalCausation`/`NonterminalCausation`, `CausationTier`/`CausationPath`,
`ContextFeatures` (the per-tier context vector plus expiring `value`/`age`), and
`LearningResults`.

Other components, mostly independent of the main pipeline:
- `MandalaCoder.java` / `MandalaCoderNN.java`: standalone demo of encoding cause→effect pairs
  through a small autoencoder-style network (dl4j), unrelated to the causation-hierarchy pipeline.
- `src/main/python/causation_coder.py` / `sparse_coder.py`: Keras equivalents/experiments for
  coding cause→effect relationships.
- `src/main/python/chart_results.py`: plots `mandala_test_results*.csv` (Mandala vs RNN error) via
  matplotlib — separate, run manually after a test sweep.
- `src/main/c++/pattern_detector/`: an unrelated standalone C++/Visual Studio experiment for
  detecting which input signals matter to an output; not part of the Java/Python build.

`work/` is a scratch/output directory (build artifacts, generated `.py` datasets, `.dot`/`.png`
graph exports, prior test run CSVs) — treat its contents as regenerable, not source.
