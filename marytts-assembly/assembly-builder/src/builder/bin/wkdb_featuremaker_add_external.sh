#!/bin/sh

##################################
# wkdb_featuremaker_add_external.sh
# Author: Fabio Tesser
# Email: fabio.tesser@gmail.com
##################################

# EXIT ERROR settings 
set -o errexit

DESCRIPTION="The FeatureMakerAddExternal add sentences from a text file, classify them as reliable, or non-reliable (sentences with unknownWords or strangeSymbols) and extracts context features from the reliable sentences. All this extracted data will be kept in the DB."

NUMARG=2
if [ $# -ne $NUMARG ]
then
  echo "NAME:
  	`basename $0`

DESCRIPTION:
    $DESCRIPTION

USAGE: 
`basename $0` [config_file] [sentences_file---with-ABSOLUTE-PATH]
	config_file: wkdb config file  
sentences_file: file that contains the sentence to add separeted by new line.
	It is suggested to write it with absolute path, because there is
	a \"cd\" command in this script.
EXAMPLE:
	`basename $0` /home/mary/wikidb_data/wkdb.conf sentences.txt"
 
  exit 1
fi  

# read variables from config file
CONFIG_FILE="`dirname "$1"`/`basename "$1"`"
. $CONFIG_FILE

SENTENCEFILE=$2
BINDIR="`dirname "$0"`"
export MARY_BASE="`(cd "$BINDIR"/.. ; pwd)`"


# This program processes the database table: locale_cleanText.
# After processing one cleanText record it is marked as processed=true.
# If for some reason the program stops, it can be restarted and it will process
# just the not processed records.

#Usage: java FeatureMaker -locale language -mysqlHost host -mysqlUser user
#                 -mysqlPasswd passwd -mysqlDB wikiDB
#                 [-reliability strict]
#                 [-featuresForSelection phoneme,next_phoneme,selection_prosody]
#
#  required: This program requires an already created cleanText table in the DB. 
#            The cleanText table can be created with the WikipediaProcess program. 
#  default/optional: [-featuresForSelection phone,next_phone,selection_prosody] (features separated by ,) 
#  optional: [-reliability [strict|lax]]
#
#  -reliability: setting that determines what kind of sentences 
#  are regarded as reliable. There are two settings: strict and lax. With 
#  setting strict, only those sentences that contain words in the lexicon
#  or words that were transcribed by the preprocessor can be selected for the synthesis script;
#  the other sentences as unreliable. With setting lax (default), also those words that
#  are transcribed with the letter to sound component can be selected.

cd $WIKIDATAPATH

MARYTTS_BASE_LIB_PATH="$MARY_BASE/lib/"

CLASSPATH=`echo "$MARYTTS_BASE_LIB_PATH"voice*.jar "$MARYTTS_BASE_LIB_PATH"marytts-lang-*.jar "$MARYTTS_BASE_LIB_PATH"marytts-builder*.jar | tr ' ' ':'`

java -showversion -ea -cp "$CLASSPATH" marytts.tools.dbselection.FeatureMakerAddExternal \
-locale "$LOCALE" \
-mysqlHost "$MYSQLHOST" \
-mysqlUser "$MYSQLUSER" \
-mysqlPasswd "$MYSQLPASSWD" \
-mysqlDB "$MYSQLDB" \
-sentFile "$SENTENCEFILE" \
-reliability "$FEATUREMAKERRELIABILITY" \
-featuresForSelection "$FEATUREMAKERFEATURESFORSELECTION" 