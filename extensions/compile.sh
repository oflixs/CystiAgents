#! /bin/bash
echo ------ compiling ----------------------

javac -cp "../allJar/*" -Xmaxerrs 5 -d .  *.java

echo ...

echo ------ end compilation ----------------
echo 

#java -cp "./;../allJar/*" extensions.Extensions extensionsOutcomes.params
#java -cp "./;../allJar/*" extensions.Extensions extensions.params
#java -cp "./;../allJar/*" extensions.Extensions 

