###ABC R analysis ====================================================

library(abc)
library(plotrix)

#1.1: Input settings
setwd("./")
#getwd()

params<-read.csv("parametersMasonCSV.csv",header=TRUE)
results<-read.csv("resultsMasonCSV.csv",header=TRUE)
targets<-read.csv("targetsMasonCSV.csv",header=TRUE)

tol<-0.0003

#targets
#results 
#params


#ABC algorithm
#abc.rej.2<-abc(target=outcomes,param=p2,sumstat=res2,method="rejection",tol=tol)
#abc.lin.2<-abc(target=outcomes,param=p2,sumstat=res2,method="ridge",tol=tol)
#abc.net.2<-abc(target=outcomes,param=p2,sumstat=res2,method="neuralnet",tol=tol)

abc.rej.1<-abc(target=targets,param=params,sumstat=results,method="rejection",tol=tol)
abc.lin.1<-abc(target=targets,param=params,sumstat=results,method="loclinear",tol=tol)
abc.net.1<-abc(target=targets,param=params,sumstat=results,method="neuralnet",tol=tol)

#Plot results
#Outcomes
targets

results
for (i in names(results)){
  #print(results[, i])
  hist(results[, i],main=i)
  abline(v=targets[i],lwd=2,col="red",lty=6)
}

#hist(results$TTEMP_515_tn,main="Distribution of outcome values (Human taeniasis)")
#abline(v=outcomes[1],lwd=2,col="red",lty=6)
#abline(v=outcomes[2],lwd=2,col="red",lty=6)
#hist(res1$LIGHT,main="Distribution of outcome values (Pig light infection)")
#abline(v=outcomes[3],lwd=2,col="red",lty=6)
#hist(res1$HEAVY,main="Distribution of outcome values (Pig heavy infection)")
#abline(v=outcomes[4],lwd=2,col="red",lty=6)

hist(abc.rej.1)
plot(abc.lin.1,param=params)

#Cross-validation to assess parameter prediction
cv.abc1<-cv4abc(param=params,sumstat=results,nval=100,tols=c(0.0003),method="rejection")
summary(cv.abc1)
plot(cv.abc1, caption="Rejection")
cv.abc1

###MANUALLY SELECT PRIOR DISTRIBUTION FOR FULL CALIBRATION###
#ABC results - posterior distributions
summary(abc.rej.1)
summary(abc.lin.1)
summary(abc.rej.1$unadj.values)
summary(abc.lin.1$unadj.values)

#----------- last graphs

results
targets
#Plot results
#Outcomes
#TN ----------------------
hist(results$TTEMP_515_tn,main="Distribution of outcome values (Human taeniasis, village 515)")
abline(v=targets$TTEMP_515_tn,lwd=2,col="red",lty=6)
#abline(v=outcomes[2],lwd=2,col="red",lty=6)

hist(results$TTEMP_566_tn,main="Distribution of outcome values (Human taeniasis, village 566)")
abline(v=targets$TTEMP_566_tn,lwd=2,col="red",lty=6)

hist(results$TTEMP_567_tn,main="Distribution of outcome values (Human taeniasis, village 567)")
abline(v=targets$TTEMP_567_tn,lwd=2,col="red",lty=6)

#PC ----------------------------------
hist(results$TTEMP_515_cysti,main="Distribution of outcome values (Pig cysticercosys village 515)")
abline(v=targets$TTEMP_515_cysti,lwd=2,col="red",lty=6)
#abline(v=outcomes[4],lwd=2,col="red",lty=6)

hist(results$TTEMP_566_cysti,main="Distribution of outcome values (Pig cysticercosys village 566)")
abline(v=targets$TTEMP_566_cysti,lwd=2,col="red",lty=6)

hist(results$TTEMP_567_cysti,main="Distribution of outcome values (Pig cysticercosys village 567)")
abline(v=targets$TTEMP_567_cysti,lwd=2,col="red",lty=6)

hist(abc.rej.1)
plot(abc.lin.1,param=params)


