/**
 * Copyright 2007 DFKI GmbH.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * This file is part of MARY TTS.
 *
 * MARY TTS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package marytts.server.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Iterator;
import java.util.Locale;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.List;
import java.util.StringTokenizer;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import marytts.datatypes.MaryDataType;
import marytts.modules.synthesis.Voice;
import marytts.server.Request;
import marytts.server.RequestHandler.StreamingOutputPiper;
import marytts.server.RequestHandler.StreamingOutputWriter;
import marytts.util.MaryRuntimeUtils;
import marytts.util.MaryUtils;
import marytts.util.data.audio.MaryAudioUtils;
import marytts.util.http.Address;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;

/**
 * Provides functionality to process synthesis http requests

 * @author Oytun T&uumlrk;
 *
 */
public class MivoqSynthesisRequestHandler extends BaseHttpRequestHandler
{
    private static int id = 0;

    private static synchronized int getId()
    {
        return id++;
    }


    private StreamingOutputWriter outputToStream;
    private StreamingOutputPiper streamToPipe;
    private PipedOutputStream pipedOutput;
    private PipedInputStream pipedInput;

    public MivoqSynthesisRequestHandler()
    {
        super();

        outputToStream = null;
        streamToPipe = null;
        pipedOutput = null;
        pipedInput = null;
    }

    @Override
    protected void handleClientRequest(String absPath, Map<String,String> queryItems, HttpResponse response, Address serverAddressAtClient)
    throws IOException
    {
/*        response.setStatusCode(HttpStatus.SC_OK);
        TestProducingNHttpEntity entity = new TestProducingNHttpEntity();
        entity.setContentType("audio/x-mp3");
        response.setEntity(entity);
        if (true) return;
  */
        logger.debug("New synthesis request: "+absPath);
        if (queryItems != null) {
            for (String key : queryItems.keySet()) {
                logger.debug("    "+key+"="+queryItems.get(key));
            }
        }
	response.setHeader("Access-Control-Allow-Origin", "*");
	// response.setHeader("Access-Control-Allow-Credentials", "true");
	// response.setHeader("Access-Control-Expose-Headers", "Cache-Control,Content-Language,Content-Type,Expires,Last-Modified,Pragma");
        process(serverAddressAtClient, queryItems, response);

    }

    protected static Map<String, HashMap<String,String>> fakeStylesByGender = null;
    static {
      if(fakeStylesByGender == null) {
      fakeStylesByGender = new HashMap<String,HashMap<String,String>>();
      HashMap<String, String> fakeStylesFemale = new HashMap<String,String>();
      HashMap<String, String> fakeStylesMale = new HashMap<String,String>();
      HashMap<String, String> fakeStylesNeutral = new HashMap<String,String>();
      // "happy" => [ "rate" => "+5%", "pitch" => "+25%" ]
      fakeStylesFemale.put("happy", "Rate(amount:0.95;)+F0Add(amount:50.0;)");
      fakeStylesNeutral.put("happy", "Rate(amount:0.95;)+F0Add(amount:37.5;)");
      fakeStylesMale.put("happy", "Rate(amount:0.95;)+F0Add(amount:25.0;)");
      // "sad" => [ "rate" => "-20%", "pitch" => "-20%" ]
      fakeStylesFemale.put("sad", "Rate(amount:1.2;)+F0Add(amount:-40.0;)");
      fakeStylesNeutral.put("sad", "Rate(amount:1.2;)+F0Add(amount:-30.0;)");
      fakeStylesMale.put("sad", "Rate(amount:1.2;)+F0Add(amount:-20.0;)");
      // "child"
      fakeStylesFemale.put("child", "HMMTractScaler(amount:1.3;)+F0Add(amount:90.0;)");
      fakeStylesNeutral.put("child", "HMMTractScaler(amount:1.3;)+F0Add(amount:95.0;)");
      fakeStylesMale.put("child", "HMMTractScaler(amout:1.3;)+F0Add(amount:100.0;)");

      fakeStylesByGender.put(Voice.FEMALE.toString(), fakeStylesFemale );
      fakeStylesByGender.put(Voice.MALE.toString(), fakeStylesMale );
      fakeStylesByGender.put(Voice.NEUTRAL.toString(), fakeStylesNeutral );
      fakeStylesByGender = Collections.unmodifiableMap(fakeStylesByGender);
      }
    }
  /*
	 Volume(amount:2.0;)
	 TractScaler(amount:1.5;)
	 HMMTractScaler(amount:1.5;)
	 F0Scale(f0Scale:2.0;)
	 F0Add(f0Add:50.0;)
	 Rate(durScale:1.5;)
	 Robot(amount:100.0;)
	 Whisper(amount:100.0;)
	 Stadium(amount:100.0;)
	 Chorus(delay1:466;amp1:0.54;delay2:600;amp2:-0.10;delay3:250;amp3:0.30;)
	 FIRFilter(type:3;fc1:500.0;fc2:2000.0;)
	 JetPilot()
  */

