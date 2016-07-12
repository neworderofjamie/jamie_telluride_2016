import collections
import enum
import functools
import itertools
import logging
import numpy
import os
import pickle
import six
import sys
import network

# Configuration
train = True
session_name = "s0103a"
num_mcu_neurons = 100
num_hcu = 1
spinnaker_kwargs = {"spalloc_num_boards": 1}
tau_p = 2000
epochs = 20

# Set PyNN spinnaker log level
logger = logging.getLogger("pynn_spinnaker")
logger.setLevel(logging.INFO)
logger.addHandler(logging.StreamHandler())

# Create folder
folder = "%s_subset_phonemes" % session_name
if not os.path.exists(folder):
    os.makedirs(folder)

# Bind parameters to euclidean HCU delay model
delay_model = functools.partial(network.euclidean_hcu_delay,
                                grid_size=1, distance_scale=0.75, velocity=0.2)

# If we're training
if train:
    phoneme_counter = collections.Counter()

    # Open proc (clean phoneme onset and end)
    with open("%s/%s.proc" % (session_name, session_name), "r") as proc_file:
        # Loop through lines in file
        for line in proc_file:
            # Split columns
            cols = line.split()

            # If this is a valid phoneme, count it
            if cols[3] != "NA":
                phoneme_counter.update((cols[3],))

    print "Phonemes:"
    most_common_phonemes = set()
    for phoneme, count in phoneme_counter.most_common(10):
        print "\t%s: %u" % (phoneme, count)
        most_common_phonemes.add(phoneme)

    # Open words file
    print "Words:"
    possible_words = {}
    with open("%s/%s.words" % (session_name, session_name), "r") as word_file:
        # Find start of data
        for line in word_file:
            if line == "#\n":
                break

        # Loop through remaining lines
        for line in word_file:
            # Split into semicolon'd groups
            groups = line.split(";")

            # Extract word
            word = groups[0].split()[2]

            # If word has already been parsed
            if word in possible_words:
                continue

            # Loop through possible phoneme sequences
            for possible_phonemes in groups[1:-1]:
                phonemes = possible_phonemes.split()

                # If all phonemes are amongst our most common subset
                possible = True
                for p in phonemes:
                    if p not in most_common_phonemes:
                        possible = False
                        break

                if possible:
                    print "\t%s: %s" % (word, str(phonemes))
                    possible_words[word] = phonemes
                    break

    # Assign indices i.e. minicolumns to phonemes, saving a copy to disk
    with open("%s/phoneme_labels.pkl" % folder, "wb") as phoneme_file:
        pickle.dump([p for p in most_common_phonemes], phoneme_file)
    phoneme_indices = {p: i for i, p in enumerate(most_common_phonemes)}

    # Build stimuli from possible words
    stim_minicolumns = []
    t = 0.0
    for _ in range(epochs):
        for phonemes in six.itervalues(possible_words):
            for p in phonemes:
                stim_minicolumns.append((phoneme_indices[p], t, 20.0, 100.0))
                t += 100.0

            t += 100.0
    # Get end time of last
    training_simtime = stim_minicolumns[-1][1] + stim_minicolumns[-1][3]
    print("%u phonemes, training for %ums" % (len(phoneme_indices), training_simtime))

    # Simulate
    hcu_results, connection_results, end_simulation = network.train_discrete(network.tau_syn_ampa_gaba, network.tau_syn_ampa_gaba,
                                                                             network.tau_syn_nmda, network.tau_syn_ampa_gaba, tau_p,
                                                                             stim_minicolumns, training_simtime, delay_model,
                                                                             1, len(phoneme_indices), num_mcu_neurons, **spinnaker_kwargs)

    # Save weights for all connections
    for i, (ampa_weight_writer, nmda_weight_writer) in enumerate(connection_results):
        # Write AMPA weights
        ampa_weight_writer("%s/connection_%u_e_e_ampa.npy" % (folder, i))

        # Write NMDA weights to correct folder
        nmda_weight_writer("%s/connection_%u_e_e_nmda.npy" % (folder, i))

    # Loop through the HCU results and save data to pickle format
    for i, (hcu_e_data_writer,) in enumerate(hcu_results):
        hcu_e_data_writer("%s/hcu_%u_e_data.pkl" % (folder, i))

    # Once data is read, end simulation
    end_simulation()
else:
    # Open label file
    with open("%s/phoneme_labels.pkl" % folder, "rb") as label_file:
        num_phonemes = len(pickle.load(label_file))

    # Testing parameters
    testing_simtime = 6000.0   # simulation time [ms]

    ampa_nmda_ratio = 4.795918367
    tau_ca2 = 300.0
    i_alpha = 0.15
    gain_per_hcu = 0.546328125

    # Calculate gain
    gain = gain_per_hcu / float(num_hcu)

    # Load biases for each HCU
    hcu_biases = []
    for i in range(num_hcu):
        # Open pickle file
        with open("%s/hcu_%u_e_data.pkl" % (folder, i), "rb") as f:
            # Load pickled data
            pickled_data = pickle.load(f)

            # Filter out bias
            hcu_bias = pickled_data.segments[0].filter(name="bias")[0]

            # Add final recorded bias to list
            # **HACK** investigate where out by 1000 comes from!
            hcu_biases.append(hcu_bias[-1,:] * 0.001)

    # Build correct filename format string for weights
    nmda_weight_filename_format = ("%s/connection_%u_e_e_nmda.npy")

    # Load weights for each connection
    connection_weights = []
    for i in range(num_hcu ** 2):
        connection_weights.append((
            "%s/connection_%u_e_e_ampa.npy" % (folder, i),
            nmda_weight_filename_format % (folder, i)
        ))

    # Stimulate the first minicolumn for 50ms, 100ms into simulation
    stim_minicolumns = [(6, 100.0, 20.0, 50.0)]


    hcu_results, end_simulation = network.test_discrete(connection_weights, hcu_biases,
                                                        gain, gain / ampa_nmda_ratio, tau_ca2, i_alpha,
                                                        stim_minicolumns, testing_simtime, delay_model,
                                                        num_hcu, num_phonemes, num_mcu_neurons, False,
                                                        **spinnaker_kwargs)

    # Loop through the HCU results and save spikes data
    for i, (hcu_e_data_writer, hcu_i_data_writer) in enumerate(hcu_results):
        hcu_e_data_writer("%s/hcu_%u_e_testing_data.pkl" % (folder, i))
        hcu_i_data_writer("%s/hcu_%u_i_testing_data.pkl" % (folder, i))

    # Once data is read, end simulation
    end_simulation()



