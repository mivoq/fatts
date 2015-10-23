#!/bin/bash

##################################
# wkdb_mysql_selectedSentences_clean.sh
# Author: Fabio Tesser
# Email: fabio.tesser@gmail.com
##################################

# EXIT ERROR settings 
set -o errexit

DESCRIPTION="Clear (not drop) the selectedSentences table (useful in the procedure Inject already selected sentences)"
NUMARG=1
if [ $# -ne $NUMARG ]
then
  echo "NAME:
  	`basename $0`

DESCRIPTION:
    $DESCRIPTION

USAGE:
`basename $0` [config_file]
	config_file: wkdb config file  

EXAMPLE:
	`basename $0` /home/mary/wikidb_data/wkdb.conf"
 
  exit 1
fi  


# read variables from config file
CONFIG_FILE="`dirname "$1"`/`basename "$1"`"
. $CONFIG_FILE


mysql --user="$MYSQLUSER" --password="$MYSQLPASSWD" -e \
"use wiki; \
CREATE TABLE ${LOCALE}_${SELECTEDSENTENCESTABLENAME}_selectedSentences ( id INT NOT NULL AUTO_INCREMENT, sentence MEDIUMBLOB NOT NULL, transcription MEDIUMBLOB NOT NULL, unwanted BOOLEAN, dbselection_id INT UNSIGNED NOT NULL, primary key(id)) CHARACTER SET utf8; \
UPDATE ${LOCALE}_dbselection set selected=false;"
