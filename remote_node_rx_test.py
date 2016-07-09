import nengo
import nengo_spinnaker

# Model network
model = nengo.Network()
with model:
    nengo_spinnaker.add_spinnaker_params(model.config)

    # Bind ip tag 2 to transmit data back to host
    model.config[nengo_spinnaker.Simulator].remote_node_tx_iptags = { 2: ("192.168.167.217", 50007) }

    # Create node for remote receiving of simulation data
    remote_speed_rx_node = nengo.Node(size_in=1, label="speed")
    model.config[remote_speed_rx_node].remote_tx_iptag = 2

    remote_steer_rx_node = nengo.Node(size_in=1, label="steer")
    model.config[remote_steer_rx_node].remote_tx_iptag = 2

    # Create an input node and an ensemble
    input_node = nengo.Node((0.01, 0.0), label="input")
    ensemble = nengo.Ensemble(100, dimensions=2, radius=1.0)

    # Connect everything together
    nengo.Connection(input_node, ensemble)
    nengo.Connection(ensemble[0], remote_speed_rx_node)
    nengo.Connection(ensemble[1], remote_steer_rx_node)

sim = nengo_spinnaker.Simulator(model)

sim.run(20.0)