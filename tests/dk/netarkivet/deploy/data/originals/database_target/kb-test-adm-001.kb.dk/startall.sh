#!/bin/bash
cd /home/test/TEST/conf/
echo Starting external harvest database.
if [ -e ./start_external_hd_database.sh ]; then
      ./start_external_hd_database.sh &
      sleep 5
fi
echo Starting external admin database.
if [ -e ./start_external_admin_database.sh ]; then
      ./start_external_admin_database.sh &
      sleep 5
fi
echo Starting all applications on: 'kb-test-adm-001.kb.dk'
if [ -e ./start_GUIApplication.sh ]; then 
      ./start_GUIApplication.sh
fi
if [ -e ./start_ArcRepositoryApplication.sh ]; then 
      ./start_ArcRepositoryApplication.sh
fi
if [ -e ./start_BitarchiveMonitorApplication.sh ]; then 
      ./start_BitarchiveMonitorApplication.sh
fi
if [ -e ./start_BitarchiveMonitorApplication.sh ]; then 
      ./start_BitarchiveMonitorApplication.sh
fi
