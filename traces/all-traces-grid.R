library(ggplot2)
library(grid)
library(reshape2)

# start with shortest trace
alldata <- data.frame(read.delim('9.txt', header=F))
alldata$V1 <- (alldata$V1 - alldata$V1[1]) / 1000.0
names(alldata)[2] <- "Trace_9"
nrows <- nrow(alldata)

for (i in c(1:8, 10:10)) { #XXX
    fname <- paste(toString(i), '.txt', sep='')
    colname <- paste('Trace_', toString(i), sep='')
    
    alldata[colname] <- read.delim(fname, header=F)[1:nrows,2]
}

melted <- melt(alldata, id='V1')
melted$variable <- factor(melted$variable,
    levels=c("Trace_1","Trace_2","Trace_3","Trace_4","Trace_5",
             "Trace_6","Trace_7","Trace_8","Trace_9","Trace_10"))

myplot <- ggplot(data=melted) +
    aes(x=V1, y=value) +
    xlab("Time (s)") +
    ylab("Voltage (V)") +
    geom_point(size=0.75) +
    facet_wrap(~variable, ncol=4) +
    theme(axis.title=element_text(size=18),
          axis.text=element_text(size=13),
          strip.text = element_text(size=18))

ggsave("all-traces-grid.pdf", width=10, height=10, pointsize=16) #, device=cairo_pdf);