'''
Created on Apr 30, 2010

@author: ssclark

This will make the "tab" output from the DAQ fit for consumption mspsim. 
I do not recommend running this near 12. It will not handle the time correctly.
'''
import optparse

# Main program
usage = "usage: %prog tracefile"
parser = optparse.OptionParser(usage=usage)
(options, args) = parser.parse_args()

if (len(args) != 2):
    parser.error("Incorrect number of arguments!");
    
infile = args[0]
outfile = args[1]

trace = file(infile,"r")
new_trace = file(outfile, "w")
for line in trace:
    row, time, junk, voltage = line.split()
    time = time.split(":")
    time = float("".join(time))*1000
    new_trace.write(str(time) + "\t" + voltage + "\n")
