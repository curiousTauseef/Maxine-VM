/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
/**
 * @author Bernd Mathiske
 */
#include <stdarg.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include "log.h"
#include "jni.h"
#include "mutex.h"
#include "threads.h"

#if !os_GUESTVMXEN
static FILE *fileStream = NULL;
#endif

void log_assert(Boolean condition, char *conditionString, char *fileName, int lineNumber) {
    if (!condition) {
        log_println("log_assert %s[%d]: %s", fileName, lineNumber, conditionString);
        exit(1);
    }
}

static mutex_Struct log_mutexStruct;

void log_initialize(const char *path) {
    mutex_initialize(&log_mutexStruct);
#if !os_GUESTVMXEN
    if (path == NULL) {
#if TELE
        path = getenv("TELE_LOG_FILE");
#else
        path = getenv("MAXINE_LOG_FILE");
#endif
    }
    if (path == NULL) {
        path = "stdout";
    }
    if (strncmp(path, "stdout\0", 7) == 0) {
        fileStream = stdout;
        /* Set the file stream to flush whenever a newline character is encountered */
        setlinebuf(fileStream);
    } else if (strncmp(path, "stderr\0", 7) == 0) {
        fileStream = stderr;
    } else {
        fileStream = fopen(path, "w");
        if (fileStream == NULL) {
            fprintf(stderr, "Could not open file for VM output stream: %s\n", path);
            exit(1);
        }
        /* Set the file stream to flush whenever a newline character is encountered */
        setlinebuf(fileStream);
    }
#endif
}

void log_lock(void) {
	int result;
	if ((result = mutex_enter_nolog(&log_mutexStruct)) != 0) {
	    log_exit(-1, "Thread %p could not lock mutex %p: %s", thread_self(), &log_mutexStruct, strerror(result));
	}
}

void log_unlock(void) {
    int result;
	if ((result = mutex_exit_nolog(&log_mutexStruct)) != 0) {
        log_exit(-1, "Thread %p could not unlock mutex %p: %s", thread_self(), &log_mutexStruct, strerror(result));
	}
}

void log_print_format(const char *format, ...) {
    va_list ap;
    va_start(ap, format);
#if !os_GUESTVMXEN
    FILE* out = fileStream == NULL ? stdout : fileStream;
    vfprintf(out, format, ap);
#else
    vprintf(format, ap);
#endif
    va_end(ap);
}

void log_print_vformat(const char *format, va_list ap) {
#if !os_GUESTVMXEN
    FILE* out = fileStream == NULL ? stdout : fileStream;
    vfprintf(out, format, ap);
#else
    vprintf(format, ap);
#endif
}

void log_print_int(int val) {
    log_print_format("%d", val);
}

void log_print_boolean(char val) {
	if (val == 0) {
	    log_print_format("false");
    } else {
        log_print_format("true");
    }
}

void log_print_char(int val) {
	log_print_format("%c", val);
}

void log_print_long(jlong val) {
	log_print_format("%ld", val);
}

void log_print_buffer(const char *buffer) {
	log_print_format("%s", buffer);
}

void log_print_word(Address address) {
    log_print_format("%p", address);
}

void log_print_newline() {
    log_print_format(NEWLINE_STRING);
}

void log_print_float(float f) {
	// TODO: fprintf may not produce exactly the same format of floating point numbers
#if os_GUESTVMXEN
	log_print_buffer("%f not supported");
#else
	log_print_format("%f", f);
#endif
}

void log_print_double(double d) {
	// TODO: fprintf may not produce exactly the same format of floating point numbers
	log_print_format("%lf", d);

}
