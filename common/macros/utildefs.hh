#pragma once

/// Transform the given argument into a string, e.g. arg -> "arg"
#if !defined(STRINGIFY)
#define STRINGIFY(a) #a
#endif

// -------------------------------------------------------------------------- //
// classes

/// Disable copy constructor and copy assignment operator
#define DISABLE_COPY(className)            \
    className(const className &) = delete; \
    className &operator=(const className &) = delete;

/// Disable move constructor and move assignment operator
#define DISABLE_MOVE(className)       \
    className(className &&) = delete; \
    className &operator=(className &&) = delete;

/// Disable copy constructor and copy assignment operator, as well as
/// move constructor and move assignment operator
#define DISABLE_COPY_AND_MOVE(className) \
    DISABLE_COPY(className)              \
    DISABLE_MOVE(className)

#define DISABLE_INSTANTIATION(className) \
    className() = delete;

// -------------------------------------------------------------------------- //

/// For testing purposes, only enabled in Debug build to prevent problems from
/// any accidental commits with this left in the code.
#ifdef RASDEBUG
#define CAUSE_SEGFAULT *(int*)0 = 0;
#else
#define CAUSE_SEGFAULT
#endif


// -------------------------------------------------------------------------- //

// Round up x to the closes multiple of s
#define ROUND_UP(x, s) (((x) + ((s)-1)) & -(s))
