from data_specification.enums.data_type import DataType

from spynnaker.pyNN.models.neuron.plasticity.stdp.timing_dependence\
    .abstract_timing_dependence import AbstractTimingDependence
from spynnaker.pyNN.models.neuron.plasticity.stdp.synapse_structure\
    .synapse_structure_weight_only import SynapseStructureWeightOnly
from spynnaker.pyNN.models.neuron.plasticity.stdp.common \
    import plasticity_helpers

import logging
logger = logging.getLogger(__name__)

# Constants
LOOKUP_SIN_SIZE = 256
LOOKUP_SIN_SHIFT = 0


class TimingDependenceCerebellum(AbstractTimingDependence):

    def __init__(self, tau=20.0, peak_time=100.0):
        AbstractTimingDependence.__init__(self)

        self.tau = tau
        self.peak_time = peak_time

        self._synapse_structure = SynapseStructureWeightOnly()

    def is_same_as(self, other):
        if (other is None) or (not isinstance(
                other, TimingDependenceCerebellum)):
            return False
        return ((self.tau == other.tau) and (self.peak_time == other.peak_time))

    @property
    def vertex_executable_suffix(self):
        return "cerebellum"

    @property
    def pre_trace_n_bytes(self):

        # Trace entries consist of a single 16-bit number
        return 0

    def get_parameters_sdram_usage_in_bytes(self):
        return (2 * LOOKUP_SIN_SIZE)

    @property
    def n_weight_terms(self):
        return 1

    def write_parameters(self, spec, machine_time_step, weight_scales):

        # Check timestep is valid
        if machine_time_step != 1000:
            raise NotImplementedError("STDP LUT generation currently only "
                                      "supports 1ms timesteps")
        # Write lookup table
        plasticity_helpers.write_exp_lut(
            spec, self.tau, LOOKUP_SIN_SIZE, LOOKUP_SIN_SHIFT)

    @property
    def synaptic_structure(self):
        return self._synapse_structure