#!/bin/zsh

for x in {1..10}; do
	echo "Voltage trace ${x}.txt:"
	./tracesummary.R "$x".txt
done
