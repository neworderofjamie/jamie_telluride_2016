import nengo
import nengo_spinnaker

MAX_SPEED = 0.2

ROBOT_PHONE_IP = "192.168.167.217"
ROBOT_PHONE_RX_PORT = 50007
ROBOT_RX_IPTAG = 2

CONTROLLER_PHONE_IP = "192.168.167.211"
CONTROLLER_PHONE_PORT = 50007
CONTROLLER_RX_IPTAG = 3

SONAR_TX_PORT = 50009
SONAR_TX_IPTAG = 4

IR_TX_PORT = 50010
IR_TX_IPTAG = 5

CONTROLLER_TX_PORT = 50012
CONTROLLER_TX_IPTAG = 6

# Model network
model = nengo.Network()
with model:
    nengo_spinnaker.add_spinnaker_params(model.config)

    # Use selected IP tag to transmit data back to selected port on phone
    model.config[nengo_spinnaker.Simulator].remote_node_tx_iptags = {
        ROBOT_RX_IPTAG: (ROBOT_PHONE_IP, ROBOT_PHONE_RX_PORT),
        CONTROLLER_RX_IPTAG: (CONTROLLER_PHONE_IP, CONTROLLER_PHONE_PORT)}

    # **YUCK
    # Send arbitrary value to phone to tell it ip address
    controller_link_ensemble = nengo.Ensemble(100, dimensions=1)
    controller_link_remote_node = nengo.Node(size_in=1)
    model.config[controller_link_remote_node].remote_tx_iptag = CONTROLLER_RX_IPTAG
    nengo.Connection(controller_link_ensemble, controller_link_remote_node)

    # Create remote input node to read IR sensor data from robot
    # **HACK** slicing remote node doesn't work so pass through ensemble
    ir_input_remote_node = nengo.Node(lambda t: (1.0, 1.0))
    model.config[ir_input_remote_node].remote_rx_iptag = (IR_TX_IPTAG, IR_TX_PORT)
    ir_input_ensemble = nengo.Ensemble(200, dimensions=2)
    nengo.Connection(ir_input_remote_node, ir_input_ensemble)

    # Create remote input node to read sonar sensor data from robot
    # **HACK** slicing remote node doesn't work so pass through ensemble
    #sonar_input_remote_node = nengo.Node(lambda t: (1.0, 1.0, 1.0))
    #model.config[sonar_input_remote_node].remote_rx_iptag = (SONAR_TX_IPTAG, SONAR_TX_PORT)
    #sonar_input_ensemble = nengo.Ensemble(300, dimensions=3)
    #nengo.Connection(sonar_input_remote_node, sonar_input_ensemble)

    # Connect inputs to sensor ensemble
    #sensor_ensemble = nengo.Ensemble(1000, dimensions=5)
    #nengo.Connection(ir_input_ensemble, sensor_ensemble[:2])
    #nengo.Connection(sonar_input_ensemble, sensor_ensemble[2:])

    # Connect sensor ensemble to motor ensemble
    motor_ensemble = nengo.Ensemble(200, dimensions=2)
    #learnt_connection = nengo.Connection(ir_input_ensemble, motor_ensemble,
    #                                     transform=[[0, 0, 0, 0, 0], [0, 0, 0, 0, 0]])
    learnt_connection = nengo.Connection(ir_input_ensemble, motor_ensemble,
                                         transform=[[0, 0], [0, 0]])


    # Create controller
    controller_input_remote_node = nengo.Node(lambda t: (1.0, 1.0, 1.0))
    model.config[controller_input_remote_node].remote_rx_iptag = (CONTROLLER_TX_IPTAG, CONTROLLER_TX_PORT)
    controller_input_ensemble = nengo.Ensemble(300, dimensions=3)
    nengo.Connection(controller_input_remote_node, controller_input_ensemble)
    
    # Error = actual - target = post - pre
    error_ensemble = nengo.Ensemble(200, dimensions=2)
    nengo.Connection(motor_ensemble, error_ensemble)
    nengo.Connection(controller_input_ensemble[:2], error_ensemble, transform=-1)
    nengo.Connection(controller_input_ensemble[2], error_ensemble.neurons, transform=[[-2.5]] * 200)

   
    # Add the learning rule to the connection
    learnt_connection.learning_rule_type = nengo.PES(learning_rate=0.001)

    # Connect the error into the learning rule
    nengo.Connection(error_ensemble, learnt_connection.learning_rule)

    # Create node to send speed signal to robot
    speed_output_remote_node = nengo.Node(size_in=1, label="speed")
    model.config[speed_output_remote_node].remote_tx_iptag = ROBOT_RX_IPTAG
    nengo.Connection(motor_ensemble[0], speed_output_remote_node)

    # Create node to send steering signal to robot
    steering_output_remote_node = nengo.Node(size_in=1, label="steer")
    model.config[steering_output_remote_node].remote_tx_iptag = ROBOT_RX_IPTAG
    nengo.Connection(motor_ensemble[1], steering_output_remote_node, transform=0.6)

sim = nengo_spinnaker.Simulator(model)
sim.run(2000.0)