# I don't remember how I input Japanese characters in Emacs...
Is this text visible to the public? Anyway, it's not really a secret.

I'm writing this text on the server with Emacs in a terminal connecting from my Windows PC. I use ATOK on my Windows PC and wanted to send Japanese characters from the client to the server, but couldn't. I know I need to add a few lines or so to my .emacs, but too much hassle... That's why I'm writing this in English.

# Overview

See [the slides](http://www.slideshare.net/k4200/lifthub-rpscala26) on SlideShare.


# How to set up the environment

## Create users

'lifthub' and 'lifthubuser' are necessary for the system.

The management application, which runs on port 8080, runs under 'lifthub' privilege. This user also does the following.

+ Set up a project
++ Copy a Lift template project (included in the Lift tarball).
++ Add some config files.
+ Access the git repository and gitosis
+ Invoke sbt
++ sbt update
++ sbt package
+ Create/delete databases
+ Create a config file for nginx

'lifthubuser' does the following.

+ Start jetty instances for user apps by calling a shell script.
+ Stop those instances using the jar file that comes with the jetty package.

## Set up gitosis

See [my blog entry](http://www.kazu.tv/blog/archives/001006.html)

$ sudo yum install gitosis

	lifthub$ ssh-keygen

Add the public key of 'lifthub' to gitosis-admin.

lifthub$ git clone gitosis@localhost:gitosis-admin.gitt

Now the gitosis.conf looks like the following.

	[group gitosis-admin]
	writable = gitosis-admin
	members = kashima@localhost.localdomain lifthub@localhost.localdomain

## Set the templates of user projects and jail environments

	[lifthub@localhost ~]$ LANG=C ls -l
	total 28
	drwxrwxr-x 5 lifthub lifthub 4096 Feb 27 17:57 gitosis-admin
	drwxrwxr-x 8 lifthub lifthub 4096 Mar  3 02:27 lifthub-sbt
	drwxrwxr-x 4 lifthub lifthub 4096 Feb 17 21:17 nginx
	drwxrwxr-x 3 lifthub lifthub 4096 Jan 17 00:46 projecttemplates
	drwxrwxr-x 2 lifthub lifthub 4096 Mar  2 16:26 sbin
	drwxrwxr-x 4 lifthub lifthub 4096 Mar  2 16:58 userprojects

	[lifthubuser@localhost ~]$ LANG=C ls -l
	total 28
	drwxr-xr-x  4 root        root        4096 Mar  2 16:58 chroot
	drwxrwxr-x 11 root        root        4096 Mar  1 14:22 jail-template
	drwxrwxr-x  8 lifthubuser lifthubuser 4096 Feb 24 12:18 lifthub-sbt
	drwxrwxr-x  2 lifthubuser lifthubuser 4096 Mar  2 16:19 sbin
	drwxrwxr-x  4 lifthubuser lifthubuser 4096 Feb 25 15:01 servers

I'll upload necessary files somewhere.

## sudoers

Add the following entries.

	lifthub     localhost = NOPASSWD:/home/lifthub/sbin/*
	lifthubuser localhost = NOPASSWD:/home/lifthubuser/sbin/*


(not finished yet)