#2.7: post-prediction check
#ABC results - posterior distributions
summary(abc.rej.1)
summary(abc.lin.1)
summary(abc.net.1)
summary(abc.rej.1$unadj.values)
summary(abc.lin.1$unadj.values)

accepted<-abc.rej.1$unadj.values
n2post<-500

#Save
#getwd()
#dir.create(paste0("/home/pray/Transfer files/Calibration/",villa))
#setwd(paste0("/home/pray/Transfer files/Calibration/",villa))
#write.csv(values2,paste0("Calibration_Priors_",villa,".csv"),row.names=F)
#finalparms<-list("lightinf"=parmmeds[1],
#                 "heavyinf"=parmmeds[2],
#                 "pl2h"=parmmeds[3],
#                 "ph2h"=fixed[4],
#                 "lightall"=fixed[5],
#                 "heavyall"=fixed[6])
#write.csv(finalparms,paste0("FinalValues_",villa,".csv"),row.names=F)
#save.image(paste0("Calibration_",villa,".rdata"))


############################################################################

#Step 3: Calibrate serological parameters
library(abc)
library(randtoolbox)
library(parallel)
library(doParallel)

#3.1: Input settings
villa<-580
n3<-10000
tol<-0.0003

#3.2: Import outcome targets 
setwd("/home/pray/Transfer files/Calibration/")
seroincidence<-read.csv("R01_Seroincidence.csv",header=TRUE)
sero<-seroincidence$r1[seroincidence$villa==villa]
seroll<-seroincidence$r1_ll[seroincidence$villa==villa]
seroul<-seroincidence$r1_ul[seroincidence$villa==villa]

#3.3: Other parameters
setwd(paste0("/home/pray/Transfer files/Calibration/",villa))
finalparms<-read.csv(paste0("FinalValues_",villa,".csv"))

#3.4: Prior distribution
Parameter<-c("light-sero","heavy-sero")
LL<-c(0,0)
UL<-c(0.5,0.5)
values3<-data.frame(Parameter,LL,UL)
k<-nrow(values3)
values3$range<-values3$UL-values3$LL

#3.5: Sample parameter distribution (priors_villa)
x<-randtoolbox::sobol(n=n3, dim=k, seed=2025)
parms3<-matrix(nrow=n3, ncol=k)
for (i in 1:k){
  parms3[,i]<-(x[,i]/(1/values3$range[i]))+values3$LL[i]
}

#3.6: Set up functions
#Start-up function
prepro<-function(dummy,gui,nl.path,model.path,nl.jarname){
  library(RNetLogo)
  options(java.parameters = "-Xmx256m")
  NLStart(nl.path=nl.path,gui=gui,nl.jarname = nl.jarname)
  NLLoadModel(model.path)
}

#Stop function
postpro<-function(x){
  NLQuit()
}

#3.7: Run model 
#Set up cluster
processors<-detectCores()
cl<-makeCluster(processors)
registerDoParallel(cl)

#Open model
invisible(parLapply(cl,1:processors,prepro,gui=F,
                    nl.path<-"/home/pray/NetLogo 6.0.4/app",
                    model.path<-"/home/pray/Transfer files/Calibration/CystiAgent_Calibration_AWS_sero.nlogo",
                    nl.jarname = "netlogo-6.0.4.jar"))

#Export objects to the cluster
clusterExport(cl=cl, varlist=c("n3", "villa","parms3","finalparms"))

#Run model
results3<-foreach(i=1:n3, .combine='c', .packages='RNetLogo') %dopar%{
  NLCommand("clear-all")
  NLCommand("set dataset", villa)
  NLCommand("set light-inf", finalparms$lightinf)
  NLCommand("set heavy-inf", finalparms$heavyinf)
  NLCommand("set pl2h", finalparms$pl2h)
  NLCommand("set ph2h", finalparms$ph2h)
  NLCommand("set light-all", finalparms$lightall)
  NLCommand("set heavy-all", finalparms$heavyall)
  NLCommand("set light-sero",parms3[i,1])
  NLCommand("set heavy-sero",parms3[i,2])
  NLCommand("setup")
  NLDoCommand(1001,"go")
  unlist(NLReport(c("sero500","sero600","sero700","sero800","sero900","sero1000")))
}

