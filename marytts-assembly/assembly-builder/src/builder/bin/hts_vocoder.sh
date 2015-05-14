#!/bin/sh

##################################
# hts_vocoder.sh
# Author: Fabio Tesser
# Email: fabio.tesser@gmail.com
##################################

# EXIT ERROR settings 
set -o errexit

DESCRIPTION="Stand alone HTS vocoder reading parameters from files in SPTK format."

MINNUMARG=12
if [ $# -lt $MINNUMARG ]
then
  echo "NAME:
  	`basename $0`

DESCRIPTION:
    $DESCRIPTION

USAGE: 
	`basename $0` [gamma] [alpha] [useloggain] [beta] [rate] [fperiod] [mcepFile] [mcepVsize] [lf0File] [lf0Vsize] [outputfile] <strFile> <strVsize> <filtersFile> <numFilters> [playback] 
	gamma: 0 
	alpha: 0.42 
	useloggain: 1=true 0=false. Suggestion: 1   
	beta: 0.15
	rate: 16000
	fperiod:  80 (5 milisec) 
	mcepFile: filename 
    mcepVsize: vector size (75 if ...)	 
    lf0File: filename
    lf0Vsize: vector size (3 if using a file from a hmm voice training data, otherwise specify)
	outputfile: wave output filename
	
	The following are optional:
	 if using mixed excitation: 
    	strFile: filename
    	strVsize: vector size (15 if using a file from a hmm voice training data, it can be found in data/filters/mix_excitation_filters.txt, otherwise specify)
    	filtersFile: filename 
    	numFilters: 5 (if using the filters file used in the HTS-MARY demo, otherwise specify)

	playback: last argument true or false to play the file
	

EXAMPLE:
	`basename $0` 0 0.42 1 0.15 16000 80 arctic_a0005.mgc 75 arctic_a0005.lf0 3 output.wav false
 	`basename $0` 0 0.42 1 0.15 16000 80 arctic_a0005.mgc 75 arctic_a0005.lf0 3 output.wav arctic_a0005.str 15 mix_excitation_5filters_99taps_16Kz.txt 5 false"
  exit 1
fi  


BINDIR="`dirname "$0"`"
export MARY_BASE="`(cd "$BINDIR"/.. ; pwd)`"
java -showversion -ea -cp "$MARY_BASE/lib/*" marytts.htsengine.HTSVocoder $*  

