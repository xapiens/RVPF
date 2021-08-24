# Related Values Processing Framework (Core).
#
# Copyright (C) 2003-2019 Serge Brisson.
#
# This software is distributable under the LGPL license.
# See details at the bottom of this file.
#
# $Id: Makefile 3961 2019-05-06 20:14:59Z SFB $

VER := 082
#SSL := 1

# Tools configuration.

ifndef OSTYPE
OSTYPE := $(shell bash -c 'echo $$OSTYPE')
ifeq "$(findstring darwin,$(OSTYPE))" "darwin"
OSTYPE := darwin
endif
endif
ifeq "$(OSTYPE)" "cygwin"
HOSTTYPE := $(shell bash -c 'echo $$HOSTTYPE')
endif

ifeq "$(OSTYPE)" "cygwin"

ifeq "$(HOSTTYPE)" "x86_64"
CC := x86_64-w64-mingw32-gcc
else
CC := i686-pc-mingw32-gcc
endif
ARFLAGS := -rcs
CFLAGS := -std=c99 -U__STRICT_ANSI__ -D_POSIX -Wall -Wmissing-prototypes -Werror -g
EXE_EXT = .exe
JAVA_INCLUDES := -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/win32
LDFLAGS := -Wl,--kill-at
LIB_EXT := .lib
LIB_PRE :=
LIBS :=
SHARED := -shared
SO_EXT := .dll
SO_PRE :=

else

ARFLAGS := -rcs
CC := gcc
CFLAGS := -std=c99 -U__STRICT_ANSI__ -D_GNU_SOURCE -fPIC -g -Wall -Wmissing-prototypes -Werror
EXE_EXT :=
LDFLAGS :=
LIB_EXT := .a
LIB_PRE := lib
LIBS := -lm
SO_PRE := lib
ifeq "$(OSTYPE)" "darwin"
JAVA_INCLUDES := -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/darwin
SHARED := -dynamiclib
SO_EXT := .dylib
else
JAVA_INCLUDES := -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux
SHARED := -shared
SO_EXT := .so
endif

endif

ifdef SSL

ifeq "$(OSTYPE)" "cygwin"
SSL_CFLAGS := -DSSL_ENABLED
SSL_LDFLAGS :=
SSL_LIBS := -lssl -lcrypto -lwsock32 -lgdi32
else ifneq "$(OSTYPE)" "darwin"
SSL_CFLAGS := -DSSL_ENABLED
SSL_LDFLAGS :=
SSL_LIBS := -lssl -lcrypto
else
SSL_CFLAGS :=
SSL_LDFLAGS :=
SSL_LIBS :=
endif

else

SSL_CFLAGS :=
SSL_LDFLAGS :=
ifeq "$(OSTYPE)" "cygwin"
SSL_LIBS := -lws2_32
else
SSL_LIBS :=
endif

endif

JAVAH := javah
ifdef JAVA_HOME
JAVAH := $(JAVA_HOME)/bin/$(JAVAH)
endif

# Workspace configuration.

ROOT_PACKAGE := org.rvpf
ROOT_PACKAGE_DIR := $(subst .,/,$(ROOT_PACKAGE))

DOCS_DB := components metadata
GUIDES_DB := config forwarder metadata rpn som

STORE_PACKAGE := $(ROOT_PACKAGE).store.server.c
STORE_PACKAGE_DIR := $(subst .,/,$(STORE_PACKAGE))
STORE_CLASS := CStore
STORE_DIR := store
STORE_FILE := CStore
STORE_IMPL_FILE := CStoreImpl
STORE_TYPES_FILE := Types
STORE_VECTOR_FILE := CStoreVector
NULL_STORE_FILE := NullStoreImpl

PROXY_STORE_IMPL_FILE := ProxyStoreImpl
HANDLES_MAP_FILE := HandlesMap

C_SRC := src/main/c
C_GEN := build/main/c
C_OBJ := build/main/obj
C_LIB := lib

