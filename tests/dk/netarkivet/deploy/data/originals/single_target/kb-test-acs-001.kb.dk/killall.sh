echo Killing all applications on: 'kb-test-acs-001.kb.dk'
#!/bin/bash
cd /home/dev/TEST/conf/
if [ -e ./kill_ViewerProxyApplication.sh ]; then 
      ./kill_ViewerProxyApplication.sh
fi
if [ -e ./kill_ViewerProxyApplication.sh ]; then 
      ./kill_ViewerProxyApplication.sh
fi
if [ -e ./kill_IndexServerApplication.sh ]; then 
      ./kill_IndexServerApplication.sh
fi
