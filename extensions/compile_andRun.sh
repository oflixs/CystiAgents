#! /bin/bash
echo ------ compiling ----------------------

cd ../

javac -cp "./allJar/*" -Xmaxerrs 5 -d .  *.java

cd extensions

javac -cp "../allJar/*" -Xmaxerrs 5 -d .  *.java

echo ...

echo ------ end compilation ----------------
echo 

#java -cp "./;../allJar/*" extensions.Extensions extensionsSensitivityAnalysis.params
#java -cp "./;../allJar/*" extensions.Extensions extensionsOutcomes.params
java -cp "./;../allJar/*" extensions.Extensions extensionsOutcomes_R01.params
#java -cp "./;../allJar/*" extensions.Extensions extensionsOutcomes_TTEMP.params
#java -cp "./;../allJar/*" extensions.Extensions extensionsOutcomesCystiHumans.params

#java -cp "./;../allJar/*" extensions.Extensions extensionsABC_TTEMP.params
#java -cp "./;../allJar/*" extensions.Extensions extensionsABC_R01.params
#java -cp "./;../allJar/*" extensions.Extensions ABCCystiHumans.params
#java -cp "./;../allJar/*" extensions.Extensions extensionsABCAnalysis.params

