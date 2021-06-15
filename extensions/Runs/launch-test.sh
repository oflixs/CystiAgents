#!/bin/bash

rm -v ./Outs/*

for i in {0..350}
do export i

#   if [ ! -d ../outputs/ITNtoHotspots1/ ]; then
#      echo dir ITNtoHotspots1 created from script
#      mkdir -p ../outputs/ITNtoHotspots1/
#   fi

   sbatch -J t$i --open-mode=truncate -o ./Outs/test-$i.out -e ./Outs/test-$i.err  test-test.sh

   sleep 10

   #sbatch -J test test.sh 

done
