#!/bin/bash
cd /home/test/TEST
java -cp /home/test/TEST/lib/db/derbynet.jar:/home/test/TEST/lib/db/derby.jar org.apache.derby.drda.NetworkServerControl -p 8118 shutdown < /dev/null >> start_external_harvest_database.log 2>&1 &
