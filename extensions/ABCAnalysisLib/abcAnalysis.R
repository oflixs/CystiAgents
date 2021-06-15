###ABC R analysis ====================================================

library(devtools)
#library(abc)
library(plotrix)
library(ggplot2)
library(cowplot)
library(ggpubr)

#1.1: Input settings
setwd("./")
getwd()

load_all('../../../ABCAnalysisLib/abc-masterObs/R')

params<-read.csv("parametersMasonCSV.csv",header=TRUE)
results<-read.csv("resultsMasonCSV.csv",header=TRUE)
targets<-read.csv("targetsMasonCSV.csv",header=TRUE)
popFact<-read.csv("popFact.csv",header=TRUE)

tol<-0.00015

#load_all('../../../ABCAnalysisLib/abc-masterObs/R')
#abc.rej.1<-abc(target=targets,param=params,sumstat=results
#               ,popFact=popFact,method="rejection", tol=tol)

#ABC algorithm
load_all('../../../ABCAnalysisLib/abc-masterObs/R')
abc.rej.1<-abc(target=targets,param=params,sumstat=results
               ,popFact=popFact,method="rejection", tol=tol)
abc.lin.1<-abc(target=targets,param=params,sumstat=results
               ,popFact=popFact,method="loclinear", tol=tol)
abc.net.1<-abc(target=targets,param=params,sumstat=results
               ,popFact=popFact,method="neuralnet", tol=tol)

abc.rej.1$unadj.values
abc.rej.1$dist
sort(abc.rej.1$dist)


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
cv.abc1<-cv4abc(param=params,sumstat=results,popFact=popFact,nval=100,tols=c(0.00015)
                ,method="rejection")
summary(cv.abc1)
plot(cv.abc1, caption="Rejection")
cv.abc1$estim$

#plot cross-correlation graphs
data<-data.frame(x=cv.abc1$true$pHumanCyst, y=cv.abc1$estim$`tol0.00015`[,1])
#colnames(data)<-NULL
data

#with plot
#plot(data, main = "pHumanCyst", sub = "",
#      xlab = "true values", ylab = "estimated values",
#      cex.main = 2,   font.main= 4, col.main= "black",
#      cex.sub = 0.75, font.sub = 3, col.sub = "green",
#      col.lab ="black",
#     cex.lab=1.2, cex.axis=1.2, cex.main=1.2, cex.sub=1.2
#)
#abline(coef = c(0,1))

#with ggplot
#plot cross-correlation graphs

#----pHumanCysts ------------------------------
data<-data.frame(x=cv.abc1$true$pHumanCyst, y=cv.abc1$estim$`tol0.00015`[,1])
#colnames(data)<-NULL
data
p1<-ggplot(data, aes(x=x, y=y)) + 
  geom_point(size=2) + 
  geom_abline(intercept = 0, slope = 1, size=1.5) + 
  ggtitle("pHumanCysts") +
  theme(
    plot.title = element_text(color="black", size=12),
    axis.title = element_blank(),
    panel.background = element_rect(fill = "white", colour = "white",
                                    size = 0, linetype = "solid"),
    
    panel.grid.major.x = element_blank(),
    panel.grid.minor.x = element_blank(),
    
    panel.grid.major.y = element_blank(),
    panel.grid.minor.y = element_blank(),
    #panel.grid.major.x = element_line(colour = "black"),
    #panel.border = element_blank(),
    #axis.ticks.y = element_blank(),
    #axis.text=element_text(size=15),
    axis.line = element_line(colour = "black", 
                            size = 1, linetype = "solid"),
    axis.text.x = element_text(face="bold", color="black", 
                               size=10,angle=0, margin=margin(0.3, 0.3, 0.3, 0.3, "cm")),
    axis.text.y = element_text(face="bold", color="black", 
                               size=10, angle=90, margin=margin(b=0.3, 0.3 , 0.3, 0.3, "cm")),
    axis.title.x=element_blank()
    
    #axis.text.x = element_blank(),
    #axis.text.y = element_blank()
  ) +
  #labs(
  #  x = "true parameter values",
  #  y = "estimated parameter values",
  #  colour = "Cylinders",
  #  shape = "Transmission"
  #) +
  #theme(axis.title.y = element_text(size = rel(2.2), angle = 90, face = "bold")) +
  #theme(axis.title.x = element_text(size = rel(2.2), angle = 0, face = "bold"))
  #scale_y_continuous(breaks = round(seq(, 1.0, by = 0.1),1)) 
  #ylim(-2.0, 2.0);
