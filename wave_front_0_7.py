import itertools
import matplotlib.pyplot as plt
import matplotlib.animation as anim
import numpy as np
from scipy import misc

# Should we run on SpiNNaker (otherwise NEST)
spinnaker = True

# Should we try and use STDP
plastic = False

# Should delay or weight be modulated
delay_modulation = True

# Scaling factor used to convert from 8-bit pixel values to delay
delay_modulation_scale = 2.0 / 255.0

# Scaling factor used to convert from 8-bit pixel values to weight
weight_modulation_scale = 29.0 / 255.0

# Load cost image
cost_image = misc.imread("map.png")

# Where to start wave front
stim_x = 43
stim_y = 43

# Where is our destination
target_x = 10
target_y = 10

# How long to simulate
duration = 400

# What weight will immediately cause a spike
# **NOTE** this is massive so single presynaptic spikes causes immediate postsynaptic spike
instant_spike_weight = 30.0

def get_neuron_index(x, y, width):
    return (y * width) + x

def add_connection(start_x, start_y,
                   end_x, end_y,
                   cost_image, conn_list,
                   delay_func, weight_func):
    # Get costs
    end_cost = cost_image[end_y, end_x]

    # If end vertex isn't blocking
    if end_cost != 0xFF:
        delay = delay_func(end_cost)
        weight = weight_func(end_cost)

        # Add connection
        conn_list.append((get_neuron_index(start_x, start_y, cost_image.shape[0]),
                            get_neuron_index(end_x, end_y, cost_image.shape[0]),
                            weight, delay))

if spinnaker:
    import spynnaker.pyNN as sim
else:
    import pyNN.nest as sim


# setup simulator
sim.setup(timestep=1.0, min_delay=1.0, max_delay=8.0)

# Create population of neurons
num_neurons = cost_image.shape[0] * cost_image.shape[1]
neurons = sim.Population(num_neurons, sim.IF_curr_exp, {"tau_refrac": 20}, label="pop")

# Record spikes
neurons.record()

# If we're modulating delay
if delay_modulation:
    # Convert 0-255 cost to a delay from 1-7 and use a
    # weight large enough to cause an immediate spike
    delay_func = lambda c: 1.0 + (c * delay_modulation_scale)
    weight_func = lambda c: instant_spike_weight
else:
    delay_func = lambda c: 1.0
    weight_func = lambda c: instant_spike_weight - (c * weight_modulation_scale)

# Loop through neurons
conn_list = []
for x, y in itertools.product(range(cost_image.shape[0]),
                              range(cost_image.shape[1])):
    # Left
    if x > 0:
        add_connection(x, y,
                       x - 1, y,
                       cost_image, conn_list,
                       delay_func, weight_func)

    # Right
    if x < (cost_image.shape[0] - 1):
        add_connection(x, y,
                       x + 1, y,
                       cost_image, conn_list,
                       delay_func, weight_func)

    # Up
    if y > 0:
        add_connection(x, y,
                       x, y - 1,
                       cost_image, conn_list,
                       delay_func, weight_func)

    # Down
    if y < (cost_image.shape[1] - 1):
        add_connection(x, y,
                       x, y + 1,
                       cost_image, conn_list,
                       delay_func, weight_func)


if plastic:
    stdp_model = sim.STDPMechanism(
        timing_dependence=sim.SpikePairRule(tau_plus=50.0, tau_minus=50.0),
        weight_dependence=sim.AdditiveWeightDependence(w_min=0.0, w_max=instant_spike_weight,
                                                       A_plus=0.000001, A_minus=1.0),
        dendritic_delay_fraction=1.0)
    synapse_dynamics = sim.SynapseDynamics(slow=stdp_model)
else:
    synapse_dynamics = None

# Create connector
proj = sim.Projection(neurons, neurons, sim.FromListConnector(conn_list),
                      synapse_dynamics=synapse_dynamics,
                      target="excitatory")


# Stimulate stim neuron
stim = sim.Population(1, sim.SpikeSourceArray, {"spike_times": [2.0]}, label="stim")
sim.Projection(stim, neurons,
               sim.FromListConnector([(0, get_neuron_index(stim_x, stim_y, cost_image.shape[0]),
                                       instant_spike_weight, 1.0)]))

# Run network
sim.run(duration)

# Read data
spikes = neurons.getSpikes()

sim.end()

# Calculate x and y coordinates corresponding to each spike
neuron_x = spikes[:,0] % cost_image.shape[1]
neuron_y = spikes[:,0] // cost_image.shape[1]

# Get time of last spike
end_time = int(np.amax(spikes[:,1]))

# Convert spike times to 3D spike matrix
matrix, _ = np.histogramdd((neuron_y, neuron_x, spikes[:,1]),
                           bins=(range(cost_image.shape[0] + 1),
                                 range(cost_image.shape[1] + 1),
                                 range(end_time + 1)))
# Check spike vector only ever has ones and zeros
assert np.amax(matrix) == 1.0

print "End time:%u" % end_time

# Create RGBA image to display path information
path_image = np.zeros((cost_image.shape[0], cost_image.shape[1], 4))

# Add pixels indicating stim and end to image
path_image[stim_y, stim_x] = (0.0, 1.0, 0.0, 1.0)
path_image[target_y, target_x] = (0.0, 0.0, 1.0, 1.0)

# Backtrack to find path
x = target_x
y = target_y
max_time = duration
while True:
     # If we've reached stimulus, stop
    if x == stim_x and y == stim_y:
        break;

    # Loop through neighbours
    first_time = max_time
    first_x = None
    first_y = None
    for x_offset, y_offset in itertools.product(range(-1, 2, 1), repeat=2):
        # Skip self-connections
        if x_offset == 0 and y_offset == 0:
            continue

        # Get spike vector for this offset vertex
        spike_vector = matrix[y + y_offset,x + x_offset,:]

        # Find the first time bin in which it spiked
        spike_time = np.where(spike_vector > 0.0)[0]

        # If there were any spikes at this pixel and the first spike
        # that occured is earlier than current best
        if len(spike_time) > 0 and spike_time[0] < first_time:
            first_time = spike_time[0]
            first_x = x + x_offset
            first_y = y + y_offset

    # Assert that we found our parent
    assert first_x is not None
    assert first_y is not None

    # Advance to parent
    x = first_x
    y = first_y
    max_time = first_time

    # Draw path point
    path_image[y, x, :] = (1.0, 1.0, 1.0, 1.0)

fig, axis = plt.subplots()

# Copy first frame of spike vector matrix into image
image = np.zeros((cost_image.shape[0], cost_image.shape[1]))
image[:] = matrix[:,:,0]

# Show cost and path images
axis.imshow(cost_image, interpolation="nearest")
axis.imshow(path_image, interpolation="nearest",
            vmin=0.0, vmax=1.0)

# Show the spiking activity
spike_image = axis.imshow(image, interpolation="nearest",
                          vmin=0.0, vmax=1.0, alpha=0.5)

def updatefig(frame):
    global image

    # Decay image
    # **TODO** interval`
    image *= 0.9

    # Add this frame's spike vector to image
    image += matrix[:,:,frame]

    # Update image data
    spike_image.set_array(image)
    return [spike_image]

# Play animation
ani = anim.FuncAnimation(fig, updatefig, range(end_time), interval=30,
                         blit=True, repeat=True)
plt.show()