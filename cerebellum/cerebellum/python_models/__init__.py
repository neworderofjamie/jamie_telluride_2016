from spynnaker.pyNN.spinnaker import executable_finder
from python_models import model_binaries

import os

# This adds the model binaries path to the paths searched by sPyNNaker
executable_finder.add_path(os.path.dirname(model_binaries.__file__))