scale_x_continuous(n.breaks = 3) +
scale_y_continuous(n.breaks =3, expand = expansion(mult = c(0, 0.3))) +
stat_cor(method = "pearson", label.x = -Inf, label.y = +Inf,
         vjust = 1, hjust= -0.1) 

p1


#----pigProglotInf ------------------------------
data<-data.frame(x=cv.abc1$true$pigProglotInf, y=cv.abc1$estim$`tol0.00015`[,2])
#colnames(data)<-NULL
data
p2<-ggplot(data, aes(x=x, y=y)) + 
  geom_point(size=2) + 
  geom_abline(intercept = 0, slope = 1, size=1.5) + 
  ggtitle("pigProglotInf") +
  theme(
    plot.title = element_text(color="black", size=12),
    axis.title = element_blank(),
    panel.background = element_rect(fill = "white", colour = "white",
                                    size = 0, linetype = "solid"),
    
    panel.grid.major.x = element_blank(),
    panel.grid.minor.x = element_blank(),
    
    panel.grid.major.y = element_blank(),
    panel.grid.minor.y = element_blank(),
    #panel.grid.major.x = element_line(colour = "black"),
    #panel.border = element_blank(),
    #axis.ticks.y = element_blank(),
    #axis.text=element_text(size=15),
    axis.line = element_line(colour = "black", 
                            size = 1, linetype = "solid"),
    axis.text.x = element_text(face="bold", color="black", 
                               size=10,angle=0, margin=margin(0.3, 0.3, 0.3, 0.3, "cm")),
    axis.text.y = element_text(face="bold", color="black", 
                               size=10, angle=90, margin=margin(b=0.3, 0.3 , 0.3, 0.3, "cm")),
    axis.title.x=element_blank()
    
    #axis.text.x = element_blank(),
    #axis.text.y = element_blank()
  ) +
  #labs(
  #  x = "true parameter values",
  #  y = "estimated parameter values",
  #  colour = "Cylinders",
  #  shape = "Transmission"
  #) +
  #theme(axis.title.y = element_text(size = rel(2.2), angle = 90, face = "bold")) +
  #theme(axis.title.x = element_text(size = rel(2.2), angle = 0, face = "bold"))
  #scale_y_continuous(breaks = round(seq(, 1.0, by = 0.1),1)) 
  #ylim(-2.0, 2.0);
scale_x_continuous(n.breaks = 3) +
scale_y_continuous(n.breaks =3, expand = expansion(mult = c(0, 0.3))) +
stat_cor(method = "pearson", label.x = -Inf, label.y = +Inf,
         vjust = 1, hjust= -0.1)

