
import numpy as np
import matplotlib.pyplot as plt 
import os
import csv
from collections import defaultdict
from collections import Counter

eventrootdir = "events/"
energyrootdir = "energy/"

def counter_to_list (temp):

	add_zeros(temp)

	data = defaultdict(list)

	for item in temp.items():
		for entry in item[1].items():
			data[entry[0]].append(entry[1])

	return data

def get_filenames (data):
	files = []
	for key in data:
		files.append(key)
	return files

def get_max_values (data):
	max_value = 0
	max_keys = []

	for item in data.items():
		if len(item[1]) > max_value:
			max_value = len(item[1])
			for key in item[1]:
				max_keys.append(key)


	return max_value, max_keys

def add_zeros (data):
	max_value, all_keys = get_max_values(data)
	missing_values = []

	for item in data.items():
		if len(item[1]) < max_value:
			missing_values.append(item)

	for item in missing_values:
		for it in item[1].elements():
			#print it
			pass

	for item in all_keys:
		for value in missing_values:
			if item not in value[1].elements():
				value[1][item] = 0

def get_event_data (rootdir):
	
	data = defaultdict(Counter)
	filelist = []

	for subdir, dirs, files in os.walk(rootdir):
		for file in files:
			filename = file[len("event_") : - len(".csv")]
			filelist.append(filename)
			with open(os.path.join(subdir,file),'rb') as f:
				content = f.readlines()
				data[filename] = Counter(content)

	return counter_to_list(data), get_filenames(data)

def average (one, two):
	return (float(one) + float(two)) / 2.0

def get_energy_data (rootdir):
	
	timedata = defaultdict(list)
	capvoltagedata = defaultdict(list)
	ekhovoltagedata = defaultdict(list)
	currentdata = defaultdict(list)

	filelist = []

	for subdir, dirs, files in os.walk(rootdir):
		for file in files:
			#filename = file[len("energy_") : - len(".csv")]
			filename = file
			filelist.append(filename)
			with open(os.path.join(subdir,file),'rb') as csvfile:
				reader = csv.reader(csvfile)
				for row in reader:
					if len(row) == 5:
						timedata.setdefault(filename,[]).append(average(row[0], row[1]))
						currentdata.setdefault(filename,[]).append(float(row[2]) * 1000)
						ekhovoltagedata.setdefault(filename, []).append(float(row[3]))
						capvoltagedata.setdefault(filename,[]).append(float(row[4]))
					
	return timedata, ekhovoltagedata, capvoltagedata, currentdata, filelist

def autolabel(rects, ax):
    # attach some text labels
    for rect in rects:
        height = rect.get_height()
        ax.text(rect.get_x() + rect.get_width()/2., 1.05*height,
                '%d' % int(height),
                ha='center', va='bottom')

def graph_peripherals (rootdir):

	data, files = get_event_data(rootdir)
	data = data.items()

	n = len(data[0][1])
	ind = np.arange(n)
	width = .25 # the width of the bars

	fig, ax = plt.subplots()

	colors = ['r', 'y', 'g', 'b']
	rects = []

	for i in range(0, len(data)):
		rects.append(ax.bar(ind + width * i, data[i][1], width, color=colors[i]))

	for rect in rects:
		autolabel(rect, ax)

	# add labels, titles, xticks, etc
	ax.set_ylabel('Number of Calls to Event')
	ax.set_xlabel('Solar Panel')
	ax.set_xticks(ind + width)
	ax.set_xticklabels(files)

	ax.legend((x[0] for x in rects), (x[0] for x in data))

	plt.show()

def graph_energy (rootdir):
	timedata, ekhovoltagedata, capvoltagedata, currentdata, files = get_energy_data(rootdir)
	
	if len(files) % 2 == 0:
		rows = len(files) / 2
	else:
		rows = len(files) / 2 + 1

	f, axs = plt.subplots(rows, 2, sharex=True, sharey=True)
	axs = axs.ravel()

	lines = []

	for i in range(0, len(files)):
		file = files[i]

		l1, l2, l3 = axs[i].plot(timedata[file], capvoltagedata[file], 'r-', timedata[file], ekhovoltagedata[file], 'm--', timedata[file], currentdata[file], 'b-')

		axs[i].set_title('Plot for ' + file, fontsize=12)

	# Set common labels
	f.text(0.5, 0.04, 'Time (S)', ha='center', va='center')
	f.text(0.06, 0.5, 'Voltage (V) / Current (mA)', ha='center', va='center', rotation='vertical')

	plt.figlegend((l1, l2, l3), ("Capacitor Voltage", "Harvester Voltage", "Current"), 'upper right')
	f.suptitle("Energy Comparison of Multiple IV Surfaces with Single Firmware", fontsize=16)
	plt.show()

if __name__ == "__main__":
	for subdir, dirs, files in os.walk(eventrootdir):
		for d in dirs:
			graph_peripherals(eventrootdir + d)
			
	for subdir, dirs, files, in os.walk(energyrootdir):
		for d in dirs:
			graph_energy(energyrootdir + d)
