echo Killing linux application: GUIApplication
#!/bin/bash
PIDS=$(ps -wwfe | grep dk.netarkivet.common.webinterface.GUIApplication | grep -v grep | grep /home/test/TEST/conf/settings_GUIApplication.xml | awk "{print \$2}")
if [ -n "$PIDS" ] ; then
    kill $PIDS;
fi

sleep 2

PIDS=$(ps -wwfe | grep dk.netarkivet.common.webinterface.GUIApplication | grep -v grep | grep /home/test/TEST/conf/settings_GUIApplication.xml | awk "{print \$2}")
if [ -n "$PIDS" ] ; then
    kill -9 $PIDS;
fi
