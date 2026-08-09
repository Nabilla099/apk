/*
Tiny MIDI Loader - v0.10.1 - 2023
Public domain, see LICENSE.md for details
https://github.com/scamtank/TinySoundFont
*/

#ifndef TML_HEADER_INCLUDED
#define TML_HEADER_INCLUDED

#ifdef __cplusplus
extern "C" {
#endif

typedef struct {
	unsigned int millisecond;
	unsigned char type;
	unsigned char channel;
	unsigned char key;
	unsigned char velocity;
	unsigned char cc;
	unsigned char value;
} tml_message;

#ifndef TML_NO_STDIO
#include <stdio.h>
#endif

tml_message* tml_load_filename(const char* filename);
#ifndef TML_NO_STDIO
tml_message* tml_load_file(FILE* f);
#endif
tml_message* tml_load_memory(const void* buffer, int size);
void tml_free(tml_message* m);
void tml_get_info(tml_message* m, int* out_used, int* out_instrumentcount, const char** out_copyright, const char** out_comment, unsigned int* out_millisecond);

#ifdef __cplusplus
}
#endif

#endif
