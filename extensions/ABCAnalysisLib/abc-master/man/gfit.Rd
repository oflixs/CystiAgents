\name{gfit}
\alias{gfit}
\title{
  Goodness of fit
}
\description{

  Perform a test for goodness-of-fit.
 
}
\usage{
gfit(target, sumstat, nb.replicate, tol=.01, statistic=mean, subset=NULL, 
trace=FALSE)
}
\arguments{
  \item{target}{
    a data frame or vector of the observed summary statistic.}   
  \item{sumstat}{
    a vector, matrix or data frame of the simulated summary statistics.}
  \item{nb.replicate}{
    number of replicates used to estimate the null distribution of the 
    goodness-of-fit statistic.}
  \item{tol}{
    a tolerance rate. Defaults to 0.01 }
  \item{statistic}{
    define the goodness-of-fit statistic. Typical values are \code{median} 
    (default), \code{mean}, or \code{max}. When using \code{median} fo 
    instance, the g.o.f. statistic is the median of the distance between 
    observed and accepted summary statistics.}
  \item{subset}{
    optional. A logical expression indicating elements or rows to keep. 
    Missing values in \code{sumstat} are taken as \code{FALSE}.}
  \item{trace}{
    a boolean indicating if a trace should be displayed when calling the 
    function. Default to \code{FALSE}.}
    
}
\details{
  
  The null distribution is estimated using already performed simulations 
  contained in \code{sumstat} as pseudo-observed datasets. For each 
  pseudo-observed dataset, the rejection algorithm is performed to obtain 
  a value of the goodness-of-fit statistic. A better estimate of the 
  P-value is obtained for larger \code{nb.replicate} but the running time
  of the function is increased. 

}
\value{
  An object of class \code{"gfit"}, which is a list with the following
  elements
  \item{dist.obs}{
    the value of the goodness-of-fit statistic for the data.}
  \item{dist.sim}{
    a vector of size \code{nb.replicate}. It is a sample of the null 
    distribution of the test statistic.}
}
\author{
  Louisiane Lemaire and Michael Blum.
}
\seealso{
  \code{\link{abc}}, \code{\link{plot.gfit}}, \code{\link{summary.gfit}}, 
  \code{\link{gfitpca}}
}
\examples{
## human demographic history
require(abc.data)
data(human)

##  Perform a test of goodness-of-fit.
##  The data are the European data and we test the fit of the bottleneck 
## model (good fit) and of the constant-size population model (poor fit) 
## Use larger values of \code{nb.replicate} (e.g. 1000)
## for real applications
res.gfit.bott=gfit(target=stat.voight["italian",], 
sumstat=stat.3pops.sim[models=="bott",], statistic=mean, nb.replicate=10)
res.gfit.const=gfit(target=stat.voight["italian",], 
sumstat=stat.3pops.sim[models=="const",], statistic=mean, nb.replicate=10)


## Plot the distribution of the null statistic and indicate where is the 
## observed value.
plot(res.gfit.bott, main="Histogram under H0")
## Call the function \code{summary}
## It computes the P-value, call \code{summary} on the vector 
## \code{dist.sim} and returns the value of the observed statistic
summary(res.gfit.bott)
}
