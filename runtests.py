
import os
import argparse
from os.path import expanduser

classpath = ".:./lib/*"
home = expanduser("~")
os.chdir(home + "/sorber/mspsim-final")

def print_make ():
	print "\n**********************************************************"
	print "**                                                      **"
	print "**         Making java project...                       **"
	print "**                                                      **"
	print "**********************************************************\n"

def print_test (file):
	print "\n------------------------------------------------------------"
	print "\n\tRunning test for firmware file: " + file + "\n"
	print "------------------------------------------------------------\n"

if __name__ == "__main__":

    parser = argparse.ArgumentParser(description="Run multiple firmware files with SIREN")
    parser.add_argument("-firmwaredir", type=str, help="Name of directory with firmware files")
    parser.add_argument("--make", dest="make", action='store_true', help="Use this arguement if you want to remake the java project")
    parser.add_argument("--no-make", dest="make", action='store_false', help="Use this arguement if you do not want to remake the java program")
    parser.add_argument("-ekhotracedir", type=str, help="Name of directory with ekho traces")
    parser.set_defaults(make=True)

    args = parser.parse_args()

    if args.make is True:
    	print_make()
    	os.system("make")

    counter = 1
    for i in os.listdir(args.firmwaredir):
    	file = args.firmwaredir + i
    	print_test(file)
    	os.system("java -cp " + classpath + " se.sics.mspsim.Main -nogui -exitwhendone -evalsubdir=test" + str(counter) + " -platform=senseandsend -ekhotracedir=" + args.ekhotracedir + " -autorun=scripts/simple.sc " + file)
    	counter += 1