    public void process(Address serverAddressAtClient, Map<String, String> queryItems, HttpResponse response)
    {
        if (queryItems == null || !(
                queryItems.containsKey("input[type]")
                && queryItems.containsKey("output[type]")
                && queryItems.containsKey("input[content]")
                )) {
            MaryHttpServerUtils.errorMissingQueryParameter(response, "'input[content]' and 'input[type]' and 'output[type]'");
            return;
        }

        String inputContent = queryItems.get("input[content]");
        String inputTypeString = queryItems.get("input[type]");
        String outputTypeString = queryItems.get("output[type]");
        MaryDataType inputType = MaryDataType.get(inputTypeString);
        if (inputType == null) {
            MaryHttpServerUtils.errorWrongQueryParameterValue(response, "input[type]", inputTypeString, null);
            return;
        }

        MaryDataType outputType = MaryDataType.get(outputTypeString);
        if (outputType == null) {
            MaryHttpServerUtils.errorWrongQueryParameterValue(response, "output[type]", outputTypeString , null);
            return;
        }

	if(inputType.isTextType()) {
	  if( !queryItems.containsKey("input[locale]") ) {
            MaryHttpServerUtils.errorMissingQueryParameter(response, "'input[locale]', needed for input[type] = " + inputTypeString );
	  }
	}
        String inputLocaleString = queryItems.get("input[locale]");
        Locale inputLocale = MaryUtils.string2locale(inputLocaleString);
        if (inputLocale == null) {
            MaryHttpServerUtils.errorWrongQueryParameterValue(response, "input[locale]", inputLocaleString, null);
            return;
        }

        boolean isOutputText = true;
        boolean streamingAudio = false;
        AudioFileFormat.Type audioFileFormatType = null;
        if (outputType.name().contains("AUDIO")) {
            isOutputText = false;
            String outputFormatString = queryItems.get("output[format]");
            if (outputFormatString == null) {
                MaryHttpServerUtils.errorMissingQueryParameter(response, "'output[format]' when output[type] = AUDIO");
                return;
            }
            if (outputFormatString.endsWith("_STREAM")) {
                streamingAudio = true;
            }
            int lastUnderscore = outputFormatString.lastIndexOf('_');
            if (lastUnderscore != -1) {
                outputFormatString = outputFormatString.substring(0, lastUnderscore);
            }
            try {
                audioFileFormatType = MaryAudioUtils.getAudioFileFormatType(outputFormatString);
            } catch (Exception ex) {}
            if (audioFileFormatType == null) {
                MaryHttpServerUtils.errorWrongQueryParameterValue(response, "output[format]", outputFormatString, null);
                return;
            } else if (audioFileFormatType.toString().equals("MP3") && !MaryRuntimeUtils.canCreateMP3()) {
                MaryHttpServerUtils.errorWrongQueryParameterValue(response, "output[format]", outputFormatString, "Conversion to MP3 not supported.");
                return;
            }
            else if (audioFileFormatType.toString().equals("Vorbis") && !MaryRuntimeUtils.canCreateOgg()) {
                MaryHttpServerUtils.errorWrongQueryParameterValue(response, "output[format]", outputFormatString, "Conversion to OGG Vorbis format not supported.");
                return;
            }
        }

        Voice voice = null;
        String voiceGenderString = queryItems.get("voice[gender]");
	Voice.Gender voiceGender = null;
        if (voiceGenderString != null) {
	  if( ! (voiceGenderString.equals("male") || voiceGenderString.equals("female") || voiceGenderString.equals("neutral")) ) {
	    MaryHttpServerUtils.errorWrongQueryParameterValue(response, "voice[gender]", voiceGenderString, null);
	  }
	  voiceGender = new Voice.Gender(voiceGenderString);
	}
        String voiceAgeString = queryItems.get("voice[age]");
	int voiceAge = -1;
        if (voiceAgeString != null) {
	  voiceAge = Integer.parseInt(voiceAgeString);
	  if( voiceAge < 0 ) {
	    MaryHttpServerUtils.errorWrongQueryParameterValue(response, "voice[age]", voiceAgeString, null);
	  }
	}
        String voiceVariantString = queryItems.get("voice[variant]");
	int voiceVariant = -1;
        if (voiceVariantString != null) {
	  voiceVariant = Integer.parseInt(voiceVariantString);
	  if( voiceVariant < 0 ) {
	    MaryHttpServerUtils.errorWrongQueryParameterValue(response, "voice[variant]", voiceVariantString, null);
	  }
	}
        String utteranceStyle = queryItems.get("utterance[style]");
        if (utteranceStyle == null) {
	  utteranceStyle = "";
	}

        String voiceName = queryItems.get("voice[name]");
	String[] voiceNameList = null;
        if (voiceName != null) {
	  voiceNameList = voiceName.split(" ");
        }

        String utteranceEffects = queryItems.get("utterance[effects]");
        if (utteranceEffects == null) {
	  utteranceEffects = "";
	}
        if (utteranceEffects.length()>0)
            logger.debug("Audio effects requested: " + utteranceEffects);
        else
            logger.debug("No audio effects requested");

	// TODO(START,Parsing)

        // optionally, there may be output type parameters
        // (e.g., the list of features to produce for the output type TARGETFEATURES)
        String outputTypeParams = queryItems.get("OUTPUT_TYPE_PARAMS");

        String logMsg = queryItems.get("LOG");
        if (logMsg != null) {
            logger.info("Connection info: "+logMsg);
        }

	// TODO(END,Parsing)

	List<Voice> voiceResult = Voice.getVoiceWithSSMLAlgorythm(inputLocale, voiceGender, voiceNameList, voiceAge);
        if (voice == null) { // no voice tag -- use locale default if it exists.
            voice = Voice.getDefaultVoice(inputLocale);
            logger.debug("No voice requested -- using default " + voice);
        }
	if( voiceResult.isEmpty() ) {
            MaryHttpServerUtils.errorWrongQueryParameterValue(response, "input[] and voice[]", "No suitable voice found for the requested configuration", null);
            return;
	}
	if(voiceVariant > 0) {
	  voiceVariant--;
	  if(voiceVariant >= voiceResult.size()) {
	    voiceVariant = voiceResult.size()-1;
	  }
	} else {
	  voiceVariant = 0;
	}
	voice = voiceResult.get(voiceVariant);
	inputLocale = voice.getLocale();

	String utteranceStyleEffects = "";
	if( fakeStylesByGender.containsKey(voice.gender().toString())) {
	  HashMap<String,String> s = fakeStylesByGender.get(voice.gender().toString());
	  if( s.containsKey(utteranceStyle) ) {
	    utteranceStyleEffects = s.get(utteranceStyle);
	    if (utteranceStyleEffects.length()>0) {
	      if (utteranceEffects.length()>0) {
		utteranceEffects = utteranceStyleEffects+"+"+utteranceEffects;
	      } else {
		utteranceEffects = utteranceStyleEffects;
	      }
	      if (utteranceEffects.length()>0)
		logger.debug("Audio effects requested: " + utteranceEffects);
	      else
		logger.debug("No audio effects requested");
	    }
	  }
	}
        // Now, the parse is complete.

        // Construct audio file format -- even when output is not AUDIO,
        // in case we need to pass via audio to get our output type.
        if (audioFileFormatType == null) {
            audioFileFormatType = AudioFileFormat.Type.AU;
        }
        AudioFormat audioFormat;
        if (audioFileFormatType.toString().equals("MP3")) {
            audioFormat = MaryRuntimeUtils.getMP3AudioFormat();
        } else if (audioFileFormatType.toString().equals("Vorbis")) {
            audioFormat = MaryRuntimeUtils.getOggAudioFormat();
        } else if (voice != null) {
            audioFormat = voice.dbAudioFormat();
        } else {
            audioFormat = Voice.AF16000;
        }
        AudioFileFormat audioFileFormat = new AudioFileFormat(audioFileFormatType, audioFormat, AudioSystem.NOT_SPECIFIED);

        final Request maryRequest = new Request(inputType, outputType, inputLocale, voice, utteranceEffects, utteranceStyle, getId(), audioFileFormat, streamingAudio, outputTypeParams);

        // Process the request and send back the data
        boolean ok = true;
        try {
            maryRequest.setInputData(inputContent);
            logger.info("Read: "+inputContent);
        } catch (Exception e) {
            String message = "Problem reading input";
            logger.warn(message, e);
            MaryHttpServerUtils.errorInternalServerError(response, message, e);
            ok = false;
        }
        if (ok) {
            if (streamingAudio) {
                // Start two separate threads:
                // 1. one thread to process the request;
                new Thread("RH "+maryRequest.getId()) {
                    public void run()
                    {
                        Logger myLogger = MaryUtils.getLogger(this.getName());
                        try {
                            maryRequest.process();
                            myLogger.info("Streaming request processed successfully.");
                        } catch (Throwable t) {
                            myLogger.error("Processing failed.", t);
                        }
                    }
                }.start();

                // 2. one thread to take the audio data as it becomes available
                //    and write it into the ProducingNHttpEntity.
                // The second one does not depend on the first one practically,
                // because the AppendableSequenceAudioInputStream returned by
                // maryRequest.getAudio() was already created in the constructor of Request.
                AudioInputStream audio = maryRequest.getAudio();
                assert audio != null : "Streaming audio but no audio stream -- very strange indeed! :-(";
                AudioFileFormat.Type audioType = maryRequest.getAudioFileFormat().getType();
                AudioStreamNHttpEntity entity = new AudioStreamNHttpEntity(maryRequest);
                new Thread(entity, "HTTPWriter "+maryRequest.getId()).start();
                // entity knows its contentType, no need to set explicitly here.
                response.setEntity(entity);
                response.setStatusCode(HttpStatus.SC_OK);
                return;
            } else { // not streaming audio
                // Process input data to output data
                try {
                    maryRequest.process(); // this may take some time
                } catch (Throwable e) {
                    String message = "Processing failed.";
                    logger.error(message, e);
                    MaryHttpServerUtils.errorInternalServerError(response, message, e);
                    ok = false;
                }
                if (ok) {
                    // Write output data to client
                    try {
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        maryRequest.writeOutputData(outputStream);
                        String contentType;
                        if (maryRequest.getOutputType().isXMLType() || maryRequest.getOutputType().isTextType()) //text output
                            contentType = "text/plain; charset=UTF-8";
                        else //audio output
                            contentType = MaryHttpServerUtils.getMimeType(maryRequest.getAudioFileFormat().getType());
                        MaryHttpServerUtils.toHttpResponse(outputStream.toByteArray(), response, contentType);
                    } catch (Exception e) {
                        String message = "Cannot write output";
                        logger.warn(message, e);
                        MaryHttpServerUtils.errorInternalServerError(response, message, e);
                        ok = false;
                    }
                }
            }
        }


        if (ok)
            logger.info("Request handled successfully.");
        else
            logger.info("Request couldn't be handled successfully.");
        if (MaryRuntimeUtils.lowMemoryCondition()) {
            logger.info("Low memory condition detected (only " + MaryUtils.availableMemory() + " bytes left). Triggering garbage collection.");
            Runtime.getRuntime().gc();
            logger.info("After garbage collection: " + MaryUtils.availableMemory() + " bytes available.");
        }
    }


