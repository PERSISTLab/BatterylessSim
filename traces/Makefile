TRACES=$(wildcard *.txt)
DSAMPLED=$(TRACES:.txt=-ds100.tmp)
DSAMPLEDPDF=$(TRACES:.txt=-ds100.pdf)

all:
	@echo 'Try "make sparklines"'
	@echo $(DSAMPLEDPDF)

%-ds100.tmp: %.txt
	awk '(NR%100==0){print}' $< > $@

.PHONY: all sparklines clean

%.pdf: %.tmp sparkline.R
	./sparkline.R $< && mv $<.pdf $@

sparklines: $(DSAMPLEDPDF)

clean:
	rm $(DSAMPLED)