#----pigProglotInf ------------------------------
data<-data.frame(x=cv.abc1$true$pigEggsInf, y=cv.abc1$estim$`tol0.00015`[,3])
#colnames(data)<-NULL
data
p3<-ggplot(data, aes(x=x, y=y)) + 
  geom_point(size=2) + 
  geom_abline(intercept = 0, slope = 1, size=1.5) + 
  ggtitle("pigEggsInf") +
  theme(
    plot.title = element_text(color="black", size=12),
    axis.title = element_blank(),
    panel.background = element_rect(fill = "white", colour = "white",
                                    size = 0, linetype = "solid"),
    
    panel.grid.major.x = element_blank(),
    panel.grid.minor.x = element_blank(),
    
    panel.grid.major.y = element_blank(),
    panel.grid.minor.y = element_blank(),
    #panel.grid.major.x = element_line(colour = "black"),
    #panel.border = element_blank(),
    #axis.ticks.y = element_blank(),
    #axis.text=element_text(size=15),
    axis.line = element_line(colour = "black", 
                            size = 1, linetype = "solid"),
    axis.text.x = element_text(face="bold", color="black", 
                               size=10,angle=0, margin=margin(0.3, 0.3, 0.3, 0.3, "cm")),
    axis.text.y = element_text(face="bold", color="black", 
                               size=10, angle=90, margin=margin(b=0.3, 0.3 , 0.3, 0.3, "cm")),
    axis.title.x=element_blank()
    
    #axis.text.x = element_blank(),
    #axis.text.y = element_blank()
  ) +
  #labs(
  #  x = "true parameter values",
  #  y = "estimated parameter values",
  #  colour = "Cylinders",
  #  shape = "Transmission"
  #) +
  #theme(axis.title.y = element_text(size = rel(2.2), angle = 90, face = "bold")) +
  #theme(axis.title.x = element_text(size = rel(2.2), angle = 0, face = "bold"))
  #scale_y_continuous(breaks = round(seq(, 1.0, by = 0.1),1)) 
  #ylim(-2.0, 2.0);
scale_x_continuous(n.breaks = 3) +
scale_y_continuous(n.breaks =3, expand = expansion(mult = c(0, 0.3))) +
stat_cor(method = "pearson", label.x = -Inf, label.y = +Inf,
         vjust = 1, hjust= -0.1)


#----pigProglotInf -dherencetolatrineUse-----------------------------
data<-data.frame(x=cv.abc1$true$TTEMP_567_adherenceToLatrineUse, y=cv.abc1$estim$`tol0.00015`[,4])
#colnames(data)<-NULL
cv.abc1$true
p4<-ggplot(data, aes(x=x, y=y)) + 
  geom_point(size=2) + 
  geom_abline(intercept = 0, slope = 1, size=1.5) + 
  ggtitle("adherencetoLatrine 567") +
  theme(
    plot.title = element_text(color="black", size=12),
    axis.title = element_blank(),
    panel.background = element_rect(fill = "white", colour = "white",
                                    size = 0, linetype = "solid"),
    
    panel.grid.major.x = element_blank(),
    panel.grid.minor.x = element_blank(),
    
    panel.grid.major.y = element_blank(),
    panel.grid.minor.y = element_blank(),
    #panel.grid.major.x = element_line(colour = "black"),
    #panel.border = element_blank(),
    #axis.ticks.y = element_blank(),
    #axis.text=element_text(size=15),
    axis.line = element_line(colour = "black", 
                            size = 1, linetype = "solid"),
    axis.text.x = element_text(face="bold", color="black", 
                               size=10,angle=0, margin=margin(0.3, 0.3, 0.3, 0.3, "cm")),
    axis.text.y = element_text(face="bold", color="black", 
                               size=10, angle=90, margin=margin(b=0.3, 0.3 , 0.3, 0.3, "cm")),
    axis.title.x=element_blank()
    
    #axis.text.x = element_blank(),
    #axis.text.y = element_blank()
  ) +
  #labs(
  #  x = "true parameter values",
  #  y = "estimated parameter values",
  #  colour = "Cylinders",
  #  shape = "Transmission"
  #) +
  #theme(axis.title.y = element_text(size = rel(2.2), angle = 90, face = "bold")) +
  #theme(axis.title.x = element_text(size = rel(2.2), angle = 0, face = "bold"))
  #scale_y_continuous(breaks = round(seq(, 1.0, by = 0.1),1)) 
  #ylim(-2.0, 2.0);
