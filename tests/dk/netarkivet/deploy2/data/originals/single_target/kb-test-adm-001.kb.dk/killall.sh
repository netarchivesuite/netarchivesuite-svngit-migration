echo Killing all applications at: kb-test-adm-001.kb.dk
#!/bin/bash
cd /home/dev/TEST/conf/
if [ -e ./kill_GUIApplication.sh]; then 
      ./kill_GUIApplication.sh
fi
if [ -e ./kill_ArcRepositoryApplication.sh]; then 
      ./kill_ArcRepositoryApplication.sh
fi
if [ -e ./kill_BitarchiveMonitorApplication_KBBM.sh]; then 
      ./kill_BitarchiveMonitorApplication_KBBM.sh
fi
if [ -e ./kill_HarvestControllerApplication.sh]; then 
      ./kill_HarvestControllerApplication.sh
fi
