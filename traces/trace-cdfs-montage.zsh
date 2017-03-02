#!/bin/zsh

for x in {1..10}; do
	./tracecdf.R "$x".txt "$x"cdf.pdf
done

montage -verbose -label %f -tile 2x5 -geometry 600x \
	[0-9]cdf.pdf 10cdf.pdf \
	JPEG:trace-cdfs-montage.jpg