scale_x_continuous(n.breaks = 3) +
scale_y_continuous(n.breaks =3, expand = expansion(mult = c(0, 0.3))) +
stat_cor(method = "pearson", label.x = -Inf, label.y = +Inf,
         vjust = 1, hjust= -0.1)


#----pigProglotInf -dherencetolatrineUse-----------------------------
data<-data.frame(x=cv.abc1$true$TTEMP_566_adherenceToLatrineUse, y=cv.abc1$estim$`tol0.00015`[,5])
#colnames(data)<-NULL
cv.abc1$true
p5<-ggplot(data, aes(x=x, y=y)) + 
  geom_point(size=2) + 
  geom_abline(intercept = 0, slope = 1, size=1.5) + 
  ggtitle("adherencetoLatrine 566") +
  theme(
    plot.title = element_text(color="black", size=12),
    axis.title = element_blank(),
    panel.background = element_rect(fill = "white", colour = "white",
                                    size = 0, linetype = "solid"),
    
    panel.grid.major.x = element_blank(),
    panel.grid.minor.x = element_blank(),
    
    panel.grid.major.y = element_blank(),
    panel.grid.minor.y = element_blank(),
    #panel.grid.major.x = element_line(colour = "black"),
    #panel.border = element_blank(),
    #axis.ticks.y = element_blank(),
    #axis.text=element_text(size=15),
    axis.line = element_line(colour = "black", 
                            size = 1, linetype = "solid"),
    axis.text.x = element_text(face="bold", color="black", 
                               size=10,angle=0, margin=margin(0.3, 0.3, 0.3, 0.3, "cm")),
    axis.text.y = element_text(face="bold", color="black", 
                               size=10, angle=90, margin=margin(b=0.3, 0.3 , 0.3, 0.3, "cm")),
    axis.title.x=element_blank()
    
    #axis.text.x = element_blank(),
    #axis.text.y = element_blank()
  ) +
  #labs(
  #  x = "true parameter values",
  #  y = "estimated parameter values",
  #  colour = "Cylinders",
  #  shape = "Transmission"
  #) +
  #theme(axis.title.y = element_text(size = rel(2.2), angle = 90, face = "bold")) +
  #theme(axis.title.x = element_text(size = rel(2.2), angle = 0, face = "bold"))
  #scale_y_continuous(breaks = round(seq(, 1.0, by = 0.1),1)) 
  #ylim(-2.0, 2.0);
scale_x_continuous(n.breaks = 3) +
scale_y_continuous(n.breaks =3, expand = expansion(mult = c(0, 0.3))) +
stat_cor(method = "pearson", label.x = -Inf, label.y = +Inf,
         vjust = 1, hjust= -0.1)


#----pigProglotInf -dherencetolatrineUse-----------------------------
data<-data.frame(x=cv.abc1$true$TTEMP_515_adherenceToLatrine, y=cv.abc1$estim$`tol0.00015`[,6])
#colnames(data)<-NULL
cv.abc1$true
p6<-ggplot(data, aes(x=x, y=y)) + 
  geom_point(size=2) + 
  geom_abline(intercept = 0, slope = 1, size=1.5) + 
  ggtitle("adherencetoLatrineUse 515") +
  theme(
    plot.title = element_text(color="black", size=12),
    axis.title = element_blank(),
    panel.background = element_rect(fill = "white", colour = "white",
                                    size = 0, linetype = "solid"),
    
    panel.grid.major.x = element_blank(),
    panel.grid.minor.x = element_blank(),
    
    panel.grid.major.y = element_blank(),
    panel.grid.minor.y = element_blank(),
    #panel.grid.major.x = element_line(colour = "black"),
    #panel.border = element_blank(),
    #axis.ticks.y = element_blank(),
    #axis.text=element_text(size=15),
    axis.line = element_line(colour = "black", 
                            size = 1, linetype = "solid"),
    axis.text.x = element_text(face="bold", color="black", 
                               size=10,angle=0, margin=margin(0.3, 0.3, 0.3, 0.3, "cm")),
    axis.text.y = element_text(face="bold", color="black", 
                               size=10, angle=90, margin=margin(b=0.3, 0.3 , 0.3, 0.3, "cm")),
    axis.title.x=element_blank()
    
    #axis.text.x = element_blank(),
    #axis.text.y = element_blank()
  ) +
  #labs(
  #  x = "true parameter values",
  #  y = "estimated parameter values",
  #  colour = "Cylinders",
  #  shape = "Transmission"
  #) +
  #theme(axis.title.y = element_text(size = rel(2.2), angle = 90, face = "bold")) +
  #theme(axis.title.x = element_text(size = rel(2.2), angle = 0, face = "bold"))
  #scale_y_continuous(breaks = round(seq(, 1.0, by = 0.1),1)) 
  #ylim(-2.0, 2.0);
