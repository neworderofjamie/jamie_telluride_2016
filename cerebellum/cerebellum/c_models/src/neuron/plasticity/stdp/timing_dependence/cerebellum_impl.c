#include "cerebellum_impl.h"

int16_t sin_lookup[SIN_SIZE];
uint32_t peak_time;

//---------------------------------------
// Functions
//---------------------------------------
address_t timing_initialise(address_t address) {

    log_info("timing_initialise: starting");
    log_info("\tCerebellum timing rule");

    // this address contains peak time in units of timesteps. 
    // we added half the SIN_LUT size, since this is where the LUT peaks
    peak_time = address[0];
    log_info("\t\tPeak time:%u timesteps", peak_time);

    // Copy LUTs from following memory
    address_t lut_address = maths_copy_int16_lut(&address[1], SIN_SIZE, &sin_lookup[0]);

    log_info("timing_initialise: completed successfully");

    // Return the address after the last one read
    return lut_address;
}
