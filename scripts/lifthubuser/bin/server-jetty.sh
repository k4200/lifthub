#!/bin/sh
#
# Port number is fixed.
#


usage() {
  echo "$0 <start|stop> <name>"
  echo "$0 <kill|clean> <name>"
}


NAME=$2

JETTY_HOME=/usr/opt/jetty/jetty-6
JAVA=/usr/opt/openjdk6/bin/java
STOP_PORT=10000
# ServerManager monitors this log, so don't change the name.
LOG=/home/lifthubuser/logs/server-exec.log
PID_FILE=/home/lifthubuser/logs/$NAME.pid
CONF=/home/lifthubuser/etc/jetty.xml

JAIL_ID=`jls | grep jails/$NAME | awk '{print $1}'`

cd /usr/opt/jetty/jetty-6


case "$1" in
  start)
    echo "starting..."
    $JAVA -DSTOP.PORT=$STOP_PORT -DSTOP.KEY=$NAME \
    -Drun.mode=production -jar $JETTY_HOME/start.jar $CONF > $LOG 2>&1 &
    ;;
  stop)
    echo "stopping..."
    $JAVA -DSTOP.PORT=$STOP_PORT -DSTOP.KEY=$NAME -jar $JETTY_HOME/start.jar --stop > $LOG 2>&1
    ;;
  kill)
    kill -9 `cat $PID_FILE`
    ;;
  clean)
    echo "clean"
    ps aux | grep "STOP.KEY=$NAME" | grep -v grep | awk '{print $2}' | xargs kill -9
    rm -rf /tmp/Jetty*
    ;;
  *)
    usage
    exit 2
    ;;
esac

