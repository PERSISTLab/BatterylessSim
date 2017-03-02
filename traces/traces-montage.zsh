#!/bin/zsh

montage -verbose -label %f -tile 2x5 -geometry 600x \
	[0-9]plot.pdf 10plot.pdf \
	JPEG:traces-montage.jpg
