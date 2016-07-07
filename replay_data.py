import csv
import math
import matplotlib.animation as anim
import matplotlib.pyplot as plt
import numpy as np

grid_width = 10
grid_height = 10

with open("outdoor_park_path_noon/2016-07-05 11_17_37.014/2016-07-05 11_17_37.014.csv", "rb") as csv_file:
    csv_reader = csv.reader(csv_file, delimiter = ",")

    # Skip header
    header_row = csv_reader.next()

    # Extract columns
    columns = zip(*csv_reader)

    # Read times out of 1st columns
    times = np.asarray(columns[0], dtype=int)
    lon = np.asarray(columns[1], dtype=float)
    lat = np.asarray(columns[2], dtype=float)

    # Make times relative to first time
    times -= times[0]

    # Make longitude and latitude relative to smallest
    lon -= np.amin(lon)
    lat -= np.amin(lat)

    # Scale into cellspace
    lon = np.floor(lon * (float(grid_width - 1) / np.amax(lon))).astype(int)
    lat = np.floor(lat * (float(grid_height - 1) / np.amax(lat))).astype(int)

    # Copy first frame of spike vector matrix into image
    frame = np.zeros((grid_height, grid_width))
    frame[lat[0], lon[0]] = 1.0
    next_index = 1

    # Show first frame
    fig, axis = plt.subplots()
    path_image = axis.imshow(frame, interpolation="nearest",
                             vmin=0.0, vmax=1.0)

    def updatefig(frame_time):
        global frame, next_index, path_image

        # If it's time for the next frame
        while next_index < len(times) and frame_time >= times[next_index]:
            # Set pixel
            frame[lat[next_index], lon[next_index]] = 1.0

            # Go onto next sample
            next_index += 1

        # Update image data
        path_image.set_array(frame)

        return [path_image]

    # Play animation
    ani = anim.FuncAnimation(fig, updatefig, range(0, times[-1], 100), interval=1,
                             blit=True, repeat=False)
    plt.show()