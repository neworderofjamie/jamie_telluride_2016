import nengo
import nengo_spinnaker

MAX_SPEED = 0.15

PHONE_IP = "192.168.167.217"
PHONE_RX_PORT = 50007

RX_IPTAG = 2

SONAR_TX_PORT = 50009
SONAR_TX_IPTAG = 3

# Model network
model = nengo.Network()
with model:
    nengo_spinnaker.add_spinnaker_params(model.config)

    # Use selected IP tag to transmit data back to selected port on phone
    model.config[nengo_spinnaker.Simulator].remote_node_tx_iptags = { RX_IPTAG: (PHONE_IP, PHONE_RX_PORT) }

    # Create remote input node to read sonar sensor data from robot
    # **HACK** slicing remote node doesn't work so pass through ensemble
    sonar_input_remote_node = nengo.Node(lambda t: (1.0, 1.0, 1.0))
    model.config[sonar_input_remote_node].remote_rx_iptag = (SONAR_TX_IPTAG, SONAR_TX_PORT)
    sonar_input_ensemble = nengo.Ensemble(300, dimensions=3)
    nengo.Connection(sonar_input_remote_node, sonar_input_ensemble)
    
    # Constant node used to provide zeroness to various bits of network
    one = nengo.Node([1.0])

    # Inhibit left and right ensembles with left and right sonar
    left_ensemble = nengo.Ensemble(100, dimensions=1)
    right_ensemble = nengo.Ensemble(100, dimensions=1)
    nengo.Connection(one, left_ensemble)
    nengo.Connection(one, right_ensemble)
    nengo.Connection(sonar_input_ensemble[0], left_ensemble.neurons, transform=[[-2.5]] * 100)
    nengo.Connection(sonar_input_ensemble[2], right_ensemble.neurons, transform=[[-2.5]] * 100)

    # Apply center sonar to both left and right action
    #nengo.Connection(sonar_input_ensemble[1], left_ensemble.neurons, transform=[[-0.5]] * 100)
    #nengo.Connection(sonar_input_ensemble[1], right_ensemble.neurons, transform=[[-0.5]] * 100)

    # Create speed ensemble, inhbited by left and right signale
    max_speed = nengo.Node([MAX_SPEED])
    speed_ensemble = nengo.Ensemble(100, dimensions=1)
    nengo.Connection(max_speed, speed_ensemble, transform=1)
    #nengo.Connection(left_ensemble, speed_ensemble.neurons, transform=[[-1]] * 100)
    #nengo.Connection(right_ensemble, speed_ensemble.neurons, transform=[[-1]] * 100)

    # Create node to send speed signal to robot
    speed_output_remote_node = nengo.Node(size_in=1, label="speed")
    model.config[speed_output_remote_node].remote_tx_iptag = RX_IPTAG
    nengo.Connection(speed_ensemble, speed_output_remote_node)

    # Connect sonar ensembles to basal ganglia
    basal_ganglia = nengo.networks.BasalGanglia(2)
    nengo.Connection(left_ensemble, basal_ganglia.input[0])
    nengo.Connection(right_ensemble, basal_ganglia.input[1])

    # Pass outputs through thalamus network
    thalamus = nengo.networks.Thalamus(2)
    nengo.Connection(basal_ganglia.output[0], thalamus.input[0])
    nengo.Connection(basal_ganglia.output[1], thalamus.input[1])

    # Use thalamus output to drive steering
    steering_inhib = nengo.Node([1.0])
    steering_ensemble = nengo.Ensemble(100, dimensions=1)
    nengo.Connection(steering_inhib, steering_ensemble.neurons, transform=[[-1.15]] * 100)
    nengo.Connection(thalamus.output[0], steering_ensemble, transform=1.0)
    nengo.Connection(thalamus.output[1], steering_ensemble, transform=-1.0)

    # Create node to send steering signal to robot
    steering_output_remote_node = nengo.Node(size_in=1, label="steer")
    model.config[steering_output_remote_node].remote_tx_iptag = RX_IPTAG
    nengo.Connection(steering_ensemble, steering_output_remote_node)

sim = nengo_spinnaker.Simulator(model)
sim.run(2000.0)