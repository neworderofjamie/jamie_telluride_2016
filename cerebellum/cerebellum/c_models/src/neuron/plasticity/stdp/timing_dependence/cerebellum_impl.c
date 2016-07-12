#include "cerebellum_impl.h"

int16_t sin_lookup[SIN_SIZE];

//---------------------------------------
// Functions
//---------------------------------------
address_t timing_initialise(address_t address) {

    log_info("timing_initialise: starting");
    log_info("\tCerebellum timing rule");

    // Copy LUTs from following memory
    address_t lut_address = maths_copy_int16_lut(&address[0], SIN_SIZE, &sin_lookup[0]);

    log_info("timing_initialise: completed successfully");

    // Return the address after the last one read
    return lut_address;
}
