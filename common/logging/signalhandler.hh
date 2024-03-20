//
// Created by Dimitar Misev
// Copyright (c) 2018 rasdaman GmbH. All rights reserved.
//

#ifndef _COMMON_SIGNALHANDLER_HH_
#define _COMMON_SIGNALHANDLER_HH_

#include <signal.h>
#include <string>
#include <initializer_list>

namespace common
{

using SignalHandlerFunction = void (*)(int, siginfo_t *, void *);

/**
 * Functionality to help handling signals.
 */
class SignalHandler
{
public:
    /**
     * Specified signals will be ignored.
     */
    static void ignoreSignals(const std::initializer_list<int> &signals);

    /**
     * "Standard" signals will be ignored:
     * SIGHUP, SIGPIPE, SIGCONT, SIGTSTP, SIGTTIN, SIGTTOU, SIGWINCH
     */
    static void ignoreStandardSignals();

    /**
     * Specified signals will be handled by the specified handler.
     */
    static void handleSignals(const std::initializer_list<int> &signals, SignalHandlerFunction handler);

    /**
     * "Shutdown" signals will be handled by the specified handler:
     * SIGINT, SIGTERM, SIGQUIT
     */
    static void handleShutdownSignals(SignalHandlerFunction handler);

    /**
     * "Abort" signals will be handled by the specified handler:
     * SIGSEGV, SIGABRT, SIGFPE, SIGBUS
     */
    static void handleAbortSignals(SignalHandlerFunction handler);

    /**
     * Install the specified handler for a specific signal.
     */
    static void installSignalHandler(void (*handler)(int, siginfo_t *, void *), int signal);
    
    /**
     * @return a stack trace as a string; the caller address is not included.
     */
    static std::string getStackTrace();
    
    /**
     * Prints crash details: server pid, signal details, and a stacktrace.
     * This function is Async-Signal-Safe (AS-Safe), good to be called from
     * signal handlers: https://www.gnu.org/software/libc/manual/html_node/POSIX-Safety-Concepts.html#index-AS_002dSafe
     * @param info signal info
     * @param file a filepath to which to write the information; if no file 
     * is specified or set to nullptr, details will be printed to stdout.
     */
    static void printCrashDetailsASSafe(siginfo_t *info, const char *file = nullptr);
    
    /**
     * Print the given string to stdout/file in Async-Signal-Safe (AS-Safe) way, 
     * good to be called from signal handlers: 
     * https://www.gnu.org/software/libc/manual/html_node/POSIX-Safety-Concepts.html#index-AS_002dSafe
     * @param msg the string to be printed.
     * @param file a filepath to which to write the information; if no file 
     * is specified or set to nullptr, details will be printed to stdout.
     */
    static void printASSafe(const char *msg,
                            const char *file = nullptr);

    /// @return the signal name given a signal number, e.g. 9 -> SIGKILL
    static const char *signalName(int signalNumber);

private:
    
    /**
     * Add information about the signal info to the provide C-string buf.
     * The buffer must be large enough; size of 200 should be more than enough.
     * The buffer must be a valid C-string ending with a '\0'; signal details
     * are appended with strcat.
     * @param info signal info
     * @param buf a buffer large enough to contain the signal details
     */
    static void printSignalInfoASSafe(siginfo_t *info, char *buf);
    
    /**
     * Prints the current stacktrace with backtrace + backtrace_symbols_fd.
     * This function is Async-Signal-Safe (AS-Safe), good to be called from
     * signal handlers: https://www.gnu.org/software/libc/manual/html_node/POSIX-Safety-Concepts.html#index-AS_002dSafe
     * @param file a filepath to which to write the information; if no file 
     * is specified or set to nullptr, details will be printed to stdout.
     */
    static void printStackTraceASSafe(const char *file = nullptr);
    
    static void printSignalInfo(siginfo_t *info, char *buf);
    
    static void printBasicSignalInfo(siginfo_t *info, char *buf);

    static void printExtraSignalInfo(siginfo_t *info, char *buf);
    
    static void printAddress(void *ptr, char *buf);
    
    static void printInteger(long long val, char *buf);
    
    static int openFile(const char *file = nullptr);
    static void closeFile(int fd, const char *file = nullptr);
};

}  // namespace common
#endif
