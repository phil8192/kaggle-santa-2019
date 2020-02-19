# Kaggle Santa's workshop tour 2019

Code for Kaggle's annual optimisation
[competition](https://www.kaggle.com/c/santa-workshop-tour-2019).
Code for last year can be found [here](https://github.com/phil8192/tsp-java).

## Features

* Multi-threaded search using a mix of brute-force and Simulated Annealing.
* Fast computation of objective function using deltas for assignment and
  accounting penalties.
* Random and brute-force search over cartesian product of candidate
  assignments.
* Simulated Annealing with probalistic candidate selection favouring least
  damaging moves.

## Score

!["internet traffic cross section"](https://raw.githubusercontent.com/phil8192/kaggle-santa-2019/master/solutions/69042.01.jpg "assigned days") 

* 69101.38 [(114/1,620)](https://www.kaggle.com/edgecrusher/competitions) (< 0.31% of optimal lower bound)
* 69042.01 (104) - post competition deadline