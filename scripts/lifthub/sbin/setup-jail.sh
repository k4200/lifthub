#!/bin/bash

usage() {
  echo "$0 <create|delete> <name>"
}

if [ $# -ne 2 ];
then
  usage
  exit 2
fi


NAME=$2

TMPL=/home/lifthubuser/jail-template
JAIL_ROOT=/home/lifthubuser/chroot
REPO_ROOT=/var/lib/gitosis/repositories

case "$1" in
  create)
    cp -a $TMPL $JAIL_ROOT/$NAME
    mount -t proc none $JAIL_ROOT/$NAME/proc/
    ;;
  delete)
    umount $JAIL_ROOT/$NAME/proc
    rm -rf $JAIL_ROOT/$NAME

    # This has nothing to do with the jail environment,
    # but it requires root or gitosis privilege to delete it,
    # which neither the web app nor Server Manager has,
    # so put it here for now.
    rm -rf $REPO_ROOT/$NAME.git
    ;;
  *)
    usage
    exit 2
    ;;
esac