#Shut down cluster
invisible(parLapply(cl,1:processors,postpro))
stopCluster(cl)

#3.7: Analyze R3 results
#Consolidate results
resultsX3<-matrix(results3,nrow=n3,ncol=6,byrow=T) 
results_sum3<-(resultsX3[,1]+resultsX3[,2]+resultsX3[,3]+resultsX3[,4]+resultsX3[,5]+resultsX3[,6])/6

res3<-data.frame(results_sum3)
colnames(res3)<-c("SEROINC")

p3<-data.frame(parms3)
colnames(p3)<-c("light-sero","heavy-sero")

#ABC algorithm
abc.rej.3<-abc(target=sero,param=p3,sumstat=res3,method="rejection",tol=tol)
abc.lin.3<-abc(target=sero,param=p3,sumstat=res3,method="ridge",tol=tol)
abc.net.3<-abc(target=sero,param=p3,sumstat=res3,method="neuralnet",tol=tol)

resultsSS3<-resultsX3[abc.rej.3$region==T,]

#Plot results
#Outcomes
hist(res3$SEROINC,main="Distribution of outcome values (Pig Seroincidence)")
abline(v=sero,lwd=2,col="red",lty=6)

hist(abc.rej.3)
plot(abc.lin.3,param=p3)

#SEROINCIDENCE
plotCI(c(500,600,700,800,900,1000),rep(sero,6),
       ylim=c(0,0.8),xlim=c(500,1000),
       ui=rep(seroul,6),
       li=rep(seroll,6),lwd=3,pch=NA)
for (i in seq(1,n3,by=10)){
  lines(c(500,600,700,800,900,1000),resultsX3[i,1:6],col="green",lwd=1,lty=3)
}
for (i in 1:(n3*tol)){
  lines(c(500,600,700,800,900,1000),resultsSS3[i,1:6],col="darkgreen",lwd=1,lty=3)
}
lines(c(500,600,700,800,900,1000),c(median(resultsSS3[,1]),
                                    median(resultsSS3[,2]),
                                    median(resultsSS3[,3]),
                                    median(resultsSS3[,4]),
                                    median(resultsSS3[,5]),
                                    median(resultsSS3[,6])),col='red',lwd=4)
lines(c(500,600,700,800,900,1000),c(quantile(resultsSS3[,1],probs=0.95),
                                    quantile(resultsSS3[,2],probs=0.95),
                                    quantile(resultsSS3[,3],probs=0.95),
                                    quantile(resultsSS3[,4],probs=0.95),
                                    quantile(resultsSS3[,5],probs=0.95),
                                    quantile(resultsSS3[,6],probs=0.95)),col='red',lwd=3,lty=2)
lines(c(500,600,700,800,900,1000),c(quantile(resultsSS3[,1],probs=0.05),
                                    quantile(resultsSS3[,2],probs=0.05),
                                    quantile(resultsSS3[,3],probs=0.05),
                                    quantile(resultsSS3[,4],probs=0.05),
                                    quantile(resultsSS3[,5],probs=0.05),
                                    quantile(resultsSS3[,6],probs=0.05)),col='red',lwd=3,lty=2)
plotCI(c(500,600,700,800,900,1000),rep(sero,6),
       ylim=c(0,0.8),xlim=c(500,1000),
       ui=rep(seroul,6),
       li=rep(seroll,6),lwd=3,pch=NA,add=T)
abline(a=sero,b=0,lwd=4, col="black",lty=4)

#3.8: post-prediction check
#ABC results - posterior distributions
summary(abc.rej.3)
summary(abc.lin.3)
summary(abc.rej.3$unadj.values)
summary(abc.lin.3$unadj.values)

accepted<-abc.rej.3$unadj.values
n3post<-500

#Set parameter values based on medians of posteriors
parmmeds3<-numeric(0)
for (i in 1:k){
  parmmeds[i]<-median(accepted[,i])
}

#Optional manual entry
parmmeds<-c(NA)

