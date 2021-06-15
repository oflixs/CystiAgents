#! /bin/tcsh
# Set MASON_HOME to the 'mason' directory
#setenv MASON_HOME       ${0:h}/..
#setenv MASON_HOME       /home/oflixs/Dropbox/Research/AG_Galapagos/Simulations/mason
#You have only to change next variable:
#setenv MASON_PATH /media/Backups/Dropbox_ubuntu/Dropbox/Research/AG_Galapagos/Simulations/mason/

cd ../

echo ------ compiling ----------------------

~/jdk1.8.0_131/bin/javac -cp "../allJar/*" -Xmaxerrs 5 -d .  *.java

echo ...

echo ------ end compilation ----------------
echo 

#~/jdk1.8.0_131/bin/java -cp "./:../allJar/*" extensions.Extensions extensionsOutcomes.params
~/jdk1.8.0_131/bin/java -cp "./:../allJar/*" -Xms2500m -Xmx2500m  extensions.Extensions extensionsABCAnalysis.params
#~/jdk1.8.0_131/bin/java -cp "./;../allJar/*" extensions.Extensions 

