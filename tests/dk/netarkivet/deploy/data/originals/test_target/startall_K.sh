#!/bin/bash
echo --------------------------------------------
echo STARTING MACHINE: test@kb-test-adm-001.kb.dk
ssh test@kb-test-adm-001.kb.dk ". /etc/profile; /home/test/test/conf/startall.sh; sleep 5; cat /home/test/test/*.log"
echo --------------------------------------------
echo STARTING MACHINE: ba-test@kb-test-bar-010.bitarkiv.kb.dk
ssh ba-test@kb-test-bar-010.bitarkiv.kb.dk "cmd /c  test\conf\startall.bat " 
sleep 5
ssh ba-test@kb-test-bar-010.bitarkiv.kb.dk "type test\start_BitarchiveApplication.log" 
echo --------------------------------------------
echo STARTING MACHINE: ba-test@kb-test-bar-011.bitarkiv.kb.dk
ssh ba-test@kb-test-bar-011.bitarkiv.kb.dk "cmd /c  test\conf\startall.bat " 
sleep 5
ssh ba-test@kb-test-bar-011.bitarkiv.kb.dk "type test\start_BitarchiveApplication.log" 
echo --------------------------------------------
echo STARTING MACHINE: test@kb-test-har-001.kb.dk
ssh test@kb-test-har-001.kb.dk ". /etc/profile; /home/test/test/conf/startall.sh; sleep 5; cat /home/test/test/*.log"
echo --------------------------------------------
echo STARTING MACHINE: test@kb-test-har-002.kb.dk
ssh test@kb-test-har-002.kb.dk ". /etc/profile; /home/test/test/conf/startall.sh; sleep 5; cat /home/test/test/*.log"
echo --------------------------------------------
echo STARTING MACHINE: test@kb-test-acs-001.kb.dk
ssh test@kb-test-acs-001.kb.dk ". /etc/profile; /home/test/test/conf/startall.sh; sleep 5; cat /home/test/test/*.log"
echo --------------------------------------------
