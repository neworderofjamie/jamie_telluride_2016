import nengo
import nengo_spinnaker

# Model network
model = nengo.Network()
with model:
    nengo_spinnaker.add_spinnaker_params(model.config)

    # Bind ip tag 2 to transmit data back to host
    model.config[nengo_spinnaker.Simulator].remote_node_tx_iptags = { 2: ("192.168.167.217", 50007) }

    # Create node to print output
    def printer(t, x):
        print x
    heading_output_node = nengo.Node(printer, size_in=1, label="output")

    # Create an input node that receives input on UDP port 50007
    # **YUCK** have to specify something to make correct input
    heading_input_remote_node = nengo.Node(lambda t: (0.0), label="input")
    model.config[heading_input_remote_node].remote_rx_iptag = (3, 50008)

    # Connecting remote heading input via ensemble to printer
    heading_ensemble = nengo.Ensemble(100, dimensions=1, radius=1.0)
    nengo.Connection(heading_input_remote_node, heading_ensemble)
    nengo.Connection(heading_ensemble, heading_output_node)

    # Create remote output nodes which send speed and steering data to iptag 2
    speed_output_remote_node = nengo.Node(size_in=1, label="speed")
    model.config[speed_output_remote_node].remote_tx_iptag = 2
    steer_output_remote_node = nengo.Node(size_in=1, label="steer")
    model.config[steer_output_remote_node].remote_tx_iptag = 2

    # Create an input node and an ensemble
    motor_input_node = nengo.Node((0.1, 0.0), label="input")
    motor_ensemble = nengo.Ensemble(100, dimensions=2, radius=1.0)

    # Connect everything together
    nengo.Connection(motor_input_node, motor_ensemble)
    nengo.Connection(motor_ensemble[0], speed_output_remote_node)
    nengo.Connection(motor_ensemble[1], steer_output_remote_node)

sim = nengo_spinnaker.Simulator(model)

sim.run(20.0)