
import numpy as np
import matplotlib.pyplot as plt 
import csv
import random
from noise import pnoise1
from mpl_toolkits.mplot3d import Axes3D
from matplotlib import cm
import pandas as pd
from scipy.interpolate import griddata
import os
import matplotlib.tri as tri

n = 6
columns = 2**n + 1
time_to_run = 10 # in seconds

'''
	Use this function 
'''
def find_best_degree (x, y):
	ret = 100
	degree = 3
	for i in range (3, 10):
		p, res, _, _, _ = np.polyfit(x, y, i, full=True)
		if res < ret:
			degree = i
			ret = res

	return degree

def create_curve (solar_panel):
	x = np.array(tuple(x[0] for x in solar_panel))
	y = np.array(tuple(y[1] for y in solar_panel))

	#	degree = find_best_degree(x, y)
	degree = 4
	#print degree

	coefficients = np.polyfit(x, y, degree)
	polynomial = np.poly1d(coefficients)
	ys = polynomial(x)

	'''plt.plot(x, y, '.')
	plt.plot(x, ys)
	plt.show()'''

	spacing = (solar_panel[-1][0] - solar_panel[0][0]) / (columns - 1)
	xs = 0
	current_list = []
	voltage_list = []
	for i in range(1, columns):
		#print "voltage = " + str(xs) + " and current = " + str(polynomial(xs))
		current_list.append(polynomial(xs))
		voltage_list.append(xs)
		xs += spacing

	# current should always be 0 here, and the polyfit isn't exact at this point
	current_list.append(0.0)
	voltage_list.append(xs)

	return voltage_list, current_list

def create_image (final_data, title):
	try:
		os.makedirs("surfaces/")
	except:
		pass # directory already exists

	DATA = np.array(final_data)
	Xs = DATA[:,0] # voltage
	Ys = DATA[:,1] # time
	Zs = DATA[:,2] # current

	fig = plt.figure()
	ax = fig.gca(projection='3d')

	#plt.contour(x2, y2, z2)
	#plt.contour(x2, z2, y2)
	#plt.contour(y2, x2, z2)
	#plt.contour(y2, z2, x2)
	#plt.contour(z2, y2, x2)
	#ax.contour(z2, x2, y2, extend3d=True, stride=.2)
	ax.plot_trisurf(Xs, Ys, Zs)#edgecolor='none', vmax=0)

	ax.set_ylabel("Voltage (V)")
	ax.set_zlabel("Time (s)")
	ax.set_xlabel("Current (A)")

	#fig.colorbar(surf, shrink=0.5, aspect=5)
	plt.title(title)

	#plt.savefig("surfaces/" + title + ".eps")
	plt.show()

	return ax

def get_number_of_cells (solar_panel, voltage):
	if voltage is None:
		return 1
	else:	
		return (voltage / solar_panel[-1][0])

def get_square_cm_size (size):
	if size is None:
		return 1
	else:
		width = size[0]
		height = size[1]
		mm_squared = height * width
		return mm_squared / 100 # squared centimeters

'''
	solar_panel: company's IV surface for their solar panel
	open_voltage: Voltage in open circuit (in Volts)
	size: dimensions of solar cell in millimeters
	file_path: Name of file to output
'''
def create_surface (solar_panel, file_path="surface", open_voltage=None, size=None):

	cells = get_number_of_cells(solar_panel, open_voltage)

	cm_squared = get_square_cm_size(size)

	voltage_list, current_list = create_curve(solar_panel)
	try:
		os.makedirs("traces/")
	except:
		pass # directory already exists

	csvfile = open("traces/" + file_path + ".csv", 'w')
	writer = csv.writer(csvfile, quoting=csv.QUOTE_MINIMAL)
	writer.writerow([n]) # First write the 'n' value
	if (open_voltage is None):
		writer.writerow([0, solar_panel[-1][0]]) # Voltage range (min and max)
	else:
		writer.writerow([0, open_voltage]) # Voltage range (min and max)

	totaltime = 0
	final_data = []
	#print file_path
	while totaltime < time_to_run: 
		totaltime += .2 # Increment time in seconds
		magnitude = (pnoise1(totaltime, 8) + .7)# * .6
		#print "time = " + str(totaltime) + " magnitude = " + str(magnitude)
		current_row = []
		max_voltage = voltage_list[-1] * magnitude * cells

		for voltage, current in zip(voltage_list, current_list):
			#print voltage * cells * magnitude
			amps = (current * cm_squared * magnitude) / 1000
			volts = voltage * cells * magnitude
			if voltage * cells >= max_voltage:
				amps = 0
				
			final_data.append([voltage * cells * magnitude, totaltime, amps]) 
			current_row.append(amps)

		writer.writerow([totaltime] + current_row)

	return create_image(final_data, file_path)

if __name__ == "__main__":
	# All of these are in pairs of (Voltage <V/cell>, Current<mA/cm^2>)
	panasonic_bsg = ((0, 14.9), (.2, 14.5), (.4, 14.1), (.6, 13.2), (.72, 10), (.8, 6), (.88, 0))
	am1456_size = (25, 10) # in mm
	am1417_size = (35, 13.9)
	am1454_size = (41.6, 26.3)
	am5610_size = (25, 20)
	am5608_size = (60.1, 41.3)
	am8804_size = (48.1, 55.1)
	am8701_size = (57.7, 55.1)
	kxob22_01x8f = ((0, 4.45), (.5, 4.43), (1.5, 4.4), (2.5, 4.3), (3.0, 4.1), (3.25, 3.9), (3.5, 3.7), (3.75, 3.3), (4.0, 2.7), (4.25, 1.75), (4.5, .7), (4.7, 0))
	kxob22_04x3l = ((0, 15.0), (.3, 14.95), (.6, 14.9), (.9, 14.85), (1.2, 14.7), (1.35, 14.2), (1.5, 13.1), (1.65, 10.7), (1.8, 6.0), (1.95, 0))
	kxob22_12x1l = ((0, 42.5), (.1, 42.4), (.2, 42.3), (.3, 42.1), (.4, 41.6), (.45, 40), (.5, 37.5), (.55, 32), (.6, 20), (.63, 0))

	'''create_surface(panasonic_bsg, "am1456", 2.4, am1456_size)
	create_surface(panasonic_bsg, "am1417", 2.4, am1417_size)
	create_surface(panasonic_bsg, "am1454", 2.4, am1454_size)
	create_surface(panasonic_bsg, "am5610", 5.1, am5610_size)
	create_surface(panasonic_bsg, "am5608", 5.1, am5608_size)
	create_surface(panasonic_bsg, "am8804", 6.8, am8804_size)
	create_surface(panasonic_bsg, "am8701", 6.0, am8701_size)'''
	create_surface(kxob22_01x8f, "kxob22_0x1x8f")	
	#create_surface(kxob22_04x3l, "kxob22_04x3l")
	#create_surface(kxob22_12x1l, "kxob22_12x1l")
