#!/bin/bash

##################################
# wkdb_mysql_set_unwanted_unreliable_sentences_by_text.sh
# Author: Fabio Tesser
# Email: fabio.tesser@gmail.com
##################################

# EXIT ERROR settings 
set -o errexit

DESCRIPTION="Set as unwanted some sentences that have text like the input files."

NUMARG=2
if [ $# -ne $NUMARG ]
then
  echo "NAME:
  	`basename $0`

DESCRIPTION:
    $DESCRIPTION

USAGE: 
`basename $0` [config_file] [id_file]
	config_file: wkdb config file  
	text_file: file that contains the text of the sentences to set unwanted
EXAMPLE:
	`basename $0` /home/mary/wikidb_data/wkdb.conf unwanted.txt"
 
  exit 1
fi  


# read variables from config file
CONFIG_FILE="`dirname "$1"`/`basename "$1"`"
. $CONFIG_FILE

FILENAME=$2

# Load text file lines into a bash array.
ID_LIST=""
OLD_IFS=$IFS
IFS=$'\n'
INIT=true
for line in $(cat $FILENAME); do
    escaped_line=$( echo ${line} |sed "s/'/\\\'/g" | sed "s/(/\\\(/g" | sed "s/)/\\\)/g")
    if [[ $INIT == true ]] ; then
	INIT=false;
    	ID_LIST="'${escaped_line}'";
    else
        ID_LIST="$ID_LIST, '${escaped_line}'";
    fi
done
IFS=$OLD_IFS

echo "UPDATE ${LOCALE}_dbselection SET unwanted=true WHERE sentence in ($ID_LIST);" 

mysql --user="$MYSQLUSER" --password="$MYSQLPASSWD" -e \
"use wiki; \
UPDATE ${LOCALE}_dbselection SET unwanted=true, reliable=false WHERE sentence in ($ID_LIST);"
