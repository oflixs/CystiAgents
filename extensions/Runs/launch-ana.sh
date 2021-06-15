#!/bin/bash

   sbatch -J ana --open-mode=truncate -o ./Outs/ana.out -e ./Outs/ana.err  test-ana.sh