scale_x_continuous(n.breaks = 3) +
scale_y_continuous(n.breaks =3, expand = expansion(mult = c(0, 0.3))) +
stat_cor(method = "pearson", label.x = -Inf, label.y = +Inf,
         vjust = 1, hjust= -0.1)


 
#draw the plot  cross - correlation----------------------------
pp<-plot_grid(p1, p2, p3, p6, p5, p4, ncol=3)
pp

ggdraw(pp)  +
draw_label("true parameter value", angle=90, x=0, size=22) +
draw_label("estimated parameter value", y=0, size=22) +
  theme(
      plot.margin = margin(1, 1, 1, 1, "cm")
    )  


###MANUALLY SELECT PRIOR DISTRIBUTION FOR FULL CALIBRATION###
#ABC results - posterior distributions
summary(abc.rej.1)
summary(abc.lin.1)
summary(abc.rej.1$unadj.values)
summary(abc.lin.1$unadj.values)


data1<-data.frame(y=abc.rej.1$unadj.values[,1], x="pHumanCyst")
data1
data2<-data.frame(y=abc.rej.1$unadj.values[,2], x="pigProglotInf")
data2
data3<-data.frame(y=abc.rej.1$unadj.values[,3], x="pigEggsInf")
data3
data4<-data.frame(y=abc.rej.1$unadj.values[,4], x="567 adh.ToLatrine")
data4
data5<-data.frame(y=abc.rej.1$unadj.values[,5], x="566 adh.ToLatrine")
data5
data6<-data.frame(y=abc.rej.1$unadj.values[,6], x="515 adh.ToLatrine")
data6

data<-rbind(data1, data2, data3, data6, data5, data4)
data

#-------------------------------------------------------------
gpp1 <- ggplot(data1, aes(x=x, y=y)) + 
  geom_boxplot()+ 
  theme(
    plot.title = element_text(color="black", size=12),
    axis.title = element_blank(),
    panel.background = element_rect(fill = "white", colour = "white",
                                    size = 0, linetype = "solid"),
    
    panel.grid.major.x = element_blank(),
    panel.grid.minor.x = element_blank(),
    
    panel.grid.major.y = element_blank(),
    panel.grid.minor.y = element_blank(),
    #panel.grid.major.x = element_line(colour = "black"),
    #panel.border = element_blank(),
    #axis.ticks.y = element_blank(),
    #axis.text=element_text(size=15),
    axis.line = element_line(colour = "black", 
                            size = 1, linetype = "solid"),
    axis.text.x = element_text(face="bold", color="black", 
                               size=18,angle=0, margin=margin(0.3, 0.3, 0.3, 0.3, "cm")),
    axis.text.y = element_text(face="bold", color="black", 
                               size=14, angle=90, margin=margin(b=0.3, 0.3 , 0.3, 0.3, "cm")),
    axis.title.x=element_blank()
    
    #axis.text.x = element_blank(),
    #axis.text.y = element_blank()
  ) +
scale_y_continuous(n.breaks =2, labels = scales::scientific)

