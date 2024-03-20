#pragma once

#include "utildefs.hh"

// clang-format off

#if defined(__clang__)

#define DIAGNOSTIC_PUSH   _Pragma("clang diagnostic push")
#define IGNORE_WARNING(x) _Pragma(STRINGIFY(clang diagnostic ignored x))
#define DIAGNOSTIC_POP    _Pragma("clang diagnostic pop")

#elif defined(__GNUC__) || defined(__GNUG__)

#define DIAGNOSTIC_PUSH   _Pragma("GCC diagnostic push")
#define IGNORE_WARNING(x) _Pragma(STRINGIFY(GCC diagnostic ignored x))
#define DIAGNOSTIC_POP    _Pragma("GCC diagnostic pop")

#endif

// clang-format on
