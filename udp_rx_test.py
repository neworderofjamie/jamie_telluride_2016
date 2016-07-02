import numpy as np
import socket
from rig.machine_control.packets import SCPPacket
from nengo_spinnaker.utils import type_casts as tp

in_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
in_socket.bind(('', 50007))

while True:
    try:
        data = in_socket.recv(512)
    except IOError:
        continue  # No more to read

    # Unpack the data, and store it as the input for the
    # appropriate Node.
    packet = SCPPacket.from_bytestring(data)
    values = tp.fix_to_np(
        np.frombuffer(packet.data, dtype=np.int32)
    )

    print (packet.src_x, packet.src_y, packet.src_cpu), values