#!/bin/bash

## This script can be used to start up a clean "local" functioning
## NetarchiveSuite with all applications running on the local machine. The only
## configuration which should be necessary is setting the paths to the JMS
## broker, JRE, and NetarchiveSuite project below.

## Path for Java 1.5.0_06 or higher, can be overridden by $JAVA or $JAVA_HOME
JAVA=${JAVA:=${JAVA_HOME:=/usr/java}}
## Path for the JMS broker, can be overriden by $IMQ
IMQ=${IMQ:=/opt/sun/mq/bin/imqbrokerd}

## ------------ The following settings normally work --------------

## The home directory for this local NetarchiveSuite relative to base dir
ARCREP_HOME=${ARCREP_HOME:=$( cd $( dirname $0 ) && pwd )}

## Path for the NetarchiveSuite base dir, can be overridden by $NETARCHIVEDIR,
## but that's usually not necessary.
NETARCHIVEDIR=${NETARCHIVEDIR:=$( cd $ARCREP_HOME/../.. && pwd )}

## How far to offset the xterms horizontally.  If you make the xterms smaller,
## change this parameter correspondingly
XTERM_HOFFSET=${XTERM_HOFFSET:=550}
## How far to offset the xterms vertically.  If you make the xterms smaller,
## change this parameter correspondingly
XTERM_VOFFSET=${XTERM_VOFFSET:=350}

## The command used to start the xterms.  By defauly, extra save lines are
## set.  If you want the windows to be smaller, you may be able to use -fs
## to change font size, if your xterm is compiled with freetype.  Otherwise,
## you might be able to use another implementation, as long as the -geometry,
## -title, -e, and -hold arguments are supported.
XTERM_CMD=${XTERM_CMD:='xterm -sl 1000'}

## ---------- No changes should be required below this line ---------------

## The below ports are only used internally on the same machine, but you
## should make sure nothing else is using them.

## Initial JMXPORT, must be ++'d after each application started, incl. SideKick
## Ports 8100-8110 are used
export JMXPORT=8100

## Initial HTTPPORT, must be ++'d after each application started
## Ports 8070-8078 are used
export HTTPPORT=8070

## Initial FILETRANSFERPORT, must be ++'d after each application started
## Ports 8040-8048 are used
export FILETRANSFERPORT=8040

## Set $KEEPDATA to non-empty to avoid cleaning data at the start of each run.
## This will make the startup process more complex, but will allow you to reuse
## what you did last time.

## Whether or not to use -hold argument
## Set $HOLD to the empty string to have windows automatically close when
## the process dies.
if [ -z "${!HOLD*}" ]; then
    HOLD=-hold
fi

## ----------- No interesting information below this line --------------

# Utility functions
function makeJmxOptions {
    echo "-Dsettings.common.jmx.port=$JMXPORT \
      -Dsettings.common.jmx.rmiPort=$(( $JMXPORT + 100 )) \
      -Dsettings.common.jmx.passwordFile=$NETARCHIVEDIR/conf/jmxremote.password";
}

function makeXtermOffset {
    echo +$(( $XTERM_HOFFSET * ($WINDOWPOS / 3) ))+$(( $XTERM_VOFFSET * ($WINDOWPOS % 3) ));
}

# Start a "normal" application (non-harvest).
# Arg 1 is the XTerm's title
# Arg 2 is the class name
# Arg 3 is any other args that need to be passed to Java.
function startApp {
    local title app termgeom otherargs;
    title=$1;
    app=$2;
    otherargs=$3;
    termgeom=$TERMSIZE`makeXtermOffset`;
    $XTERM_CMD $HOLD -geometry $termgeom -title "$title" -e $JAVA/bin/java \
        $JVM_ARGS `makeJmxOptions` -Dsettings.common.http.port=$HTTPPORT \
        -Dsettings.common.remoteFile.port=$FILETRANSFERPORT \
        -classpath $CLASSPATH $otherargs dk.netarkivet.$app &
    WINDOWPOS=$(( $WINDOWPOS + 1 ));
    JMXPORT=$(( $JMXPORT + 1 ));
    HTTPPORT=$(( $HTTPPORT + 1 ));
    FILETRANSFERPORT=$(( $FILETRANSFERPORT + 1 ))
}

