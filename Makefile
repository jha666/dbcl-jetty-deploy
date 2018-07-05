include dbcl.${OS}.mk

REVISION = PA1
WSBP=bin

DBCL_JAR = C:\Users\jha\git\DbClassLoaderMaster\target\dbcl-PA3.jar

DBCL_FILES = $(WSBP)/se/independent/dbcl/jetty/DbWebAppProvider.class \
	$(WSBP)/se/independent/dbcl/jetty/DbWebAppContext.class 
	

all: lib\dbcl-jetty-deploy-$(REVISION).jar


lib\dbcl-jetty-deploy-$(REVISION).jar: lib $(DBCL_FILES) $(DBCL_JAR)	
	copy /y $(DBCL_JAR)  $@
	$(JAR) uf $@ -C bin se 
	
lib:
	mkdir lib

clean:
	$(RM-F) lib/dbcl-jetty-deploy-$(REVISION).jar
	

dist:	all
	$(ZIP) dbcl-jetty.zip etc lib modules start.d
	
%.class:

	
