/** Related Values Processing Framework.
 *
 * $Id: test-rvpf_xpvpc.c 2377 2014-09-16 15:04:26Z SFB $
 */
#include "rvpf_xpvpc.h"

#include <stddef.h>
#include <stdio.h>

#if defined(_WIN32)
#include <windows.h>
#define SLEEP(s) Sleep((s) * 1000)
#else
#include <unistd.h>
#define SLEEP(s) sleep(s)
#endif

#define XPVPC_ADDRESS ":11000"
#define XPVPC_ADDRESS_SSL ":11001"
#define XPVPC_USER "user"
#define XPVPC_PASSWORD "password"
#define XPVPC_TRUST_FILE "tests/config/server.crt"
#define XPVPC_CERTIFICATE_FILE "tests/config/client.pem"

#define TEST "test"
#define POINT "Test1"

extern int main(int argc, char **argv)
{
    printf("%s\n", rvpf_xpvpc_version());

    RVPF_XPVPC_Context context = rvpf_xpvpc_create();
    char *xpvpcAddress;

    if (rvpf_xpvpc_printError(context, TEST)) return -1;

    if (rvpf_ssl_enabled()) {
        printf("%s\n", rvpf_ssl_version());
        xpvpcAddress = XPVPC_ADDRESS_SSL;
        rvpf_ssl_setTrust(rvpf_xpvpc_ssl(context), XPVPC_TRUST_FILE, NULL);
        rvpf_ssl_setCertificate(rvpf_xpvpc_ssl(context), XPVPC_CERTIFICATE_FILE);
    } else xpvpcAddress = XPVPC_ADDRESS;

    rvpf_xpvpc_open(context, xpvpcAddress);
    rvpf_xpvpc_printError(context, TEST);

    if (rvpf_xpvpc_succeeded(context)) {
        rvpf_xpvpc_setClient(context, "TEST");
        rvpf_xpvpc_login(context, XPVPC_USER, XPVPC_PASSWORD);
        rvpf_xpvpc_printError(context, TEST);
    }

    if (rvpf_xpvpc_succeeded(context)) {
        rvpf_xpvpc_printError(context, TEST);
        rvpf_xpvpc_sendValue(context, POINT, "2006-01-01 01:00", NULL, "00.1234");
        rvpf_xpvpc_printError(context, TEST);
        SLEEP(2);
        rvpf_xpvpc_sendValue(context, POINT, "2006-01-01 02:00", NULL, "05.6789");
        rvpf_xpvpc_printError(context, TEST);
        rvpf_xpvpc_flush(context);
        rvpf_xpvpc_printError(context, TEST);
    }

    if (rvpf_xpvpc_succeeded(context)) {
        rvpf_xpvpc_sendValue(context, POINT, "2006-01-01 03:00", NULL, "10.1234");
        rvpf_xpvpc_printError(context, TEST);
        rvpf_xpvpc_sendValue(context, POINT, "2006-01-01 04:00", NULL, "15.6789");
        rvpf_xpvpc_printError(context, TEST);
    }

    if (rvpf_xpvpc_succeeded(context)) {
        SLEEP(2);
        rvpf_xpvpc_sendValue(context, POINT, "2006-01-01 02:00", RVPF_XPVPC_DELETED_STATE, NULL);
        rvpf_xpvpc_printError(context, TEST);
        rvpf_xpvpc_sendValue(context, POINT, "2006-01-01 05:00", NULL, "20.1234");
        rvpf_xpvpc_printError(context, TEST);
        rvpf_xpvpc_sendValue(context, POINT, "2006-01-01 06:00", NULL, "25.6789");
        rvpf_xpvpc_printError(context, TEST);
    }

    rvpf_xpvpc_close(context);
    rvpf_xpvpc_printError(context, TEST);

    rvpf_xpvpc_dispose(context);
}

// End.
