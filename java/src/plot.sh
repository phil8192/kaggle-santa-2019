#!/bin/bash
for i in $(ls ../../solutions/*.csv |tac); do Rscript plot.R "$i" ;done
