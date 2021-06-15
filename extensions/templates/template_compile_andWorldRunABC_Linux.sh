#! /bin/bash

#To compile and run the CystiAgents Agent-Based Model in Mason

#echo ------ compiling ----------------------
#echo ......
##This to compile
#	~/jdk1.8.0_131/bin/javac -classpath './allJar/*' -Xmaxerrs 5 -d . *.java
#echo ------ end compiling ------------------

#This to run
~/jdk1.8.0_131/bin/java -classpath './:./allJar/*' sim.app.cystiagents.CystiAgentsWorld simName timeMark
