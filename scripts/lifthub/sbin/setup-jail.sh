#!/bin/sh

usage() {
  echo "$0 create <name> <IP address>"
  echo "$0 delete <name>"
}


case "$1" in
  create)
    if [ $# -ne 3 ];
    then
      usage
      exit 2
    fi
    ;;
  delete)
    if [ $# -ne 2 ];
    then
      usage
      exit 2
    fi
    ;;
esac


NAME=$2

JAIL_ROOT=/home/jails
RC_CONF_JAIL=/etc/rc.conf.jail

case "$1" in
  create)
    IPADDR=$3

    # network interface
    TEST=`ifconfig | grep "$IPADDR "`
    if [ -z "$TEST" ];
    then
      ifconfig lo0 alias $IPADDR netmask 255.255.255.255
      #echo "ifconfig lo0 alias $IPADDR netmask 255.255.255.255"
    fi
    # jail
    ezjail-admin create -f lifthub $NAME $IPADDR

    # write an entry for the IP address to rc.conf.jail
    TEST=`grep "$IPADDR " $RC_CONF_JAIL`
    if [ -z "$TEST" ];
    then
      N=`grep ifconfig_lo0_alias $RC_CONF_JAIL | tail -1 | sed -e 's/ifconfig_lo0_alias\([0-9]\{1,\}\).*/\1/'`
      N=`expr $N + 1`
      echo "ifconfig_lo0_alias$N=\"inet $IPADDR netmask 255.255.255.255\"" >> $RC_CONF_JAIL
      #echo "ifconfig_lo0_alias$N=\"inet $IPADDR netmask 255.255.255.255\""
    fi

    #
    /usr/opt/etc/rc.d/ezjail.sh start $NAME

    # Set up config files.
    cp -a /etc/resolv.conf $JAIL_ROOT/$NAME/etc
    echo "$IPADDR $NAME" >> $JAIL_ROOT/$NAME/etc/hosts

    ;;
  delete)
    ezjail-admin stop $NAME
    ezjail-admin delete -w $NAME
    ;;
  *)
    usage
    exit 2
    ;;
esac


