#pragma once

// clang-format off

/// Transform the given argument into a string.
#if !defined(STRINGIFY)
#define STRINGIFY(a) #a
#endif


/// To explicitly mark unused attributes
#define UNUSED          __attribute__((unused))
#define MAYBE_UNUSED    [[maybe_unused]]

/// Use when the compiler cannot deduce that some code is unreachable, usually
/// to silence erroneous warnings:
/// error: control reaches end of non-void function [-Werror=return-type]
#define UNREACHABLE     __builtin_unreachable();

/// Two seemingly different pointers may point to storage locations in the same 
/// array (*aliasing*). As a result, data dependencies can arise when performing 
/// loop-based computations using pointers, as the pointers may potentially 
/// point to overlapping regions in memory. Use this macro to assure the 
/// compiler that pointer arguments never refer to overlapping memory regions.
/// Example: `void compute(double *RESTRICT a, double *RESTRICT b)`
/// More details at e.g. https://cvw.cac.cornell.edu/Vector/coding_aliasing
#define RESTRICT        __restrict__

/// By default structures are padded in C. When specifying this attribute the
/// compiler will not add any padding to the struct fields. It should be put
/// between the struct/class keyword and its name, e.g.
/// `struct PACKED MyStruct { ... };`
/// Note that unaligned access may segfault on ARM and other architectures; it
/// mainly works on x86.
#define PACKED          __attribute__((packed))

/// The [[nodiscard]] attribute can be used to indicate that the return value of 
/// a function shouldn't be ignored when you do a function call. If the return 
/// value is ignored, the compiler should give a warning on this.
/// https://en.cppreference.com/w/cpp/language/attributes/nodiscard
#define NODISCARD       [[nodiscard]]

/// Whenever a case is ended in a switch, the code of the next case will get
/// executed. This last one can be prevented by using the ´break` statement. As
/// this fallthrough behavior can introduce bugs when not intended, compilers
/// and static analyzers tend give a warning on this. Using this attribute
/// indicates that the fallthrough behavior is indeed what we want in the code.
/// https://riptutorial.com/cplusplus/example/18747/--fallthrough--
#define FALLTHROUGH     [[fallthrough]]

/// Indicate that the function does not return to the caller by either executing 
/// a return statement, or by reaching the end if it's body. Such a function 
/// may end by calling exit, or throwing an exception.
#define NORETURN        [[noreturn]]

// -------------------------------------------------------------------------- //
// function inlining

#ifndef RASDEBUG
/// Always inline a function
/// https://gcc.gnu.org/onlinedocs/gcc/Common-Function-Attributes.html#index-always_005finline-function-attribute
#define ALWAYS_INLINE   __attribute__((always_inline))
/// Inline all function calls in a function (except NOINLINE ones)
/// https://gcc.gnu.org/onlinedocs/gcc/Common-Function-Attributes.html#index-flatten-function-attribute
#define FLATTEN         __attribute__((flatten))
#else
#define ALWAYS_INLINE
#define FLATTEN
#endif
/// Never inline a function
/// https://gcc.gnu.org/onlinedocs/gcc/Common-Function-Attributes.html#index-noinline-function-attribute
#define NOINLINE        __attribute__((noinline))

// -------------------------------------------------------------------------- //
// branch prediction

/// Use in conditionals to signal that the condition is very unlikely
/// https://gcc.gnu.org/onlinedocs/gcc/Other-Builtins.html#index-_005f_005fbuiltin_005fexpect
#if !defined(unlikely)
#define unlikely(x)     __builtin_expect(!!(x), 0)
#endif
/// Use in conditionals to signal that the condition is very likely
/// https://gcc.gnu.org/onlinedocs/gcc/Other-Builtins.html#index-_005f_005fbuiltin_005fexpect
#if !defined(likely)
#define likely(x)       __builtin_expect(!!(x), 1)
#endif

// clang-format on
