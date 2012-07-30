echo Killing linux application: BitarchiveApplication
#!/bin/bash
PIDS=$(ps -wwfe | grep dk.netarkivet.archive.bitarchive.BitarchiveApplication | grep -v grep | grep /home/netarkiv/test/conf/settings_BitarchiveApplication.xml | awk "{print \$2}")
if [ -n "$PIDS" ] ; then
    kill $PIDS;
fi

sleep 2

PIDS=$(ps -wwfe | grep dk.netarkivet.archive.bitarchive.BitarchiveApplication | grep -v grep | grep /home/netarkiv/test/conf/settings_BitarchiveApplication.xml | awk "{print \$2}")
if [ -n "$PIDS" ] ; then
    kill -9 $PIDS;
fi