LIB_SRC := $(C_SRC)/lib/impl
LIB_INCLUDE := $(C_SRC)/lib/include
LIB_OBJ := build/obj
LIB_OBJS := $(patsubst $(LIB_SRC)/%.c,$(LIB_OBJ)/%.o,$(wildcard $(LIB_SRC)/*.c))
LIB_LIB := $(LIB_PRE)rvpf$(LIB_EXT)

CLASSES_DIR := $(wildcard build/main/classes)

DOC_SRC := src/main/docs
FO_DIR := build/main/fo

T_C_SRC := src/test/c
T_EXE := tests/bin

ifdef CLASSES_DIR
STORE_CLASSPATH := $(CLASSES_DIR)
STORE_CONTAINER := $(CLASSES_DIR)/$(STORE_PACKAGE_DIR)/$(STORE_CLASS).class
else
ifeq "$(OSTYPE)" "cygwin"
STORE_CLASSPATH := lib/rvpf-core.jar;lib/rvpf-store.jar
else
STORE_CLASSPATH := lib/rvpf-core.jar:lib/rvpf-store.jar
endif
STORE_CONTAINER := lib/rvpf-store.jar lib/rvpf-core.jar
endif

ifdef DIST_MEDIA
MEDIA_DIST_DIR := $(DIST_MEDIA)/RVPF/dist/$(VER)
endif

WEB_DIR := web
WEB_DIST_DIR := ../$(WEB_DIR)/dist/$(VER)
ifdef RVPF_VER
WEB_LIB_DIR := ../../../$(RVPF_VER)/RVPF/$(WEB_DIR)/lib
else
WEB_LIB_DIR := ../$(WEB_DIR)/lib
endif

REVISION_H := $(LIB_INCLUDE)/rvpf_revision.h

DOCS_CSS := $(WEB_DIR)/docbook.css
DOCS_FIGS := $(subst $(DOC_SRC)/,,$(foreach db,$(DOCS_DB),$(wildcard $(DOC_SRC)/$(db)/figures)))
DOCS_HTML := $(foreach db,$(DOCS_DB),$(WEB_DIR)/$(db)/$(db).html)

GUIDES_CSS := $(WEB_DIR)/guides/docbook.css
GUIDES_FIGS := $(subst $(DOC_SRC)/,,$(foreach db,$(GUIDES_DB),$(wildcard $(DOC_SRC)/guides/$(db)/figures)))
GUIDES_HTML := $(foreach db,$(GUIDES_DB),$(WEB_DIR)/guides/$(db)/$(db).html)

HTML_FIGS := $(addprefix $(WEB_DIR)/,$(DOCS_FIGS) $(GUIDES_FIGS))

PIPE_EXE := $(T_EXE)/test-rvpf_pipe$(EXE_EXT)
STORE_SO := $(C_LIB)/$(SO_PRE)rvpf-c-store$(SO_EXT)
STORE_LIB := $(C_LIB)/rvpf-c-store$(LIB_EXT)
NULL_STORE_SO := $(C_LIB)/$(SO_PRE)rvpf-c-store-null$(SO_EXT)
XPVPC_EXE := $(T_EXE)/test-rvpf_xpvpc$(EXE_EXT)

VERSION := $(VER)-x

# Generic rules.

.SUFFIXES :
.SECONDARY :

# Targets.

.PHONY : all clean deploy dist distclean exe help html lib refresh sign so

all : lib so

help :
	@echo "Please specify a target:"
	@echo "	all -- Builds all executables."
	@echo "	clean -- Removes generated files."
	@echo "	deploy -- Deploys distribution files."
	@echo "	dist -- Builds all for distribution."
	@echo "	help -- Displays this."
	@echo "	html -- Builds html files from DocBook documents."
	@echo "	pipe -- Builds Pipe helper C API."
	@echo "	refresh -- Cleans, then builds everything."
	@echo "	sign -- Signs distribution files."
	@echo "	so -- Builds shared objects."
	@echo "	xpvpc -- Builds XMLPointValuesClient C API."

deploy : $(WEB_DIST_DIR)/index.html
	rsync -rt --chmod=go-w,go+rX  --delete \
	    --exclude='/dist/' \
	    --exclude='/lib/' \
	    --exclude='/gui/' \
	    --exclude='/jdbc/' \
	    --exclude='/jnlp/' \
	    --exclude='/pap/' \
	    --exclude='/valve/' \
	    --exclude='.svn' $(WEB_DIR)/ ../$(WEB_DIR)/
ifeq "$(wildcard dist/*)" ""
	@echo "The 'dist' subdirectory is empty!"
	@false
endif
	rm -f $(WEB_DIST_DIR)/rvpf-core-*
	mv dist/rvpf-core-* $(WEB_DIST_DIR)/
	./run js index $(WEB_DIST_DIR)
	@chmod -x $(WEB_DIST_DIR)/rvpf-core-*.tgz
	@chmod go-w,go+r $(WEB_DIST_DIR)/../*
	@chmod -R go-w,go+r $(WEB_LIB_DIR)/../*
ifdef DIST_MEDIA
	@mkdir -p $(MEDIA_DIST_DIR)
	rm -f $(MEDIA_DIST_DIR)/rvpf-core-*
	cp $(WEB_DIST_DIR)/rvpf-core-* $(MEDIA_DIST_DIR)/
endif

dist : all html
	@mkdir -p dist
	echo "$(VERSION)" >VERSION

sign :
	@mkdir -p dist
	@rm -f dist/rvpf-*.asc dist/rvpf-*-asc.tgz
	@find dist -name 'rvpf-*' -exec gpg --homedir ~/.gnupg/rvpf -b -u RVPF '{}' \;

so : $(STORE_SO) $(NULL_STORE_SO)

lib : $(LIB_LIB)

exe : $(PIPE_EXE) $(XPVPC_EXE)

html :  $(HTML_FIGS) $(DOCS_HTML) $(GUIDES_CSS) $(GUIDES_HTML)

refresh : clean all

clean :
	rm -f VERSION
	echo '#define RVPF_REVISION "$(RVPF_REVISION)"' >$(REVISION_H)
	rm -f $(STORE_SO) $(NULL_STORE_SO) $(STORE_LIB)
	rm -fR $(C_OBJ) $(C_GEN) $(LIB_LIB) $(LIB_OBJ) $(T_EXE)
	@rm -fR $(WEB_DIR)/components
	@rm -fR $(WEB_DIR)/metadata
	@rm -fR $(WEB_DIR)/guides
	@rm -fR $(FO_DIR)/components
	@rm -fR $(FO_DIR)/metadata
	@rm -fR $(FO_DIR)/guides

$(GUIDES_CSS) : $(DOCS_CSS)
	@mkdir -p $(WEB_DIR)/guides
	cp $< $@

$(WEB_DIST_DIR)/index.html : \
        $(WEB_DIR)/dist/index.html \
        $(WEB_DIST_DIR)/../index.html \
        $(WEB_DIST_DIR)/../style.css \
        | $(WEB_DIST_DIR)
	cp $< $@

$(WEB_DIST_DIR)/../index.html : $(WEB_DIR)/dist/root-index.html | $(WEB_DIST_DIR)
	cp $< $@

$(WEB_DIST_DIR)/../style.css : $(WEB_DIR)/style.css | $(WEB_DIST_DIR)
	cp $< $@

$(WEB_DIST_DIR) :
	mkdir -p $(WEB_DIST_DIR)

$(REVISION_H):
	svn status -u Makefile | sed -n 's/.* \([0-9]*\)$$/#define RVPF_REVISION "\1"/p' >$(REVISION_H)

$(LIB_LIB) : $(LIB_OBJS)
	$(AR) $(ARFLAGS) $@ $^

$(LIB_OBJ)/%.o : $(LIB_SRC)/%.c $(wildcard $(LIB_INCLUDE)/*.h) | $(LIB_OBJ)
	$(CC) -c -o $@ $(CFLAGS) $(SSL_CFLAGS) -I$(LIB_INCLUDE) $<

$(LIB_OBJ) :
	mkdir -p $(LIB_OBJ)

$(T_EXE)/%$(EXE_EXT) : $(T_C_SRC)/%.c $(LIB_LIB) | $(T_EXE)
	$(CC) -o $@ $(CFLAGS) -I$(LIB_INCLUDE) $(LDFLAGS) $(SSL_LDFLAGS) $< -L. -lrvpf $(LIBS) $(SSL_LIBS)

$(T_EXE) :
	mkdir -p $(T_EXE)

$(STORE_SO) : $(C_OBJ)/$(STORE_DIR)/$(STORE_FILE).o \
        $(C_OBJ)/$(STORE_DIR)/$(PROXY_STORE_IMPL_FILE).o $(STORE_LIB)
	$(CC) -o $@ $(LDFLAGS) $(SHARED) $+

$(NULL_STORE_SO) : \
        $(C_OBJ)/$(STORE_DIR)/$(NULL_STORE_FILE).o \
        $(STORE_LIB)
	$(CC) -o $@ $(LDFLAGS) $(SHARED) $+

$(STORE_LIB) : $(C_OBJ)/$(STORE_DIR)/$(STORE_IMPL_FILE).o \
        $(C_OBJ)/$(STORE_DIR)/$(HANDLES_MAP_FILE).o
	$(AR) $(ARFLAGS) $@ $+

$(C_OBJ)/$(STORE_DIR)/$(STORE_FILE).o : \
        $(C_SRC)/$(STORE_DIR)/$(STORE_FILE).c \
        $(C_GEN)/$(STORE_DIR)/$(STORE_FILE).h  \
        $(C_SRC)/$(STORE_DIR)/$(PROXY_STORE_IMPL_FILE).h \
        $(C_SRC)/$(STORE_DIR)/$(STORE_IMPL_FILE).h \
        $(C_SRC)/$(STORE_DIR)/$(STORE_TYPES_FILE).h \
        | $(C_OBJ)/$(STORE_DIR)
	$(CC) -c -o $@ $(CFLAGS) -I$(C_GEN)/$(STORE_DIR) $(JAVA_INCLUDES) $<

$(C_GEN)/$(STORE_DIR)/$(STORE_FILE).h : $(STORE_CONTAINER) | $(C_OBJ)/$(STORE_DIR)
	$(JAVAH) -o $@ -force -classpath $(STORE_CLASSPATH) $(STORE_PACKAGE).$(STORE_CLASS)

$(C_OBJ)/$(STORE_DIR)/%.o : \
        $(C_SRC)/$(STORE_DIR)/%.c \
        $(wildcard $(LIB_INCLUDE)/*.h) \
        | $(C_OBJ)/$(STORE_DIR)
	$(CC) -c -o $@ $(CFLAGS) $(JAVA_INCLUDES) $<

$(C_OBJ)/$(STORE_DIR) :
	mkdir -p $(C_OBJ)/$(STORE_DIR)

$(FO_DIR)/%.fo : $(DOC_SRC)/%.xml
	xsltproc -o $@ --nonet $(DOC_SRC)/$*-fo.xsl $<

$(FO_DIR)/%/figures : $(DOC_SRC)/%/figures
	@mkdir -p $(FO_DIR)/$*
	@rm -fR $@
	cp -R $< $@

$(WEB_DIR)/%/figures : $(DOC_SRC)/%/figures
	@mkdir -p $(WEB_DIR)/$*
	@rm -fR $@
	cp -R $< $@

$(WEB_DIR)/%.html : $(DOC_SRC)/%.xml
	xsltproc -o $@ --nonet $(DOC_SRC)/$*-html.xsl $<

# This is free software; you can redistribute it and/or modify
# it under the terms of the GNU Lesser General Public License
# version 2.1 as published by the Free Software Foundation.
#
# This software is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with this software; if not, write to the Free Software
# Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
