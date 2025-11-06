#pragma once

// Minimal configuration header for building Presage on Android.
// Values mirror the defaults produced by autotools on Linux while
// disabling optional X11 dependencies that are not relevant on Android.

#define HAVE_CONFIG_H 1

#define HAVE_DIRENT_H 1
#define HAVE_DLFCN_H 1
#define HAVE_INTTYPES_H 1
#define HAVE_MEMORY_H 1
#define HAVE_PTHREAD_H 1
#define HAVE_STDINT_H 1
#define HAVE_STDLIB_H 1
#define HAVE_STRING_H 1
#define HAVE_STRINGS_H 1
#define HAVE_SYS_STAT_H 1
#define HAVE_SYS_TYPES_H 1
#define HAVE_UNISTD_H 1

#define STDC_HEADERS 1
#define TIXML_USE_STL 1
/* SQLite support is disabled in this Android build. */
#undef HAVE_SQLITE3_H
#undef USE_SQLITE

#define PACKAGE "presage"
#define PACKAGE_NAME "presage"
#define PACKAGE_STRING "presage 0.9.1"
#define PACKAGE_TARNAME "presage"
#define PACKAGE_VERSION "0.9.1"
#define PACKAGE_BUGREPORT ""
#define PACKAGE_URL ""

#define VERSION "0.9.1"
