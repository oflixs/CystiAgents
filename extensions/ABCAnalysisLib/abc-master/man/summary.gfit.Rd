\name{summary.gfit}
\alias{summary.gfit}
\title{
  Calculates the p-value of the goodness-of-fit test.
}
\description{

 This function calculates the p-value of the goodness-of-fit test from an 
  object of class \code{"gfit"}.

}
\usage{
\method{summary}{gfit}(object, \dots)
}
\arguments{
  \item{object}{
    an object of class \code{"gfit"}.}
  \item{\dots}{
    other parameters passed to \code{summary}.}
}
\details{
  
  It computes the P-value, call \code{summary} on the vector of 
  statistics simulated under the null and returns the value of the 
  observed statistic.

}
\value{
A list of the following elements
  \item{pvalue}{
    P-value for goodness-of-fit.}
  \item{s.dist.sim}{
    the result of a call to the function \code{summary}. Provides a 
    summary of the distribution of the goodness-of-fit statistic simulated
    under the null.}
  \item{dist.obs}{
    the observed value goodness-of-fit statistic.}
}
\seealso{
  \code{\link{gfit}}, \code{\link{abc}}, \code{\link{plot.gfit}}
}
\examples{
 ## see ?gfit for exemples
}
