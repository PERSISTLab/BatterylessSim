#!/usr/bin/Rscript

args <- commandArgs(TRUE)
infile <- args[[1]]
outfile <- args[[2]]

# set up the output device as a PDF file
pdf(outfile, width=6, height=4, pointsize=10, family="Helvetica")

data <- read.table(infile, header=FALSE, sep="\t")
plot(ecdf(data[,2]), do.points=FALSE, verticals=TRUE,
	xlab="Voltage (V)", ylab="Pr(V < x)",
	main=paste("CDF of voltage, trace=", infile))
