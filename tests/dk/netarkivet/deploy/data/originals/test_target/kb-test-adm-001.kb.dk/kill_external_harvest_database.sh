#!/bin/bash
cd /home/test/test
java -cp /home/test/test/lib/db/derbynet.jar:/home/test/test/lib/db/derby.jar org.apache.derby.drda.NetworkServerControl -p 8118 shutdown < /dev/null >> start_external_harvest_database.log 2>&1 &