## Start HarvestController w/SideKick
## Because of the SideKick system, we need a script that runs the
## HarvestControllerServer.
## Additionally, the HarvestControllerServer takes some specific parameters
## SideKick uses same placement as HarvestController, but separate JMX port
# Arg 1 is the consecutive number of this HarvestController
# Arg 2 is the priority (HIGH or LOW, corresponding to selective or snapshot)
function startHarvestApp {
    local hcsid priority prioritysetting portsetting runsetting title;
    local dirsetting hcstart startscript scriptfile;
    hcsid=$1;
    priority=$2;
    priority_settings=-Dsettings.harvester.harvesting.harvestControllerPriority=${priority}PRIORITY;
    portsetting="-Dsettings.harvester.harvesting.queuePriority=${priority}PRIORITY \
             -Dsettings.common.http.port=$HTTPPORT ";
    runsetting=-Dsettings.harvester.harvesting.isrunningFile=./hcs${hcsid}Running.tmp;
    title="Harvest Controller (Priority ${priority})";
    dirsetting="-Dsettings.harvester.harvesting.serverDir=server$hcsid \
      -Dsettings.harvester.harvesting.oldjobsDir=oldjobs$hcsid";

    hcstart="$JAVA/bin/java `makeJmxOptions` $JVM_ARGS -classpath $CLASSPATH \
      $prioritysetting $portsetting $runsetting $dirsetting \
      dk.netarkivet.harvester.harvesting.HarvestControllerApplication";
    scriptfile=./hcs${hcsid}.sh;
    startscript="$XTERM_CMD $XTERM_ARGS -geometry $TERM_SIZE`makeXtermOffset` -title \"$title\" \
      -e $hcstart &";
    echo "$startscript" > $scriptfile;
    chmod 755 $scriptfile;
    $scriptfile;
    JMXPORT=$(( $JMXPORT + 1 ));
    HTTPPORT=$(( $HTTPPORT + 1 ));

    # This starts a SideKick that re-runs the script made before
    $XTERM_CMD -geometry 40x12`makeXtermOffset` $HOLD -title SideKick \
      -e $JAVA/bin/java `makeJmxOptions` $JVM_ARGS -classpath \
      $CLASSPATH $runsetting $portsetting \
      dk.netarkivet.harvester.sidekick.SideKick \
      dk.netarkivet.harvester.sidekick.HarvestControllerServerMonitorHook \
      $scriptfile &
    JMXPORT=$(( $JMXPORT + 1 ));
    WINDOWPOS=$(( $WINDOWPOS + 1 ));
}

## Clean up and copy harvest definition data

## Remove and recopy various settings etc.
## Not done if KEEPDATA is set and we have already run at least once.
if [ -z "$KEEPDATA" -o ! -e $ARCREP_HOME/data/working ]; then
    rm -rf $ARCREP_HOME/data/working
    cp -r $ARCREP_HOME/data/originals $ARCREP_HOME/data/working
    mkdir $ARCREP_HOME/lib
    cp -r $NETARCHIVEDIR/lib/heritrix $ARCREP_HOME/lib

    ## Remove and unzip the embedded database
    rm -rf $ARCREP_HOME/data/working/harvestdefinitionbasedir/fullhddb
    unzip -ou -d $ARCREP_HOME/data/working/harvestdefinitionbasedir/ \
      $ARCREP_HOME/data/working/harvestdefinitionbasedir/fullhddb.jar

    ## Clean up other stuff left behind

    ## Clean up old logs
    rm -rf $ARCREP_HOME/log/* $ARCREP_HOME/derby.log

    ## Clean up everything else left behind by old harvests
    rm -rf $ARCREP_HOME/admin.data $ARCREP_HOME/server* \
        $ARCREP_HOME/bitarchive* $ARCREP_HOME/oldjobs* $ARCREP_HOME/cache
fi

chmod 600 $NETARCHIVEDIR/conf/jmxremote.password
mkdir -p $ARCREP_HOME/log

## Clean up log locks
rm -f $ARCREP_HOME/log/*.lck

## Clean up temporary things left behind
rm -rf $ARCREP_HOME/hcs*.sh

## Copy web pages over in case they changed.
cp -r $NETARCHIVEDIR/webpages $ARCREP_HOME/data/working

## JVM arguments for all processes
## Includes a simple indicator of the fact that this is a simple_harvest process
JVM_ARGS="-Xmx1512m -Ddk.netarkivet.settings.file=$ARCREP_HOME/settings.xml \
   -Ddk.netarkivet.monitorsettings.file=$ARCREP_HOME/monitor_settings.xml \
   -Djava.util.logging.config.file=$ARCREP_HOME/log.prop -Dsimple.harvest.indicator=0"

## Classpath
CLASSPATH=:$NETARCHIVEDIR/lib/dk.netarkivet.archive.jar:$NETARCHIVEDIR/lib/dk.netarkivet.viewerproxy.jar:$NETARCHIVEDIR/lib/dk.netarkivet.harvester.jar:$NETARCHIVEDIR/lib/dk.netarkivet.monitor.jar

export CLASSPATH

## Term size
TERM_SIZE=80x24

## Initial window placement count, must be ++'d to get a new window position
export WINDOWPOS=0

## Restart broker
##
killall -q -9 `basename $IMQ`
sleep 2
$XTERM_CMD $HOLD -geometry $TERM_SIZE+`makeXtermOffset` -title " JMS Broker" \
  -e $IMQ -reset store -tty &
WINDOWPOS=$(( $WINDOWPOS + 1 ))
echo Waiting for IMQ broker to start, please ignore messages.
while ! telnet localhost 7676 2>&1 | grep 'portmapper tcp' ; do
   sleep 1
done

## Start Bitarchive
startApp Bitarchive archive.bitarchive.BitarchiveApplication

## Start ArcRepository
startApp ArcRepository archive.arcrepository.ArcRepositoryApplication

## Start IndexServer
startApp IndexServer archive.indexserver.IndexServerApplication

## Start HarvestDefinitionGui
startApp HarvestDefinition harvester.webinterface.HarvestDefinitionApplication

# Start viewerproxy
startApp Viewerproxy viewerproxy.ViewerProxyApplication

## Start two harvesters and their sidekicks
startHarvestApp 1 LOW

startHarvestApp 2 HIGH

## Start BitarchiveMonitor
startApp BitarchiveMonitor archive.bitarchive.BitarchiveMonitorApplication
