#! /bin/bash

#To compile and run the CystiAgents Agent-Based Model in Mason

#This to compile
#make all --directory ../../../../../../

#This to run
#java -cp "../../../../../../target/classes;../../../../../../allJar/*" sim.app.cystiagents.CystiAgentsWorld simName timeMark
java -cp "./;./allJar/*" sim.app.cystiagents.CystiAgentsWorld simName timeMark