#gpp
#gpp<- gpp + facet_wrap( ~x, scales="free")
#gpp


#-------------------------------------------------------------
gpp2 <- ggplot(data2, aes(x=x, y=y)) + 
  geom_boxplot()+ 
  theme(
    plot.title = element_text(color="black", size=12),
    axis.title = element_blank(),
    panel.background = element_rect(fill = "white", colour = "white",
                                    size = 0, linetype = "solid"),
    
    panel.grid.major.x = element_blank(),
    panel.grid.minor.x = element_blank(),
    
    panel.grid.major.y = element_blank(),
    panel.grid.minor.y = element_blank(),
    #panel.grid.major.x = element_line(colour = "black"),
    #panel.border = element_blank(),
    #axis.ticks.y = element_blank(),
    #axis.text=element_text(size=15),
    axis.line = element_line(colour = "black", 
                            size = 1, linetype = "solid"),
    axis.text.x = element_text(face="bold", color="black", 
                               size=18,angle=0, margin=margin(0.3, 0.3, 0.3, 0.3, "cm")),
    axis.text.y = element_text(face="bold", color="black", 
                               size=14, angle=90, margin=margin(b=0.3, 0.3 , 0.3, 0.3, "cm")),
    axis.title.x=element_blank()
    
    #axis.text.x = element_blank(),
    #axis.text.y = element_blank()
  ) +
scale_y_continuous(n.breaks =3)

#-------------------------------------------------------------
gpp3 <- ggplot(data3, aes(x=x, y=y)) + 
  geom_boxplot()+ 
  theme(
    plot.title = element_text(color="black", size=12),
    axis.title = element_blank(),
    panel.background = element_rect(fill = "white", colour = "white",
                                    size = 0, linetype = "solid"),
    
    panel.grid.major.x = element_blank(),
    panel.grid.minor.x = element_blank(),
    
    panel.grid.major.y = element_blank(),
    panel.grid.minor.y = element_blank(),
    #panel.grid.major.x = element_line(colour = "black"),
    #panel.border = element_blank(),
    #axis.ticks.y = element_blank(),
    #axis.text=element_text(size=15),
    axis.line = element_line(colour = "black", 
                            size = 1, linetype = "solid"),
    axis.text.x = element_text(face="bold", color="black", 
                               size=18,angle=0, margin=margin(0.3, 0.3, 0.3, 0.3, "cm")),
    axis.text.y = element_text(face="bold", color="black", 
                               size=14, angle=90, margin=margin(b=0.3, 0.3 , 0.3, 0.3, "cm")),
    axis.title.x=element_blank()
    
    #axis.text.x = element_blank(),
    #axis.text.y = element_blank()
  ) +
scale_y_continuous(n.breaks =3)

#-------------------------------------------------------------
gpp4 <- ggplot(data4, aes(x=x, y=y)) + 
  geom_boxplot()+ 
  theme(
    plot.title = element_text(color="black", size=12),
    axis.title = element_blank(),
    panel.background = element_rect(fill = "white", colour = "white",
                                    size = 0, linetype = "solid"),
    
    panel.grid.major.x = element_blank(),
    panel.grid.minor.x = element_blank(),
    
    panel.grid.major.y = element_blank(),
    panel.grid.minor.y = element_blank(),
    #panel.grid.major.x = element_line(colour = "black"),
    #panel.border = element_blank(),
    #axis.ticks.y = element_blank(),
    #axis.text=element_text(size=15),
    axis.line = element_line(colour = "black", 
                            size = 1, linetype = "solid"),
    axis.text.x = element_text(face="bold", color="black", 
                               size=18,angle=0, margin=margin(0.3, 0.3, 0.3, 0.3, "cm")),
    axis.text.y = element_text(face="bold", color="black", 
                               size=14, angle=90, margin=margin(b=0.3, 0.3 , 0.3, 0.3, "cm")),
    axis.title.x=element_blank()
    
    #axis.text.x = element_blank(),
    #axis.text.y = element_blank()
  ) +
