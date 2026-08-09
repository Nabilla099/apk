/*
TinySoundFont - v0.12.1 - 2023
Public domain, see LICENSE.md for details
https://github.com/scamtank/TinySoundFont
*/

#ifndef TSF_HEADER_INCLUDED
#define TSF_HEADER_INCLUDED

#ifdef __cplusplus
extern "C" {
#endif

#include "tml.h"

typedef struct tsf tsf;

#ifndef TSF_NO_STDIO
#include <stdio.h>
#endif

tsf* tsf_load_filename(const char* filename);
#ifndef TSF_NO_STDIO
tsf* tsf_load_file(FILE* f);
#endif
tsf* tsf_load_memory(const void* buffer, int size);
tsf* tsf_copy(tsf* f);
void tsf_free(tsf* f);
void tsf_close(tsf* f);

int tsf_get_presetcount(tsf* f);
const char* tsf_get_presetname(tsf* f, int preset);

void tsf_set_output(tsf* f, int outputmode, int samplerate, float globalgaindb);
void tsf_note_on(tsf* f, int preset, int key, float vel);
void tsf_note_off(tsf* f, int preset, int key);
void tsf_note_off_all(tsf* f);
void tsf_render_short(tsf* f, short* outputBuffer, int numSamples, int flag);
void tsf_render_float(tsf* f, float* outputBuffer, int numSamples, int flag);
void tsf_reset(tsf* f);

void tsf_channel_set_bank_preset(tsf* f, int channel, int bank, int preset);
void tsf_channel_set_presetnumber(tsf* f, int channel, int preset, int midipreset);
void tsf_channel_midi_control(tsf* f, int channel, int controller, int control);
void tsf_channel_note_on(tsf* f, int channel, int key, float vel);
void tsf_channel_note_off(tsf* f, int channel, int key);
void tsf_channel_note_off_all(tsf* f, int channel);
void tsf_channel_sounds_off_all(tsf* f, int channel);
void tsf_channel_set_pitchwheel(tsf* f, int channel, int pitch);

int tsf_get_channel_presetindex(tsf* f, int channel);
void tsf_set_volume(tsf* f, float gaindb);

#ifdef __cplusplus
}
#endif

#endif
