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
