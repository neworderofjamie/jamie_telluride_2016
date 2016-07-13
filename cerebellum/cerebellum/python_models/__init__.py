from spynnaker.pyNN.spinnaker import executable_finder
from model_binaries import __file__ as binaries_path

from timing_dependence_cerebellum import TimingDependenceCerebellum
from if_curr_exp_supervision import IFCurrExpSupervision

import os

# This adds the model binaries path to the paths searched by sPyNNaker
executable_finder.add_path(os.path.dirname(binaries_path))
