\name{gfitpca}
\alias{gfitpca}
\title{
  Goodness of fit with principal component analysis
}
\description{

  Perform a priori goodness of fit using the two first components 
  obtained with PCA.
}
\usage{
gfitpca(target, sumstat, index, cprob=0.1, xlim=NULL, ylim=NULL, ...)
}
\arguments{
  \item{target}{
    a data frame or vector of the observed summary statistic.}
  \item{sumstat}{
    a matrix or data frame of the simulated summary statistics.}
  \item{index}{
    a vector of models names. It must be character and have the same
    length as the number of row in \code{sumstat} to indicate which row
    belong to which model.}
  \item{cprob}{
    \code{cprob} is the proportion of points outside the displayed 
    envelope. Default to 0.1 which corresponds to a 90\% envelope.}
  \item{xlim, ylim}{
    optional, numeric vectors of length 2, giving the x and y coordinates
    ranges.}
  \item{\dots}{
    other parameters passed to \code{plot}.}
}
\details{

  The function performs PCA using the a priori simulated summary
  statistics. It displays envelopes containing 1-\code{hprob} percent of
  the simulations.
  The projection of the observed summary statistics is displayed in
  order to check if they are contained or not in the envelopes.
  If the projection lies outside the envelope of a given model, it is
  an indication of poor fit.

}
\author{
  Louisiane Lemaire and Michael Blum
}
\seealso{
  \code{\link{abc}}, \code{\link{plot.gfit}}, \code{\link{summary.gfit}}, 
  \code{\link{gfit}}
}
\examples{
## human demographic history
require(abc.data)
data(human)
## five R objects are loaded. See ?human and vignette("abc") for details.

## Perform a priori goodness of fit for 3 different demographic models
## The envelopes containing 90% of the simulations are displayed.
## For the European data, a reasonable fit is only provided by the 
## bottleneck model.
## The number of simulations is reduced to improve speed (do not do that
## with your own data)
index<-c(1:5000,50001:55000,100001:105000)
gfitpca(target=stat.voight["italian",], sumstat=stat.3pops.sim[index,],
index=models[index], cprob=0.1)
}

