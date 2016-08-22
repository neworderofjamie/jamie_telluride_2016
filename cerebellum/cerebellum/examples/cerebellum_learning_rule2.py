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
teaching_time = 400.
tau=50.
peak_time = 100.
#num_pre_cells = 100

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
ts=0.5	# does not work at 0.4, 0.6, 0.8, why??
sim.setup(timestep=ts, min_delay=ts, max_delay=15*ts)

sim_time = 2000.
pre_stim = []

spike_times = []
for t in pylab.arange(0,teaching_time,2.):
    print t
    spike_times.append([t,sim_time-ts])

print spike_times,len(spike_times)
pre_stim = sim.Population(len(spike_times),sim.SpikeSourceArray,{'spike_times': spike_times})

teaching_stim = sim.Population(1, sim.SpikeSourceArray,{'spike_times': [teaching_time]})

# Neuron populations
population = sim.Population(1, model, cell_params)

 # Plastic Connection between pre_pop and post_pop
stdp_model = sim.STDPMechanism(
    timing_dependence = cer.TimingDependenceCerebellum(tau=tau, peak_time=peak_time),
    weight_dependence = sim.AdditiveWeightDependence(w_min=0.0, w_max=1.0, A_plus=0.1, A_minus=0.5)
)

# Connections between spike sources and neuron populations
    ####### SET HERE THE PARALLEL FIBER-PURKINJE CELL LEARNING RULE
ee_connector = sim.AllToAllConnector(weights=0.5)
projection_pf = sim.Projection(pre_stim, population, ee_connector,
                                         synapse_dynamics=sim.SynapseDynamics(slow=stdp_model),
                                         target='excitatory')

# SET HERE THE TEACHING SIGNAL PROJECTION
ee_connector = sim.OneToOneConnector()
proj_teaching = sim.Projection(teaching_stim, population, ee_connector, target='supervision')

print("Simulating for %us" % (sim_time / 1000))

# Run simulation
sim.run(sim_time)

# Get weight from each projection
end_w = projection_pf.getWeights() #[p.getWeights()[0] for p in projections_pf]
print end_w

# -------------------------------------------------------------------
# Plot curve
# -------------------------------------------------------------------
# Plot STDP curve
figure, axis = pylab.subplots()
axis.set_xlabel('Time Delta')
axis.set_ylabel('Weight')
axis.set_ylim((0.0, 1.0))
axis.plot([t[0] - teaching_time for t in spike_times], end_w)
#axis.axvline(teaching_time, linestyle="--")

pylab.show()

# End simulation on SpiNNaker
sim.end()