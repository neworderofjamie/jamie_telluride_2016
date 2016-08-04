import pylab
import spynnaker.pyNN as sim
import cerebellum as cer

# ------------------------------------------------------------------
# This example uses the sPyNNaker implementation of cerebellar
# STDP
# ------------------------------------------------------------------

# ------------------------------------------------------------------
# Common parameters
# ------------------------------------------------------------------
teaching_time = 400
delta_t = range(0,teaching_time,10)
start_time = 200
num_pre_cells = 100

# Population parameters
model = cer.IFCurrExpSupervision
cell_params = {'cm': 0.25,  # nF
               'i_offset': 0.0,
               'tau_m': 10.0,
               'tau_refrac': 2.0,
               'tau_syn_E': 2.5,
               'tau_syn_I': 2.5,
               'v_reset': -70.0,
               'v_rest': -65.0,
               'v_thresh': -55.4
               }

# SpiNNaker setup
sim.setup(timestep=0.1, min_delay=1.0, max_delay=10.0)


sim_time = 2000.0
first_spike_time = 100.0
pre_stim = []

for delta in delta_t:
    print first_spike_time + delta
    pre_stim.append(sim.Population(1, sim.SpikeSourceArray,{'spike_times': [first_spike_time + delta,
                                                                            2000.0]}))


teaching_stim = sim.Population(1, sim.SpikeSourceArray,{'spike_times': [teaching_time, ]})


# Neuron populations
population = sim.Population(1, model, cell_params)


 # Plastic Connection between pre_pop and post_pop
stdp_model = sim.STDPMechanism(
    timing_dependence = cer.TimingDependenceCerebellum(tau=50.0),
    weight_dependence = sim.AdditiveWeightDependence(w_min=0.0, w_max=1.0, A_plus=0.1, A_minus=0.5)
)

# Connections between spike sources and neuron populations
projections_pf = []
for stim in pre_stim:
    ####### SET HERE THE PARALLEL FIBER-PURKINJE CELL LEARNING RULE
    ee_connector = sim.OneToOneConnector(weights=0.5)
    projections_pf.append(sim.Projection(stim, population, ee_connector,
                                         synapse_dynamics=sim.SynapseDynamics(slow=stdp_model),
                                         target='excitatory'))

# SET HERE THE TEACHING SIGNAL PROJECTION
ee_connector = sim.OneToOneConnector()
proj_teaching = sim.Projection(teaching_stim, population, ee_connector, target='supervision')

print("Simulating for %us" % (sim_time / 1000))

# Run simulation
sim.run(sim_time)

# Get weight from each projection
end_w = [p.getWeights()[0] for p in projections_pf]


# -------------------------------------------------------------------
# Plot curve
# -------------------------------------------------------------------
# Plot STDP curve
figure, axis = pylab.subplots()
axis.set_xlabel('Time')
axis.set_ylabel('Weight')
axis.set_ylim((0.0, 1.0))
axis.plot([d + first_spike_time for d in delta_t], end_w)
axis.axvline(teaching_time, linestyle="--")

pylab.show()

# End simulation on SpiNNaker
sim.end()