! $Id: descrip.mms 1050 2009-01-16 20:34:58Z SFB $

MODULES = RVPF_PIPE, RVPF_XPVPC, RVPF_SSL
TESTS = TEST-RVPF_PIPE.EXE TEST-RVPF_XPVPC.EXE

ALL : RVPF.OLB($(MODULES)) $(TESTS)
 ! All is done.

RVPF_PIPE.OBJ : RVPF_PIPE.H

RVPF_SSL.OBJ : RVPF_SSL.H

RVPF_XPVPC.OBJ : RVPF_XPVPC.H RVPF_SSL.H

TEST-RVPF_PIPE.EXE : TEST-RVPF_PIPE.OBJ RVPF.OLB
	LINK $(MMS$SOURCE), RVPF/LIBRARY

TEST-RVPF_XPVPC.EXE : TEST-RVPF_XPVPC.OBJ RVPF.OLB
	LINK $(MMS$SOURCE), RVPF/LIBRARY

CLEAN :
	@ DELETE/LOG RVPF.OLB;*,*.obj;*,*.exe;*
	@ PURGE/LOG [...]

! End.
