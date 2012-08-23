#!/bin/bash
echo --------------------------------------------
echo INSTALLING TO MACHINE: test@kb-test-adm-001.kb.dk
echo copying null.zip to:kb-test-adm-001.kb.dk
scp null.zip test@kb-test-adm-001.kb.dk:/home/test
echo deleting test@kb-test-adm-001.kb.dk:/home/test/test/lib
ssh test@kb-test-adm-001.kb.dk rm -rf /home/test/test/lib
echo unzipping null.zip at:kb-test-adm-001.kb.dk
ssh test@kb-test-adm-001.kb.dk unzip -q -o /home/test/null.zip -d /home/test/test
echo Creating directories.
ssh test@kb-test-adm-001.kb.dk "cd /home/test/test; if [ ! -d bitpreservation ]; then mkdir bitpreservation; fi; if [ ! -d tmpdircommon ]; then mkdir tmpdircommon; fi; exit; "
echo preparing for copying of settings and scripts
ssh test@kb-test-adm-001.kb.dk " cd ~; if [ -e /home/test/test/conf/jmxremote.password ]; then chmod u+rwx /home/test/test/conf/jmxremote.password; fi; "
ssh test@kb-test-adm-001.kb.dk " cd ~; if [ -e /home/test/test/conf/jmxremote.access ]; then chmod u+rwx /home/test/test/conf/jmxremote.access; fi; "
echo copying settings and scripts
scp -r kb-test-adm-001.kb.dk/* test@kb-test-adm-001.kb.dk:/home/test/test/conf/
echo Copying harvest definition database
scp tests/dk/netarkivet/deploy/data/working/database.jar test@kb-test-adm-001.kb.dk:/home/test/test/harvestdefinitionbasedir/fullhddb.jar
echo Unzipping harvest definition database
ssh test@kb-test-adm-001.kb.dk "cd /home/test/test; if [ -d harvestDatabase ]; then echo The database directory already exists. Thus database not reset.; else unzip -q -o harvestdefinitionbasedir/fullhddb.jar -d harvestDatabase; fi; exit; "
echo Unzipping archive database
ssh test@kb-test-adm-001.kb.dk "cd /home/test/test; if [ -d adminDB ]; then echo The database directory already exists. Thus database not reset.; else unzip -q -o archivedatabasedir/archivedb.jar -d adminDB; fi; exit; "
echo make scripts executable
ssh test@kb-test-adm-001.kb.dk "chmod 700 /home/test/test/conf/*.sh "
echo make password and access files readonly
ssh test@kb-test-adm-001.kb.dk "mv -f /home/test/test/conf/jmxremote.access /home/test/test/conf/access.privileges"
ssh test@kb-test-adm-001.kb.dk "mv -f /home/test/test/conf/jmxremote.password /home/test/test/./jmxremote.password"
ssh test@kb-test-adm-001.kb.dk "chmod 400 /home/test/test/./jmxremote.password"
ssh test@kb-test-adm-001.kb.dk "chmod 400 /home/test/test/conf/access.privileges"
echo --------------------------------------------
echo INSTALLING TO MACHINE: ba-test@kb-test-bar-010.bitarkiv.kb.dk
echo copying null.zip to: kb-test-bar-010.bitarkiv.kb.dk
scp null.zip ba-test@kb-test-bar-010.bitarkiv.kb.dk:
echo removing old libraries if they exist.
ssh ba-test@kb-test-bar-010.bitarkiv.kb.dk cmd /c if exist test\\lib DEL /Q test\\lib 
echo unzipping null.zip at: kb-test-bar-010.bitarkiv.kb.dk
ssh ba-test@kb-test-bar-010.bitarkiv.kb.dk cmd /c unzip.exe -q -d test -o null.zip
echo Creating directories.
scp dir_kb-test-bar-010.bitarkiv.kb.dk.bat ba-test@kb-test-bar-010.bitarkiv.kb.dk:
ssh  ba-test@kb-test-bar-010.bitarkiv.kb.dk cmd /c dir_kb-test-bar-010.bitarkiv.kb.dk.bat
ssh  ba-test@kb-test-bar-010.bitarkiv.kb.dk cmd /c del dir_kb-test-bar-010.bitarkiv.kb.dk.bat
echo preparing for copying of settings and scripts
if [ $( ssh ba-test@kb-test-bar-010.bitarkiv.kb.dk cmd /c if exist test\\conf\\jmxremote.password echo 1 ) ]; then echo Y | ssh ba-test@kb-test-bar-010.bitarkiv.kb.dk cmd /c cacls test\\conf\\jmxremote.password /P BITARKIV\\ba-test:F; fi;
if [ $( ssh ba-test@kb-test-bar-010.bitarkiv.kb.dk cmd /c if exist test\\conf\\jmxremote.access echo 1 ) ]; then echo Y | ssh ba-test@kb-test-bar-010.bitarkiv.kb.dk cmd /c cacls test\\conf\\jmxremote.access /P BITARKIV\\ba-test:F; fi;
echo copying settings and scripts
scp -r kb-test-bar-010.bitarkiv.kb.dk/* ba-test@kb-test-bar-010.bitarkiv.kb.dk:test\\conf\\
echo make password and access files readonly
ssh ba-test@kb-test-bar-010.bitarkiv.kb.dk "cmd /c move /Y test\\conf\\jmxremote.access test\\conf\\access.privileges"
ssh ba-test@kb-test-bar-010.bitarkiv.kb.dk "cmd /c move /Y test\\conf\\jmxremote.password test\\.\\jmxremote.password"
echo Y | ssh ba-test@kb-test-bar-010.bitarkiv.kb.dk "cmd /c cacls test\\.\\jmxremote.password /P BITARKIV\\ba-test:R"
echo Y | ssh ba-test@kb-test-bar-010.bitarkiv.kb.dk "cmd /c cacls test\\conf\\access.privileges /P BITARKIV\\ba-test:R"
echo --------------------------------------------
echo INSTALLING TO MACHINE: ba-test@kb-test-bar-011.bitarkiv.kb.dk
echo copying null.zip to: kb-test-bar-011.bitarkiv.kb.dk
scp null.zip ba-test@kb-test-bar-011.bitarkiv.kb.dk:
echo removing old libraries if they exist.
ssh ba-test@kb-test-bar-011.bitarkiv.kb.dk cmd /c if exist test\\lib DEL /Q test\\lib 
echo unzipping null.zip at: kb-test-bar-011.bitarkiv.kb.dk
ssh ba-test@kb-test-bar-011.bitarkiv.kb.dk cmd /c unzip.exe -q -d test -o null.zip
echo Creating directories.
scp dir_kb-test-bar-011.bitarkiv.kb.dk.bat ba-test@kb-test-bar-011.bitarkiv.kb.dk:
ssh  ba-test@kb-test-bar-011.bitarkiv.kb.dk cmd /c dir_kb-test-bar-011.bitarkiv.kb.dk.bat
ssh  ba-test@kb-test-bar-011.bitarkiv.kb.dk cmd /c del dir_kb-test-bar-011.bitarkiv.kb.dk.bat
echo preparing for copying of settings and scripts
if [ $( ssh ba-test@kb-test-bar-011.bitarkiv.kb.dk cmd /c if exist test\\conf\\jmxremote.password echo 1 ) ]; then echo Y | ssh ba-test@kb-test-bar-011.bitarkiv.kb.dk cmd /c cacls test\\conf\\jmxremote.password /P BITARKIV\\ba-test:F; fi;
if [ $( ssh ba-test@kb-test-bar-011.bitarkiv.kb.dk cmd /c if exist test\\conf\\jmxremote.access echo 1 ) ]; then echo Y | ssh ba-test@kb-test-bar-011.bitarkiv.kb.dk cmd /c cacls test\\conf\\jmxremote.access /P BITARKIV\\ba-test:F; fi;
echo copying settings and scripts
scp -r kb-test-bar-011.bitarkiv.kb.dk/* ba-test@kb-test-bar-011.bitarkiv.kb.dk:test\\conf\\
echo make password and access files readonly
ssh ba-test@kb-test-bar-011.bitarkiv.kb.dk "cmd /c move /Y test\\conf\\jmxremote.access test\\conf\\access.privileges"
ssh ba-test@kb-test-bar-011.bitarkiv.kb.dk "cmd /c move /Y test\\conf\\jmxremote.password test\\.\\jmxremote.password"
echo Y | ssh ba-test@kb-test-bar-011.bitarkiv.kb.dk "cmd /c cacls test\\.\\jmxremote.password /P BITARKIV\\ba-test:R"
echo Y | ssh ba-test@kb-test-bar-011.bitarkiv.kb.dk "cmd /c cacls test\\conf\\access.privileges /P BITARKIV\\ba-test:R"
echo --------------------------------------------
echo INSTALLING TO MACHINE: test@kb-test-har-001.kb.dk
echo copying null.zip to:kb-test-har-001.kb.dk
scp null.zip test@kb-test-har-001.kb.dk:/home/test
echo deleting test@kb-test-har-001.kb.dk:/home/test/test/lib
ssh test@kb-test-har-001.kb.dk rm -rf /home/test/test/lib
echo unzipping null.zip at:kb-test-har-001.kb.dk
ssh test@kb-test-har-001.kb.dk unzip -q -o /home/test/null.zip -d /home/test/test
echo Creating directories.
ssh test@kb-test-har-001.kb.dk "cd /home/test/test; if [ ! -d bitpreservation ]; then mkdir bitpreservation; fi; if [ ! -d tmpdircommon ]; then mkdir tmpdircommon; fi; if [ ! -d harvester_low ]; then mkdir harvester_low; fi; exit; "
echo preparing for copying of settings and scripts
ssh test@kb-test-har-001.kb.dk " cd ~; if [ -e /home/test/test/conf/jmxremote.password ]; then chmod u+rwx /home/test/test/conf/jmxremote.password; fi; "
ssh test@kb-test-har-001.kb.dk " cd ~; if [ -e /home/test/test/conf/jmxremote.access ]; then chmod u+rwx /home/test/test/conf/jmxremote.access; fi; "
echo copying settings and scripts
scp -r kb-test-har-001.kb.dk/* test@kb-test-har-001.kb.dk:/home/test/test/conf/
echo make scripts executable
ssh test@kb-test-har-001.kb.dk "chmod 700 /home/test/test/conf/*.sh "
echo make password and access files readonly
ssh test@kb-test-har-001.kb.dk "mv -f /home/test/test/conf/jmxremote.access /home/test/test/conf/access.privileges"
ssh test@kb-test-har-001.kb.dk "mv -f /home/test/test/conf/jmxremote.password /home/test/test/./jmxremote.password"
ssh test@kb-test-har-001.kb.dk "chmod 400 /home/test/test/./jmxremote.password"
ssh test@kb-test-har-001.kb.dk "chmod 400 /home/test/test/conf/access.privileges"
echo --------------------------------------------
echo INSTALLING TO MACHINE: test@kb-test-har-002.kb.dk
echo copying null.zip to:kb-test-har-002.kb.dk
scp null.zip test@kb-test-har-002.kb.dk:/home/test
echo deleting test@kb-test-har-002.kb.dk:/home/test/test/lib
ssh test@kb-test-har-002.kb.dk rm -rf /home/test/test/lib
echo unzipping null.zip at:kb-test-har-002.kb.dk
ssh test@kb-test-har-002.kb.dk unzip -q -o /home/test/null.zip -d /home/test/test
echo Creating directories.
ssh test@kb-test-har-002.kb.dk "cd /home/test/test; if [ ! -d bitpreservation ]; then mkdir bitpreservation; fi; if [ ! -d tmpdircommon ]; then mkdir tmpdircommon; fi; if [ ! -d harvester_low ]; then mkdir harvester_low; fi; if [ ! -d harvester_high ]; then mkdir harvester_high; fi; exit; "
echo preparing for copying of settings and scripts
ssh test@kb-test-har-002.kb.dk " cd ~; if [ -e /home/test/test/conf/jmxremote.password ]; then chmod u+rwx /home/test/test/conf/jmxremote.password; fi; "
ssh test@kb-test-har-002.kb.dk " cd ~; if [ -e /home/test/test/conf/jmxremote.access ]; then chmod u+rwx /home/test/test/conf/jmxremote.access; fi; "
echo copying settings and scripts
scp -r kb-test-har-002.kb.dk/* test@kb-test-har-002.kb.dk:/home/test/test/conf/
echo make scripts executable
ssh test@kb-test-har-002.kb.dk "chmod 700 /home/test/test/conf/*.sh "
echo make password and access files readonly
ssh test@kb-test-har-002.kb.dk "mv -f /home/test/test/conf/jmxremote.access /home/test/test/conf/access.privileges"
ssh test@kb-test-har-002.kb.dk "mv -f /home/test/test/conf/jmxremote.password /home/test/test/./jmxremote.password"
ssh test@kb-test-har-002.kb.dk "chmod 400 /home/test/test/./jmxremote.password"
ssh test@kb-test-har-002.kb.dk "chmod 400 /home/test/test/conf/access.privileges"
echo --------------------------------------------
echo INSTALLING TO MACHINE: test@kb-test-acs-001.kb.dk
echo copying null.zip to:kb-test-acs-001.kb.dk
scp null.zip test@kb-test-acs-001.kb.dk:/home/test
echo deleting test@kb-test-acs-001.kb.dk:/home/test/test/lib
ssh test@kb-test-acs-001.kb.dk rm -rf /home/test/test/lib
echo unzipping null.zip at:kb-test-acs-001.kb.dk
ssh test@kb-test-acs-001.kb.dk unzip -q -o /home/test/null.zip -d /home/test/test
echo Creating directories.
ssh test@kb-test-acs-001.kb.dk "cd /home/test/test; if [ ! -d bitpreservation ]; then mkdir bitpreservation; fi; if [ ! -d tmpdircommon ]; then mkdir tmpdircommon; fi; if [ ! -d viewerproxy ]; then mkdir viewerproxy; fi; if [ ! -d viewerproxy ]; then mkdir viewerproxy; fi; exit; "
echo preparing for copying of settings and scripts
ssh test@kb-test-acs-001.kb.dk " cd ~; if [ -e /home/test/test/conf/jmxremote.password ]; then chmod u+rwx /home/test/test/conf/jmxremote.password; fi; "
ssh test@kb-test-acs-001.kb.dk " cd ~; if [ -e /home/test/test/conf/jmxremote.access ]; then chmod u+rwx /home/test/test/conf/jmxremote.access; fi; "
echo copying settings and scripts
scp -r kb-test-acs-001.kb.dk/* test@kb-test-acs-001.kb.dk:/home/test/test/conf/
echo make scripts executable
ssh test@kb-test-acs-001.kb.dk "chmod 700 /home/test/test/conf/*.sh "
echo make password and access files readonly
ssh test@kb-test-acs-001.kb.dk "mv -f /home/test/test/conf/jmxremote.access /home/test/test/conf/access.privileges"
ssh test@kb-test-acs-001.kb.dk "mv -f /home/test/test/conf/jmxremote.password /home/test/test/./jmxremote.password"
ssh test@kb-test-acs-001.kb.dk "chmod 400 /home/test/test/./jmxremote.password"
ssh test@kb-test-acs-001.kb.dk "chmod 400 /home/test/test/conf/access.privileges"
echo --------------------------------------------
