#!/bin/bash

#---------------------------
#compile core ABM
cd ../../

~/jdk1.8.0_131/bin/javac -cp "./allJar/*" -Xmaxerrs 5 -d .  *.java

#---------------------------
#comile extensions
cd extensions

~/jdk1.8.0_131/bin/javac -cp "../allJar/*" -Xmaxerrs 5 -d .  *.java

cd Runs

#---------------------------
#Remove old out files
rm -v ./Outs/*

#---------------------------
#Run extensions loop
for i in {0..1000}
#for i in {0..600}
#for i in {601..999}
#for i in {1000..1500}
do export i

#   if [ ! -d ../outputs/ITNtoHotspots1/ ]; then
#      echo dir ITNtoHotspots1 created from script
#      mkdir -p ../outputs/ITNtoHotspots1/
#   fi

   sbatch -J t$i --open-mode=truncate -o ./Outs/run-$i.out -e ./Outs/run-$i.err  test-runs.sh

   sleep 3

done