scale_y_continuous(n.breaks =3)

#-------------------------------------------------------------
gpp5 <- ggplot(data5, aes(x=x, y=y)) + 
  geom_boxplot()+ 
  theme(
    plot.title = element_text(color="black", size=12),
    axis.title = element_blank(),
    panel.background = element_rect(fill = "white", colour = "white",
                                    size = 0, linetype = "solid"),
    
    panel.grid.major.x = element_blank(),
    panel.grid.minor.x = element_blank(),
    
    panel.grid.major.y = element_blank(),
    panel.grid.minor.y = element_blank(),
    #panel.grid.major.x = element_line(colour = "black"),
    #panel.border = element_blank(),
    #axis.ticks.y = element_blank(),
    #axis.text=element_text(size=15),
    axis.line = element_line(colour = "black", 
                            size = 1, linetype = "solid"),
    axis.text.x = element_text(face="bold", color="black", 
                               size=18,angle=0, margin=margin(0.3, 0.3, 0.3, 0.3, "cm")),
    axis.text.y = element_text(face="bold", color="black", 
                               size=14, angle=90, margin=margin(b=0.3, 0.3 , 0.3, 0.3, "cm")),
    axis.title.x=element_blank()
    
    #axis.text.x = element_blank(),
    #axis.text.y = element_blank()
  ) +
scale_y_continuous(n.breaks =3)

#-------------------------------------------------------------
gpp6 <- ggplot(data6, aes(x=x, y=y)) + 
  geom_boxplot()+ 
  theme(
    plot.title = element_text(color="black", size=12),
    axis.title = element_blank(),
    panel.background = element_rect(fill = "white", colour = "white",
                                    size = 0, linetype = "solid"),
    
    panel.grid.major.x = element_blank(),
    panel.grid.minor.x = element_blank(),
    
    panel.grid.major.y = element_blank(),
    panel.grid.minor.y = element_blank(),
    #panel.grid.major.x = element_line(colour = "black"),
    #panel.border = element_blank(),
    #axis.ticks.y = element_blank(),
    #axis.text=element_text(size=15),
    axis.line = element_line(colour = "black", 
                            size = 1, linetype = "solid"),
    axis.text.x = element_text(face="bold", color="black", 
                               size=18,angle=0, margin=margin(0.3, 0.3, 0.3, 0.3, "cm")),
    axis.text.y = element_text(face="bold", color="black", 
                               size=14, angle=90, margin=margin(b=0.3, 0.3 , 0.3, 0.3, "cm")),
    axis.title.x=element_blank()
    
    #axis.text.x = element_blank(),
    #axis.text.y = element_blank()
  ) +
scale_y_continuous(n.breaks =3)


#------------------------------------------
ppPost<-plot_grid(gpp1, gpp2, gpp3, gpp6, gpp5, gpp4, ncol=3)
ppPost



pHumanCyst<-boxplot(abc.rej.1$unadj.values[,1])
pProglot<-boxplot(abc.rej.1$unadj.values[,2])
pEggs<-boxplot(abc.rej.1$unadj.values[,3])
p567<-boxplot(abc.rej.1$unadj.values[,4])
p566<-boxplot(abc.rej.1$unadj.values[,5])
p515<-boxplot(abc.rej.1$unadj.values[,6])

plot_grid(pHumanCyst, pProglot, pEggs, p515, p566, p567, ncol=3)

abc.rej.1$unadj.values

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


##################

#Save
#getwd()
#setwd(paste0("/home/pray/Transfer files/Calibration/",villa))
#finalparms$lightsero<-parmmeds3[1]
#finalparms$heavysero<-parmmeds3[2]
#write.csv(finalparms,paste0("FinalValues_sero",villa,".csv"),row.names=F)
#save.image(paste0("Calibration_sero",villa,".rdata"))
save.image(file="savedimage.rdata")



############################################################################

