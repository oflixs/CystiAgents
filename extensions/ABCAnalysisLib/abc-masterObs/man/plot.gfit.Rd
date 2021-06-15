\name{plot.gfit}
\alias{plot.gfit}
\title{
   Goodness-of-fit plot for ABC
}
\description{

  Plotting method for goodness-of-fit ABC objects.

}
\usage{
\method{plot}{gfit}(x, breaks="Freedman-Diaconis", main, \dots)
}
\arguments{
  \item{x}{
    an object of class \code{"gfit"}.}
  \item{breaks}{
    number of cells for the histogram. Defaults to "Freedman-Diaconis".}
  \item{main}{
    title for the plot}
  \item{\dots}{
    other parameters passed to \code{plot}.}
}
\details{

  Plot the distribution of the statistic under the null hypothesis and 
  indicate where is the observed value.

}
\seealso{
  \code{\link{gfit}}, \code{\link{abc}}, \code{\link{summary.gfit}}, 
  \code{\link{hist}}
}
\examples{
 ## see ?gfit for examples
}

