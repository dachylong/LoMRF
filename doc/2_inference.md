# Inference #

## Types of inference in LoMRF

In order to perform inference, we have to define the following:
  * The type of the inference. In LoMRF two types of inference can be performed:
    1. Marginal inference: computes the probabilities for all possible instantiations of query predicates of being true, given the evidence.
    2. Maximum a-posteriori inference (MAP): computes the truth values (0 = *False* and 1 = *True*) of all possible instantiations
    (groundings) that together represent the most probable state.
  * Input theory file, e.g., `theory.mln`.
  * Input evidence, e.g., `evidence.db`.
  * Output result file, e.g., `output.result`.
  * The atomic signatures (identities) that define the query predicates ('-q' option). Please note that for all query predicates LoMRF takes [Open-world assumption](https://en.wikipedia.org/wiki/Open-world_assumption).
  * (Optionally) The atomic signatures (identities) that define predicates with [Closed-world assumption](https://en.wikipedia.org/wiki/Closed-world_assumption) ('-cwa' option).
  By default, all evidence predicates have Closed-world assumption.
  * (Optionally) The atomic signatures (identities) that define predicates with [Open-world assumption](https://en.wikipedia.org/wiki/Open-world_assumption) ('-owa' option).
  By default, all non-evidence predicates have Open-world assumption.

### Inference using the `lomrf` commmand-line tool

To demonstrate the usage of LoMRF from commmand-line interface, assume that we have one knowledge base
file, named as `theoy.mln`, and one evidence file, named as `evidence.db`.  

In our example knowledge-base we have the following predicates:

| Predicate Name | Number of arguments | Predicate identity | Description |
|:--------------:|:-------------------:|:------------------:|:------------|
| Query_A | 2 | `Query_A/2` | first query predicate (Open-world assumption)
| Query_B | 2 | `Query_B/2` | second query predicate (Open-world assumption)
| Ev_A | 1 | `EV_A/1` | first evidence predicate (Closed-world assumption)
| Ev_B | 1 | `EV_B/1` | second evidence predicate (Closed-world assumption)
| Hidden | 1 | `Hidden/1`| non-evidence predicate (Open-world assumption)

As it is presented in the above table, there are two query predicates, `Query_A` and `Query_B`, where each one takes two terms as arguments. Therefore their atomic signatures are `Query_A/2` and `Query_B/2`. Similarly, there are two evidence predicates `Ev_A` and `Ev_B` that they take one term as argument. Therefore, the atomic signatures of `Ev_A` and `Ev_B` are `Ev_A/1` and `Ev_B/1`, respectively. Finally, there is one non-evidence predicate, named as `Hidden`, that takes one term as argument, thus it's signatures is `Hidden/1`.

#### Marginal inference

Marginal inference computes the conditional probability of query predicates, given some evidence.

```lang-none
lomrf -infer marginal -i theory.mln -e evidence.db -r marginal-out.result -q Query_A/2,Query_B/2 -cwa Ev_A/1,Ev_B/1 -owa Hidden/1
```
Since `Hidden/1` is a non-evidence atom and thus does not contain any facts in the `evidence.db`, we can omit the `-owa` parameter. Furthermore, when the evidence file (evidence.db) contains at least one fact for Ev_A/1 and Ev_B/1 predicates, then we can also omit the parameter `-cwa`.
In that case, LoMRF can automatically infer which predicates have Open-world or Closed-world assumption. As a result, we can simply give the following parameters and take the same results:

```lang-none
lomrf -infer marginal -i theory.mln -e evidence.db -r marginal-out.result -q Query_A/2,Query_B/2
```

Also the default inference type for LoMRF is marginal, we can further simplify the example given the following parameters:

```lang-none
lomrf -i theory.mln -e evidence.db -r marginal-out.result -q Query_A/2,Query_B/2
```

The results from marginal inference are stored in the `marginal-out.result` (see parameter `-r`)

#### MAP inference

MAP inference, on the other hand, identifies the most probable assignment among all query predicate instantiations that are consistent with the given evidence. This task reduces to finding
the truth assignment of all query predicate instantiations that maximises the sum of weights of satisfied ground clauses. This is equivalent to the weighted maximum satisfiability problem.

```lang-none
lomrf -infer map -i theory.mln -e evidence.db -r map-out.result -q Query_A/2,Query_B/2 -cwa Ev_A/1,Ev_B/1 -owa Hidden/1
```

or simply:

```lang-none
lomrf -infer map -i theory.mln -e evidence.db -r map-out.result -q Query_A/2,Query_B/2
```

The results from MAP inference are stored in the `map-out.result` (see paramter `-r`)


## Command-line Interface Options ##

By executing the ```lomrf -h``` (or ```lomrf --help```) command from the command-line interface, we take a print of multiple parameters. Below we explain all LoMRF inference command-line interface parameters:

### Basic inference options

* `-i, --input <kb file>` **[Required]** Specify the input knowledge base file, that is the file that contains the
 theory (see [Syntax](1_syntax.md) and [Quick Start](0_quick_start) for further information). You can specify either
 full or relative path or only the filename (when the file is in the current working path). For example,
 (1) full path `-i /full/path/to/theory.mln` in a Unix-based OS or `-i c:\full\path\to\theory.mln` in Windows,
 (2) relative path `-i path/to/theory.mln` in a Unix-based OS or `-i path\to\theory.mln` in Windows and
 (3) current working path `-i theory.mln`.

* `-e, --evidence <db file(s)>` **[Optional]** Specify the input evidence file(s) (see [Syntax](1_syntax.md) and
[Quick Start](0_quick_start) for further information). Multiple evidence files are allowed by separating them with
commas *without spaces*. For example, `-e file1.db,file2.db,file3.db`. Similarly with the `-i` option, you can specify
either full or relative path or only the filename (when the file is in the current working path). In case that there is
no evidence, this option can be omitted.

* `-r, --result <result file>` **[Required]** Specify the output file name to write the results of the inference.
For example, `-r output.result`. Similarly with the `-i` option, you can specify either full or relative path or only
the filename (when the file is in the current working path).

* `-q, --query <string>` **[Required]** Specify the atomic signatures or identities (i.e., predicate_name/arity) of
predicates that we would like to query. For example, the query predicate `Male(person)` has name `Male` and arity 1
(single argument), therefore we should give the argument `-q Male/1`. Multiple query atoms are allowed in LoMRF and
they are defined as comma-separated identities without white-spaces. For example, `-q Male/1,Female/1`. Please note
that LoMRF takes [Open-world assumption](https://en.wikipedia.org/wiki/Open-world_assumption) for all query atoms and
this cannot be overridden.

* `-cwa, --closed-world-assumption <string>` **[Optional]** Specify the atomic signatures or identities (i.e., predicate_name/arity)
of predicates that we would like to have [Closed-world assumption](https://en.wikipedia.org/wiki/Closed-world_assumption).
By default, LoMRF takes Closed-world assumption for all evidence atoms, that is atoms that have at least one fact in
the specified evidence files (see `-e` option). All non-evidence atoms are open-world in LoMRF, except when are included
in the `-cwa` option. For example, assume that in the evidence file(s) we have facts about the predicate `Foo/1` but not
for the predicate `Bar/1` and we would like to have Closed-world for both `Foo/1` and `Bar/1` predicates. In that case, we
have to explicitly define that `Bar/1` is also closed-world, therefore we should give `-cwa Bar/1`. Please note that in
this example, it is not required to specify that the predicate `Foo/1` is closed-world, because it has at least one fact
in the evidence file(s). If it is desirable, we can also define explicitly closed-world for `Foo/1`, i.e., `-cwa Foo/1,Bar/1`
(multiple identities are defined using comma-separated values, without spaces).

* `-owa, --open-world-assumption <string>` **[Optional]** Specify the atomic signatures or identities (i.e., predicate_name/arity)
of predicates that we would like to have [Open-world assumption](https://en.wikipedia.org/wiki/Open-world_assumption).
By default, all non-evidence atoms are open-world in LoMRF, except when are included in the `-cwa` option.

* `-infer, --inference-type <map | marginal>` **[Optional]** Specify the inference type, either [MAP](https://en.wikipedia.org/wiki/Maximum_a_posteriori_estimation)
or Marginal. By default LoMRF uses marginal inference, in order to estimate the marginal probabilities of all possible
query predicate instantiations. MAP inference can be performed using either local-search algorithm ([MaxWalkSAT](http://www.cs.rochester.edu/u/kautz/walksat/), [Selman et. al., 1993; Kautz et. al., 1996](#references))
or using an Integer Linear Programming ([ILP](https://en.wikipedia.org/wiki/Integer_programming)) solver ([Huynh and Mooney, 2011](#references)). Marginal inference
is estimated using the [MC-SAT](http://alchemy.cs.washington.edu/papers/poon06/) ([Hoifung and Domingos, 2006](#references)) algorithm, that is a [Metropolis–Hastings](https://en.wikipedia.org/wiki/Metropolis%E2%80%93Hastings_algorithm) algorithm
(a [Markov chain Monte Carlo](https://en.wikipedia.org/wiki/Markov_chain_Monte_Carlo) method)
that combines [Simulated-Annealing](https://en.wikipedia.org/wiki/Simulated_annealing) with a local-search [SAT solver](https://en.wikipedia.org/wiki/Boolean_satisfiability_problem)
([MaxWalkSAT](http://www.cs.rochester.edu/u/kautz/walksat/)), using a [Slice-sampling](https://en.wikipedia.org/wiki/Slice_sampling)
technique.


### Advanced inference options

* `-mapType, --map-type <mws | ilp>` **[Optional]** When inference is set to MAP (i.e., `-infer map`) we can define
either [MaxWalkSAT](http://www.cs.rochester.edu/u/kautz/walksat/) ([Selman et. al., 1993; Kautz et. al., 1996](#references)) or relaxed [Integer Linear Programming](https://en.wikipedia.org/wiki/Integer_programming) ([Huynh and Mooney, 2011](#references))
as MAP inference algorithm to use. By default LoMRF uses a variant of the MaxWalkSAT algorithm (with [TABU search](https://en.wikipedia.org/wiki/Tabu_search)).

* `-mapOutput, --map-output-type <all | positive>` **[Optional]** When MAP inference is selected (i.e., `-infer map`) we
can specify the format in the result file (i.e., `-r` option). We can choose either one of the following:

  1. `all`, in order to contain all possible
groundings of the resulting query predicates associated with their truth value (i.e., 0 for false and 1 for true) or
  2. `positive`, in order to contain only the ground atoms that have positive state (true).

  By default, LoMRF outputs all groundings with their truth values.

* `-satHardUnit, --sat-hard-unit` **[Optional]** Try to trivially satisfy all hard-constrained unit clauses in MaxWalkSAT.
After the grounding process, hard-constrained ground unit-clauses may produced. Such clauses contain only one ground literal
and therefore can be handled like fact predicates. If the literal is positive, then the state that satisfies this clause is true.
Similarly, if the literal is negative then the state that satisfies this clause is false. Consider, for example the ground hard-constrained
unit clause `Male(Odysseus)`. The literal is positive (i.e., is not negated), thus the clause is satisfied when `Male(Odysseus) = True`.

* `-satHardPriority, --sat-hard-priority` **[Optional]** This option applies to MaxWalkSAT and MC-SAT algorithms.
Enables satisfiability priority to hard-constrained clauses. Specifically, when the local-search algorithm (either
MaxWalkSAT or MC-SAT) requires to select an unsatisfied clause and the `satHardPriority` is enabled, then the algorithm
picks a recently unsatisfied hard-constrained clause (if any exists). In any other case the selection is random. Therefore,
`satHardPriority` adds an additional bias to satisfy hard-constrained clauses.


* `-ilpRounding, --ilp-rounding <roundup | mws>` **[Optional]** Specify which rounding algorithm to use in ILP. LoMRF uses the
algorithm proposed by [Huynh and Mooney (2011)](#references), in which the ILP problem is relaxed as a standard LP problem.
The resulting LP solution, however, may not integral. As a result the solution may contain ground query predicates in
which their state may be any number in the interval from 0 to 1, exclusive. Since, in MAP inference the solution should
contain only 0/1 states for each resulting ground predicate, LoMRF employs a rounding algorithm to change the non-integral
solutions to 0/1 state values. By default LoMRF uses the RoundUp algorithm of [Huynh and Mooney (2011)](#references) (i.e., `-ilpRounding roundup`). Alternatively, LoMRF can run the local-search MaxWalkSAT only for the non-integral part (i.e., `-ilpRounding mws`).

* `-ilpSolver, --ilp-solver <lpsolve | ojalgo | gurobi>` **[Optional]** When MAP inference is chosen to be solved using
an ILP solver, we can specify which solver to use. We can choose between the open-source solvers
[LPSolve](http://lpsolve.sourceforge.net/5.5/) and [ojAlgo](http://ojalgo.org/),
as well as the commercial solver [Gurobi](http://www.gurobi.com/). By default LoMRF uses the open-source solver LPSolve.

* `-samples, --num-samples <value>` **[Optional]** Specify the number of samples to take in MC-SAT (default is 1000).

* `-pSA, --probability-simulated-annealing <value>` **[Optional]** Specify the probability of performing a simulated
annealing step (default is 0.5), when using MC-SAT for marginal inference. Every step in MC-SAT may involve multiple
iterations, in which the state of ground atoms is flipped. At each iteration MC-SAT performs either simulated annealing or a standard MaxWalkSAT step. This option adjusts the probability to choose a simulated annealing step.

* `-pBest, --probability-best-search <value>` **[Optional]** The probability of performing a greedy search (default is 0.5)
in MaxWalkSAT or MC-SAT. The algorithm performs either a noisy or a greedy move, in particular:
  * In a *noisy move* it takes a random unsatisfied
clause and it chooses a random atom to flip.
  * In a *greedy move* it chooses to flip the atom having the best score,  that is, it satisfies more clauses.

* `-saTemperature, --simulated-annealing-temperature <value>` **[Optional]** Adjust the temperature of the simulated annealing
step in the MC-SAT algorithm. The value should range between 0 and 1. The default value is 0.8.

* `-maxFlips, --maximum-flips <value>`  **[Optional]** Specify the maximum number of flips taken to reach a solution in
MaxWalkSAT and MC-SAT (default is 1000000).

* `-targetCost, --target-cost <value>` **[Optional]** In MaxWalkSAT and MC-SAT, any possible world having cost below this
threshold will be considered as a solution (default is 0.0001).

* `-maxTries, --maximum-tries <value>` **[Optional]** Specify the maximum number of attempts to find a solution in
MaxWalkSAT and MC-SAT (default is 1)

* `-numSolutions, --number-of-solutions <value>` **[Optional]** Give the *n*-th solution (i.e., cost < target cost) in MC-SAT (default is 10).

* `-tabuLength, --tabu-length <value>` **[Optional]** Specify the minimum number of flips between flipping the same atom
in MaxWalkSAT and MC-SAT (default is 10)

* `-unitProp, --use-unit-propagation <value>` **[Optional]** Perform unit-propagation in MC-SAT (default is true).
 Performs unit propagation across the constraints in order to trivially satisfy as many as possible. When enabled, the
 search space in MC-SAT is minimized, increases sampling performance and accuracy.

* `-lateSA, --late-simulated-annealing <value>` **[Optional]**  When enabled (i.e., true), simulated annealing is
performed only when MC-SAT reaches a plateau (i.e. a world with cost <= 'targetCost'). Disabling lateSA (= false)
causes MC-SAT to converge slower, since in every iteration simulated annealing is performed (with probability = 'pSA').
By default lateSA is 'true'.

* `-noNegWeights, --eliminate-negative-weights` **[Optional]** When it is enabled, LoMRF eliminates negative
weight values from (ground) clauses. Specifically, the sign of negative weights in clauses is inverted, as well as all
disjunctions become conjunctions (due to de Morgan's law). For example, using the de Morgan's law, the weighted
clause `-2 A(x) v B(x)` is equivalent to `-2 !(!A(x) ^ !B(x))`. In MLN this is also equivalent to `2 !A(x) ^ !B(x)`,
which produces the following two unit clauses: `1 A(x)` and `1 B(x)`.

* `-noNegatedUnit, --eliminate-negated-unit` **[Optional]** When it is enabled, unit clauses with negative
literal become unit clauses with positive literal and inverted sign in their corresponding weight.

* `-dynamic, --dynamic-implementations <string>` **[Optional]** Comma separated paths to search recursively for dynamic
predicates/functions implementations (*.class and *.jar files).

## Examples

See Sections [Probabilistic Inference Examples](2_1_inference_examples.md) and [Temporal Probabilistic Inference Examples](2_2_temporal_inference_examples.md).

## References

Bart Selman, Henry Kautz, and Bram Cohen. (1993) Local Search Strategies for Satisfiability Testing.  Final version appears
in Cliques, Coloring, and Satisfiability: Second DIMACS Implementation Challenge. In David S. Johnson and Michael A. Trick, (Ed.),
DIMACS Series in Discrete Mathematics and Theoretical Computer Science, vol. 26, AMS. ([link](http://www.cs.cornell.edu/selman/papers/pdf/dimacs.pdf))

Henry Kautz, Bart Selman and Yueyen Jiang. (1996) A General Stochastic Approach to Solving Problems with Hard and Soft Constraints.
In Gu, D., Du, J. and Pardalos, P. (Eds.), The Satisfiability Problem: Theory and Applications, Vol. 35 of DIMACS Series in
Discrete Mathematics and Theoretical Computer Science, pp. 573–586. AMS. ([link](https://cs.rochester.edu/u/kautz/papers/maxsatDIMACSfinal.ps))

Tuyen N. Huynh and Raymond J. Mooney. (2011). Max-Margin Weight Learning for Markov Logic Networks. In Proceedings of the
European Conference on Machine Learning and Principles and Practice of Knowledge Discovery in Databases (ECML-PKDD 2011),
Vol. 2, pp. 81-96. ([link](http://www.ai.sri.com/~huynh/papers/huynh_mooney_ecmlpkdd09.pdf))

Poon, Hoifung and Domingos, Pedro (2006). Sound and Efficient Inference with Probabilistic and Deterministic Dependencies.
In Proceedings of the 21th National Conference on Artificial Intelligence (pp. 458-463), 2006. Boston, MA: AAAI Press. ([link](http://homes.cs.washington.edu/~pedrod/papers/aaai06a.pdf))
