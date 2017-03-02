
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

	xyz = {'x' : Xs, 'y' : Ys, 'z' : Zs}

	df = pd.DataFrame(xyz, index=range(len(xyz['x'])))

	# create the 2D-arrays
	x1 = np.linspace(df['x'].min(), df['x'].max(), len(df['x'].unique()))
	y1 = np.linspace(df['y'].min(), df['y'].max(), len(df['y'].unique()))
	x2, y2 = np.meshgrid(x1, y1)
	z2 = griddata((df['x'], df['y']), df['z'], (x2, y2), method='cubic')

	fig = plt.figure()
	ax = fig.gca(projection='3d')
	surf = ax.plot_surface(x2, y2, z2, rstride=1, cstride=1, cmap=cm.coolwarm,
    linewidth=0, antialiased=False)

	ax.set_xlabel("Voltage (V)")
	ax.set_ylabel("Time (s)")
	ax.set_zlabel("Current (A)")

	fig.colorbar(surf, shrink=0.5, aspect=5)
	plt.title(title)

	#plt.show()
	plt.savefig("surfaces/" + title + ".eps")

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

	while totaltime < time_to_run: 
		totaltime += .2 # Increment time in seconds
		divisor = int((pnoise1(totaltime, 8) + .4) * 8) + 1 # in range of 1 - <whatever you're multiplying by>
		magnitude = (pnoise1(totaltime, 8) + .7)
		#print "time = " + str(totaltime) + " magnitude = " + str(magnitude)
		#current_row = []
		currents = []
		j = columns

		for i in range(0, len(current_list)):
			if i % divisor == 0:
				currents.append((current_list[i] * cm_squared * magnitude) / 1000)
			else:
				j-=1

		for i in range(j, columns):
			currents.append(0.0)

		voltages = [x * cells for x in voltage_list]

		timedata = []
		for i in range(0, columns):
			timedata.append(totaltime)

		for voltage, current in zip(voltages, currents):
			final_data.append([voltage, totaltime, current])

		#for voltage, current in zip(voltage_list, current_list):
			#print voltage * cells * magnitude
		#	amps = (current * cm_squared * magnitude) / 1000
		#	final_data.append([voltage * cells, totaltime, amps]) 
		#	current_row.append(amps)

		writer.writerow([totaltime] + currents)

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
	#kxob22_04x3l = ((0, 15.0), (.3, 14.95), (.6, 14.9), (.9, 14.85), (1.2, 14.7), (1.35, 14.2), (1.5, 13.1), (1.65, 10.7), (1.8, 6.0), (3.95, 0))
	#kxob22_12x1l = ((0, 42.5), (.1, 42.4), (.2, 42.3), (.3, 42.1), (.4, 41.6), (.45, 40), (.5, 37.5), (.55, 32), (.6, 20), (3.63, 0))
	# the above values are not correct

	#create_surface(panasonic_bsg, "am1456", 2.4, am1456_size)
	#create_surface(panasonic_bsg, "am1417", 2.4, am1417_size)
	#reate_surface(panasonic_bsg, "am1454", 2.4, am1454_size)
	create_surface(panasonic_bsg, "am1456", 3.2, am1456_size)
	create_surface(panasonic_bsg, "am1417", 3.5, am1417_size)
	create_surface(panasonic_bsg, "am1454", 4.0, am1454_size)
	# the above values are not correct
	create_surface(panasonic_bsg, "am5610", 5.1, am5610_size)
	create_surface(panasonic_bsg, "am5608", 5.1, am5608_size)
	create_surface(panasonic_bsg, "am8804", 6.8, am8804_size)
	create_surface(panasonic_bsg, "am8701", 6.0, am8701_size)
	create_surface(kxob22_01x8f, "kxob22_0x1x8f")	
	#create_surface(kxob22_04x3l, "kxob22_04x3l")
	#create_surface(kxob22_12x1l, "kxob22_12x1l")
