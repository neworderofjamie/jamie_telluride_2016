#ifndef _CEREBELLUM_TIMING_H_
#define _CEREBELLUM_TIMING_H_

#define POST_EVENTS_ARE_SUPERVISION

// TODO: Add any variables to be stored in the post trace structure
typedef struct post_trace_t {
} post_trace_t;

// TODO: Add any variables to be stored in the pre trace structure
typedef struct pre_trace_t {
} pre_trace_t;

// TODO: Choose the required synapse structure
#include <neuron/plasticity/stdp/synapse_structure/synapse_structure_weight_impl.h>

#include <neuron/plasticity/stdp/timing_dependence/timing.h>

// TODO: Ensure the correct number of weight terms is chosen
#include <neuron/plasticity/stdp/weight_dependence/weight_one_term.h>

// Include debug header for log_info etc
#include <debug.h>

// Include generic plasticity maths functions
#include <neuron/plasticity/common/maths.h>

// Exponential decay lookup parameters
#define SIN_TIME_SHIFT 0
#define SIN_SIZE 256

//---------------------------------------
// Externals
//---------------------------------------
extern int16_t sin_lookup[SIN_SIZE];
extern uint32_t peak_time;

//---------------------------------------
// Helpers
//---------------------------------------
static inline int32_t lut_sin_exp_decay(uint32_t time)
{
  // If we're before the peak, return 0
  if(time < peak_time)
  {
    return 0;
  }
  // Otherwise subtract delay and return from LUT
  else
  {
    return maths_lut_exponential_decay(time - peak_time, SIN_TIME_SHIFT, SIN_SIZE, sin_lookup);
  }
}

//---------------------------------------
// Timing dependence inline functions
//---------------------------------------
static inline post_trace_t timing_get_initial_post_trace() {

    // TODO: Return the values required
    return (post_trace_t) {};
}

//---------------------------------------
static inline post_trace_t timing_add_post_spike(
        uint32_t time, uint32_t last_time, post_trace_t last_trace) {
    use(&last_time);
    use(&last_trace);

    log_debug("\tdelta_time=%u\n", time - last_time);

    // TODO: Perform operations when a new post-spike occurs

    // Return new pre- synaptic event with decayed trace values with energy
    // for new spike added
    return (post_trace_t) {};
}

//---------------------------------------
static inline pre_trace_t timing_add_pre_spike(
        uint32_t time, uint32_t last_time, pre_trace_t last_trace) {
    use(&last_time);
    use(&last_trace);

    log_debug("\tdelta_time=%u\n", time - last_time);

    // TODO: Perform operations when a new pre-spike occurs

    return (pre_trace_t ) {};
}

//---------------------------------------
static inline update_state_t timing_apply_pre_spike(
        uint32_t time, pre_trace_t trace, uint32_t last_pre_time,
        pre_trace_t last_pre_trace, uint32_t last_post_time,
        post_trace_t last_post_trace, update_state_t previous_state) {
    use(time);
    use(&trace);
    use(last_pre_time);
    use(&last_pre_trace);
    use(last_post_time);
    use(&last_post_trace);

    // Apply fixed potentiation for each presynaptic spike
    return weight_one_term_apply_potentiation(previous_state, STDP_FIXED_POINT_ONE);
}
//---------------------------------------
// **NOTE** actually for feedback spikes
static inline update_state_t timing_apply_post_spike(
        uint32_t time, post_trace_t trace, uint32_t last_pre_time,
        pre_trace_t last_pre_trace, uint32_t last_post_time,
        post_trace_t last_post_trace, update_state_t previous_state) {
    use(&trace);
    use(&last_pre_trace);
    use(&last_post_time);
    use(&last_post_trace);

    // Get time of event relative to last pre-synaptic event
    uint32_t time_since_last_pre = time - last_pre_time;
    if (time_since_last_pre > 0) {
        int32_t decayed_sin = lut_sin_exp_decay(time_since_last_pre);

        log_debug("\t\t\ttime_since_last_pre=%u, decayed_sin=%d\n",
                  time_since_last_pre, decayed_sin);

        // Apply depression to state (which is a weight_state)
        return weight_one_term_apply_depression(previous_state, decayed_sin);
    } else {
        return previous_state;
    }
}

#endif	// _CEREBELLUM_TIMING_H_
