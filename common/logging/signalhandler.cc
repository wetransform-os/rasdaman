//
// Created by Dimitar Misev
// Copyright (c) 2018 rasdaman GmbH. All rights reserved.
//

#include "signalhandler.hh"
#include "stacktrace.hh"
#include "common/string/stringutil.hh"
#include <logging.hh>
#include <sys/types.h>
#include <unistd.h>
#include <memory>
#include <fcntl.h>

namespace common
{

void SignalHandler::ignoreSignals(const std::initializer_list<int> &signals)
{
    for (auto s: signals)
    {
        signal(s, SIG_IGN);
    }
}

void SignalHandler::ignoreStandardSignals()
{
    ignoreSignals({SIGHUP, SIGPIPE, SIGCONT, SIGTSTP, SIGTTIN, SIGTTOU, SIGWINCH});
}

void SignalHandler::handleSignals(const std::initializer_list<int> &signals, SignalHandlerFunction handler)
{
    for (auto signal: signals)
    {
        installSignalHandler(handler, signal);
    }
}

void SignalHandler::handleShutdownSignals(SignalHandlerFunction handler)
{
    handleSignals({SIGINT, SIGTERM, SIGQUIT}, handler);
}

void SignalHandler::handleAbortSignals(SignalHandlerFunction handler)
{
    // handling SIGABRT puts rasserver in an infinite loop
    handleSignals({SIGSEGV, /* SIGABRT, */ SIGFPE, SIGILL, SIGSYS, SIGXCPU, SIGXFSZ}, handler);
}

void SignalHandler::installSignalHandler(void (*handler)(int, siginfo_t *, void *), int signal)
{
    struct sigaction sigact;

    //setup the handling function
    sigact.sa_sigaction = handler;

    sigact.sa_flags = SA_RESTART | SA_SIGINFO;
    sigemptyset(&sigact.sa_mask);

    errno = 0;
    int retVal = sigaction(signal, &sigact, (struct sigaction *)NULL);
    if (retVal != 0)
    {
        LERROR << "Installing handler for signal " << signal << " failed: " << strerror(errno);
    }
}

std::string SignalHandler::getStackTrace()
{
    common::stacktrace::StackTrace currTrace;
    // skip current line, plus the caller's line
    return currTrace.toString(2);
}

void SignalHandler::printCrashDetailsASSafe(siginfo_t *info, const char *file)
{
    // print signal details
    static const size_t signalInfoMaxSize = 200;
    char signalInfo[signalInfoMaxSize];
    strcpy(signalInfo, "Server with pid ");
    printInteger(getpid(), signalInfo);
    strcat(signalInfo, " interrupted by signal ");
    common::SignalHandler::printSignalInfoASSafe(info, signalInfo);
    strcat(signalInfo, "... stacktrace:\n");
    printASSafe(signalInfo, file);
    
    // print stack trace
    common::SignalHandler::printStackTraceASSafe(file);
}

void SignalHandler::printSignalInfoASSafe(siginfo_t *info, char *buf)
{
    if (info == NULL)
        return;

    strcat(buf, signalName(info->si_signo));
    strcat(buf,  " (");
    printSignalInfo(info, buf);
    strcat(buf, ")");
}

void SignalHandler::printASSafe(const char *msg, const char *file)
{
    int fd = openFile(file);
    write(fd, msg, strlen(msg));
    closeFile(fd, file);
}

void SignalHandler::printStackTraceASSafe(const char *file)
{
    static const size_t TRACE_MAX = 100;
    void *buffer[TRACE_MAX];
    memset(buffer, 0, sizeof(buffer));
    int size = backtrace(buffer, static_cast<int>(sizeof(buffer) / sizeof(void*)));
    int fd = openFile(file);
    backtrace_symbols_fd(buffer+3, size, fd);
    closeFile(fd, file);
}

const char *SignalHandler::signalName(int signalNumber)
{
    switch (signalNumber)
    {
    // Default action: Abnormal termination of the process.
    case SIGABRT: return "SIGABRT";
    case SIGILL: return "SIGILL";
    case SIGBUS: return "SIGBUS";
    case SIGFPE: return "SIGFPE";
    case SIGSEGV: return "SIGSEGV";
    case SIGQUIT: return "SIGQUIT";
    case SIGTRAP: return "SIGTRAP";
    case SIGSYS: return "SIGSYS";
    case SIGXCPU: return "SIGXCPU";
    case SIGXFSZ: return "SIGXFSZ";
    // Default action: Abnormal termination of the process. The process is terminated with all
    // the consequences of _exit() except that the status made available to
    // wait() and waitpid() indicates abnormal termination by the specified signal.
    case SIGALRM: return "SIGALRM";
    case SIGHUP: return "SIGHUP";
    case SIGINT: return "SIGINT";
    case SIGKILL: return "SIGKILL";
    case SIGTERM: return "SIGTERM";
    case SIGPIPE: return "SIGPIPE";
    case SIGUSR1: return "SIGUSR1";
    case SIGUSR2: return "SIGUSR2";
    case SIGPOLL: return "SIGPOLL";
    case SIGPROF: return "SIGPROF";
    case SIGVTALRM: return "SIGVTALRM";
    // Default action: Stop the process.
    case SIGSTOP: return "SIGSTOP";
    case SIGTSTP: return "SIGTSTP";
    case SIGTTIN: return "SIGTTIN";
    case SIGTTOU: return "SIGTTOU";
    // Default action: Continue the process.
    case SIGCONT: return "SIGCONT";
    // Default action: Ignore the signal.
    case SIGCHLD: return "SIGCHLD";
    case SIGURG: return "SIGURG";
    case SIGWINCH: return "SIGWINCH";
    // Shouldn't happen
    default: return "UNKNOWN";
    }
}

void SignalHandler::printSignalInfo(siginfo_t *info, char *buf)
{
    if (info == NULL)
        return;
    auto lenBefore = strlen(buf);
    printBasicSignalInfo(info, buf);
    auto lenAfter = strlen(buf);
    if (lenAfter > lenBefore)
        strcat(buf, ": ");
    printExtraSignalInfo(info, buf);
}

void SignalHandler::printBasicSignalInfo(siginfo_t *info, char *buf)
{
    if (info == NULL)
        return;

    const char *ret = "";
    switch (info->si_signo)
    {
    case SIGABRT: ret = "Abnormal termination"; break;
    case SIGILL: ret = "Illegal instruction"; break;
    case SIGBUS: ret = "Access to an undefined portion of a memory object"; break;
    case SIGFPE: ret = "Erroneous arithmetic operation"; break;
    case SIGSEGV: ret = ""; break;
    case SIGQUIT: ret = "Terminal quit signal"; break;
    case SIGTRAP: ret = ""; break;
    case SIGSYS: ret = "Bad system call"; break;
    case SIGXCPU: ret = "CPU time limit exceeded"; break;
    case SIGXFSZ: ret = "File size limit exceeded"; break;
    case SIGALRM: ret = "Alarm clock"; break;
    case SIGHUP: ret = "Hangup"; break;
    case SIGINT: ret = "Terminal interrupt signal"; break;
    case SIGKILL: ret = "Kill, cannot be caught or ignored"; break;
    case SIGTERM: ret = "Terminate execution"; break;
    case SIGPIPE: ret = "Write on a pipe with no one to read it"; break;
    case SIGUSR1: ret = "User-defined signal 1"; break;
    case SIGUSR2: ret = "User-defined signal 2"; break;
    case SIGPOLL: ret = "Pollable event"; break;
    case SIGPROF: ret = "Profiling timer expired"; break;
    case SIGVTALRM: ret = "Virtual timer expired"; break;
    case SIGSTOP: ret = "Stop executing, cannot be caught or ignored"; break;
    case SIGTSTP: ret = "Terminal stop signal"; break;
    case SIGTTIN: ret = "Background process attempting read"; break;
    case SIGTTOU: ret = "Background process attempting write"; break;
    case SIGCONT: ret = "Continue execution, if stopped"; break;
    case SIGCHLD: ret = ""; break;
    case SIGURG: ret = "High bandwidth data is available at a socket"; break;
    case SIGWINCH: ret = "Window size change"; break;
    // Shouldn't happen
    default: ret = ""; break;
    }
    strcat(buf, ret);
}

void SignalHandler::printExtraSignalInfo(siginfo_t *info, char *buf)
{
    if (info == NULL)
        return;

    bool retSet = true;
    
    switch (info->si_signo)
    {
    case SIGILL:
    {
        const char *ret = "";
        switch (info->si_code)
        {
        case ILL_ILLOPC: ret = "Illegal opcode "; break;
        case ILL_ILLOPN: ret = "Illegal operand "; break;
        case ILL_ILLADR: ret = "Illegal addressing mode "; break;
        case ILL_ILLTRP: ret = "Illegal trap "; break;
        case ILL_PRVOPC: ret = "Privileged opcode "; break;
        case ILL_PRVREG: ret = "Privileged register "; break;
        case ILL_COPROC: ret = "Coprocessor error "; break;
        case ILL_BADSTK: ret = "Internal stack error "; break;
        default: break;
        }
        strcat(buf, ret);
        strcat(buf, "at address ");
        printAddress(info->si_addr, buf);
        break;
    }
    case SIGBUS:
    {
        const char *ret = "";
        switch (info->si_code)
        {
        case BUS_ADRALN: ret = "Invalid address alignment"; break;
        case BUS_ADRERR: ret = "Nonexistent physical address"; break;
        case BUS_OBJERR: ret = "Object-specific hardware error"; break;
        default: break;
        }
        retSet = strlen(ret) > 0;
        strcat(buf, ret);
        break;
    }
    case SIGFPE:
    {
        const char *ret = "";
        switch (info->si_code)
        {
        case FPE_INTDIV: ret = "Integer divide by zero"; break;
        case FPE_INTOVF: ret = "Integer overflow"; break;
        case FPE_FLTDIV: ret = "Floating-point divide by zero"; break;
        case FPE_FLTOVF: ret = "Floating-point overflow"; break;
        case FPE_FLTUND: ret = "Floating-point underflow"; break;
        case FPE_FLTRES: ret = "Floating-point inexact result"; break;
        case FPE_FLTINV: ret = "Invalid floating-point operation"; break;
        case FPE_FLTSUB: ret = "Subscript out of range"; break;
        default: break;
        }
        retSet = strlen(ret) > 0;
        strcat(buf, ret);
        break;
    }
    case SIGSEGV:
    {
        const char *ret = "";
        switch (info->si_code)
        {
        case SEGV_ACCERR: ret = "Invalid permissions for memory access "; break;
        case SEGV_MAPERR:
        default: ret = "Invalid memory access "; break;
        }
        strcat(buf, ret);
        strcat(buf, "at address ");
        printAddress(info->si_addr, buf);
        break;
    }
    case SIGTRAP:
    {
        const char *ret = "";
        switch (info->si_code)
        {
        case TRAP_BRKPT: ret = "Process breakpoint"; break;
        case TRAP_TRACE: ret = "Process trace trap"; break;
        default: break;
        }
        retSet = strlen(ret) > 0;
        strcat(buf, ret);
        break;
    }
    case SIGPOLL:
    {
        const char *ret = "";
        long band = 0;
        bool bandSet = false;
        switch (info->si_code)
        {
        case POLL_IN: ret = "Data input available, band event "; band = info->si_band; bandSet = true; break;
        case POLL_OUT: ret = "Output buffers available, band event "; band = info->si_band; bandSet = true;; break;
        case POLL_MSG: ret = "Input message available, band event "; band = info->si_band; bandSet = true;; break;
        case POLL_ERR: ret = "I/O error"; break;
        case POLL_PRI: ret = "High priority input available"; break;
        case POLL_HUP: ret = "Device disconnected"; break;
        default: break;
        }
        retSet = strlen(ret) > 0;
        strcat(buf, ret);
        if (bandSet)
            printInteger(band, buf);
        break;
    }
    case SIGCHLD:
    {
        const char *ret = "";
        switch (info->si_code)
        {
        case CLD_EXITED: ret = "Child exited"; break;
        case CLD_KILLED: ret = "Child aborted and did not create a core file"; break;
        case CLD_DUMPED: ret = "Child aborted and created a core file"; break;
        case CLD_TRAPPED: ret = "Child trapped"; break;
        case CLD_STOPPED: ret = "Child stopped"; break;
        case CLD_CONTINUED: ret = "Child continued"; break;
        default: ret = "Child process terminated, stopped, or continued"; break;
        }
        strcat(buf, ret);
        strcat(buf, " [child pid: ");
        printInteger(info->si_pid, buf);
        strcat(buf, ", exit code: ");
        printInteger(info->si_status, buf);
        strcat(buf, ", uid: ");
        printInteger(info->si_uid, buf);
        strcat(buf, "]");
        break;
    }
    default: break;
    }

    {
        const char *tmp = "";
        switch (info->si_code)
        {
        case SI_USER: tmp = "sent by kill()"; break;
        case SI_QUEUE: tmp = "sent by sigqueue()"; break;
        case SI_TIMER: tmp = "timer set by timer_settime() expired"; break;
        case SI_ASYNCIO: tmp = "asynchronous I/O request completed"; break;
        case SI_MESGQ: tmp = "a message arrived on an empty message queue"; break;
        case SI_KERNEL: tmp = "sent by kernel"; break;
        default: break;
        }
        if (strlen(tmp) > 0)
        {
            if (retSet)
                strcat(buf, ", ");
            strcat(buf, tmp);
            retSet = true;
        }
    }

    if (info->si_signo != SIGCHLD &&
        (info->si_code == SI_USER || info->si_code == SI_QUEUE || info->si_code < 0) &&
        info->si_pid > 0)
    {
        if (retSet)
            strcat(buf, ", ");
        if (info->si_pid == getpid())
        {
            strcat(buf, "Raised internally by this process");
        }
        else
        {
            strcat(buf, "Sent by process with pid ");
            printInteger(info->si_pid, buf);
            strcat(buf, " run by user with uid ");
            printInteger(info->si_uid, buf);
        }
        retSet = true;
    }

    if (info->si_errno != 0)
    {
        if (retSet)
            strcat(buf, ", ");
        strcat(buf, "Associated error: ");
        printInteger(info->si_errno, buf);
    }
}

void SignalHandler::printAddress(void *ptr, char *buf)
{
    auto n = strlen(buf);
    char *p = common::StringUtil::uintToString(intptr_t(ptr), buf + n);
    p[0] = '\0';
}

void SignalHandler::printInteger(long long val, char *buf)
{
    auto n = strlen(buf);
    char *p = common::StringUtil::uintToString(val, buf + n);
    p[0] = '\0';
}

int SignalHandler::openFile(const char *file)
{
    int fd = STDERR_FILENO;
    if (file != nullptr)
        fd = open(file, O_WRONLY | O_APPEND);
    if (errno != 0)
        fd = STDERR_FILENO;
    return fd;
}

void SignalHandler::closeFile(int fd, const char *file)
{
    if (file != nullptr)
        close(fd);
}


}  // namespace common
