# Kaggle Santa's workshop tour 2019

Code for Kaggle's annual optimisation competition. Code for last year can be
found [here](https://github.com/phil8192/tsp-java).

## Features

* Multithreaded search using a mix of brute-force and Simulated Annealing.
* Fast computation of objective function using deltas for assignment and
  accounting penalties.
* Random and brute-force search over cartesian product of candidate
  assignments.
* Simulated Annealing with probalistic candidate selection favouring least
  damaging moves.
