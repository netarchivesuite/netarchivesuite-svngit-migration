echo Killing linux application: GUIApplication
#!/bin/bash
PIDS=$(ps -wwfe | grep dk.netarkivet.common.webinterface.GUIApplication | grep -v grep | grep /home/dev/TEST/conf/settings_GUIApplication.xml | awk "{print \$2}")
if [ -n "$PIDS" ] ; then
    kill -9 $PIDS;
fi
