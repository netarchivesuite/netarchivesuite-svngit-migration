echo Starting all applications on: 'kb-test-har-002.kb.dk'
#!/bin/bash
cd /home/test/test/conf/
if [ -e ./start_HarvestControllerApplication_low.sh ]; then 
      ./start_HarvestControllerApplication_low.sh
fi
if [ -e ./start_HarvestControllerApplication_high.sh ]; then 
      ./start_HarvestControllerApplication_high.sh
fi