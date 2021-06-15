#! /bin/bash

#To compile and run the CystiAgents Agent-Based Model in Mason

echo ------ compiling ----------------------
echo ......
#This to compile
#javac -cp "./allJar/*" -Xmaxerrs 5 -d .  *.java
echo ------ end compiling ------------------

#This to run
java -cp "./;./allJar/*" sim.app.cystiagents.CystiAgentsWorld simName
