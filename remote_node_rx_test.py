import nengo
import nengo_spinnaker

# Model network
model = nengo.Network()
with model:
    nengo_spinnaker.add_spinnaker_params(model.config)

    # Request one remote ip tag bound to address on the SpiNNaker network
    model.config[nengo_spinnaker.Simulator].remote_node_iptags = { 2: ("192.168.240.1", 50007) }

    # Create node for remote receiving of simulation data
    remote_rx_node = nengo.Node(size_in=2, label="remote")
    model.config[remote_rx_node].remote_node_iptag = 2

    # Create an input node and an ensemble
    input_node = nengo.Node((0.5, 0.75), label="input")
    ensemble = nengo.Ensemble(100, dimensions=2, radius=1.0)

    # Connect everything together
    nengo.Connection(input_node, ensemble)
    nengo.Connection(ensemble, remote_rx_node)

sim = nengo_spinnaker.Simulator(model)

sim.run(20.0)