    protected String toRequestedAudioEffectsString(Map<String, String> keyValuePairs)
    {
        StringBuilder effects = new StringBuilder();
        StringTokenizer tt;
        Set<String> keys = keyValuePairs.keySet();
        String currentKey;
        String currentEffectName, currentEffectParams;
        for (Iterator<String> it = keys.iterator(); it.hasNext();)
        {
            currentKey = it.next();
            if (currentKey.startsWith("effect_"))
            {
                if (currentKey.endsWith("_selected"))
                {
                    if (keyValuePairs.get(currentKey).compareTo("on")==0)
                    {
                        if (effects.length()>0)
                            effects.append("+");

                        tt = new StringTokenizer(currentKey, "_");
                        if (tt.hasMoreTokens()) tt.nextToken(); //Skip "effects_"
                        if (tt.hasMoreTokens()) //The next token is the effect name
                        {
                            currentEffectName = tt.nextToken();

                            currentEffectParams = keyValuePairs.get("effect_" + currentEffectName + "_parameters");
                            if (currentEffectParams!=null && currentEffectParams.length()>0)
                                effects.append(currentEffectName).append("(").append(currentEffectParams).append(")");
                            else
                                effects.append(currentEffectName);
                        }
                    }
                }
            }
        }

        return effects.toString();
    }

}

