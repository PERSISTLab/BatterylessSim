#!/usr/bin/Rscript

args <- commandArgs(TRUE)
infile <- args[[1]]

data <- read.table(infile, header=FALSE, sep="\t")
summary(data[,2])