#Run model 
#Set up cluster
processors<-detectCores()
cl<-makeCluster(processors)
registerDoParallel(cl)

#Open model
invisible(parLapply(cl,1:processors,prepro,gui=F,
                    nl.path<-"/home/pray/NetLogo 6.0.4/app",
                    model.path<-"/home/pray/Transfer files/Calibration/CystiAgent_Calibration_AWS_sero.nlogo",
                    nl.jarname = "netlogo-6.0.4.jar"))

#Export objects to the cluster
clusterExport(cl=cl, varlist=c("n3post", "villa","parmmeds3","finalparms"))

#Run model
results3post<-foreach(i=1:n3post, .combine='c', .packages='RNetLogo') %dopar%{
  NLCommand("clear-all")
  NLCommand("set dataset", villa)
  NLCommand("set light-inf", finalparms$lightinf)
  NLCommand("set heavy-inf", finalparms$heavyinf)
  NLCommand("set pl2h", finalparms$pl2h)
  NLCommand("set ph2h", finalparms$ph2h)
  NLCommand("set light-all", finalparms$lightall)
  NLCommand("set heavy-all", finalparms$heavyall)
  NLCommand("set light-sero",parmmeds3[1])
  NLCommand("set heavy-sero",parmmeds3[2])
  NLCommand("setup")
  NLDoCommand(1001,"go")
  unlist(NLReport(c("sero500","sero600","sero700","sero800","sero900","sero1000")))
}

#Shut down cluster
invisible(parLapply(cl,1:processors,postpro))
stopCluster(cl)

#Consolidate results
resultsX3post<-matrix(results3post,nrow=n3post,ncol=6,byrow=T)

#Plots results of posterior check

#SEROINCIDENCE
plotCI(c(500,600,700,800,900,1000),rep(sero,6),
       ylim=c(0,0.8),xlim=c(500,1000),
       ui=rep(seroul,6),
       li=rep(seroll,6),lwd=3,pch=NA)
for (i in 1:n3post){
  lines(c(500,600,700,800,900,1000),resultsX3post[i,1:6],col="green",lwd=1,lty=3)
}
lines(c(500,600,700,800,900,1000),c(median(resultsX3post[,1]),
                                    median(resultsX3post[,2]),
                                    median(resultsX3post[,3]),
                                    median(resultsX3post[,4]),
                                    median(resultsX3post[,5]),
                                    median(resultsX3post[,6])),col='darkgreen',lwd=4)
lines(c(500,600,700,800,900,1000),c(quantile(resultsX3post[,1],probs=0.95),
                                    quantile(resultsX3post[,2],probs=0.95),
                                    quantile(resultsX3post[,3],probs=0.95),
                                    quantile(resultsX3post[,4],probs=0.95),
                                    quantile(resultsX3post[,5],probs=0.95),
                                    quantile(resultsX3post[,6],probs=0.95)),col='darkgreen',lwd=3,lty=2)
lines(c(500,600,700,800,900,1000),c(quantile(resultsX3post[,1],probs=0.05),
                                    quantile(resultsX3post[,2],probs=0.05),
                                    quantile(resultsX3post[,3],probs=0.05),
                                    quantile(resultsX3post[,4],probs=0.05),
                                    quantile(resultsX3post[,5],probs=0.05),
                                    quantile(resultsX3post[,6],probs=0.05)),col='darkgreen',lwd=3,lty=2)
plotCI(c(500,600,700,800,900,1000),rep(sero,6),
       ylim=c(0,0.2),xlim=c(500,1000),
       ui=rep(seroul,6),
       li=rep(seroll,6),lwd=3,pch=NA,add=T)
abline(a=sero,b=0,lwd=4, col="black",lty=6)


##################

#Save
getwd()
setwd(paste0("/home/pray/Transfer files/Calibration/",villa))
finalparms$lightsero<-parmmeds3[1]
finalparms$heavysero<-parmmeds3[2]
write.csv(finalparms,paste0("FinalValues_sero",villa,".csv"),row.names=F)
save.image(paste0("Calibration_sero",villa,".rdata"))


############################################################################

