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
import marytts.server.http.params.effects.*;

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

import org.json.*;

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
      fakeStylesFemale.put("happy", "[{'Rate':0.95,'F0Add':50.0}]");
      fakeStylesNeutral.put("happy", "[{'Rate':0.95,'F0Add':37.5}]");
      fakeStylesMale.put("happy", "[{'Rate':0.95,'F0Add':25.0}]");
      // "sad" => [ "rate" => "-20%", "pitch" => "-20%" ]
      fakeStylesFemale.put("sad", "[{'Rate':1.2,'F0Add':-40.0}]");
      fakeStylesNeutral.put("sad", "[{'Rate':1.2,'F0Add':-30.0}]");
      fakeStylesMale.put("sad", "[{'Rate':1.2,'F0Add':-20.0}]");
      // "child"
      fakeStylesFemale.put("child", "[{'HMMTractScaler':1.3,'F0Add':90.0}]");
      fakeStylesNeutral.put("child", "[{'HMMTractScaler':1.3,'F0Add':95.0}]");
      fakeStylesMale.put("child", "[{'HMMTractScaler':1.3,'F0Add':100.0}]");

      fakeStylesByGender.put(Voice.FEMALE.toString(), fakeStylesFemale );
      fakeStylesByGender.put(Voice.MALE.toString(), fakeStylesMale );
      fakeStylesByGender.put(Voice.NEUTRAL.toString(), fakeStylesNeutral );
      fakeStylesByGender = Collections.unmodifiableMap(fakeStylesByGender);
      }
    }
    protected static HashMap<String,ParamParser> effectsRegistry = null;
    static {
      if(effectsRegistry == null) {
    effectsRegistry = new HashMap<String,ParamParser>();
    effectsRegistry.put("Volume", new DoubleParamParser("Volume", "Volume", "Scale the output volume.", MergeStrategy.MULTIPLY, 1, 0, 10, 2) );

    effectsRegistry.put("TractScaler",
		 new DoubleParamParser("TractScaler", "Vocal Tract Scaling", "Creates a shortened or lengthened vocal tract effect by shifting the formants. For values less than 1.0, the formants are shifted to lower frequencies resulting in a longer vocal tract (i.e. a deeper voice). Values greater than 1.0 shift the formants to higher frequencies. The result is a shorter vocal tract.", MergeStrategy.MULTIPLY, 1, 0.25, 4.0, 1.5)
		 );
    
    effectsRegistry.put("HMMTractScaler",
		 new DoubleParamParser("HMMTractScaler", "Vocal Tract Scaling (HMM)", "Creates a shortened or lengthened vocal tract effect by shifting the formants. For values less than 1.0, the formants are shifted to lower frequencies resulting in a longer vocal tract (i.e. a deeper voice). Values greater than 1.0 shift the formants to higher frequencies. The result is a shorter vocal tract.", MergeStrategy.MULTIPLY, 1, 0.25, 4.0, 1.5)
		 );

    effectsRegistry.put("F0Scale",
		 new DoubleParamParser("F0Scale", "F0 Scaling", "All voiced F0 values are multiplied by this value. This operation effectively scales the range of f0 values. Note that mean F0 is preserved during the operation. Values greater than 1.0 expand the F0 range (i.e. voice with more variable pitch); Value less than 1.0 compress the F0 range (i.e. more monotonic voice).", MergeStrategy.MULTIPLY, 1, 0, 3.0, 2)
		 );

    
    effectsRegistry.put("F0Add",
		 new DoubleParamParser("F0Add", "F0 mean Shifting", "Shifts the mean F0 value by a given Hz amount.", MergeStrategy.ADD, 0, -300, 300, 50)
		 );

    
    effectsRegistry.put("Rate",
		 new DoubleParamParser("Rate", "Duration scaling", "Scale the duration for synthesized speech output. Values greater than 1.0 will increase the output duration. Values less than 1.0 will produce shorter output.", MergeStrategy.MULTIPLY, 1, 0.1, 3.0, 1.5)
		 );

    
    effectsRegistry.put("Whisper",
		 new DoubleParamParser("Whisper", "Whispered Voice", "Creates a whispered voice by replacing the LPC residual with white noise.", MergeStrategy.ADD, 0, 0, 100, 100)
		 );

    
    effectsRegistry.put("Stadium",
		 new DoubleParamParser("Stadium", "Stadium", "Adds stadium effect by applying a specially designed multi-tap chorus.", MergeStrategy.ADD, 0, 0, 200, 100)
		 );

    
    effectsRegistry.put("Robot",
		 new DoubleParamParser("Robot", "Robotic Voice", "Creates a robotic voice by setting all phases to zero.", MergeStrategy.ADD, 0, 0, 100, 100)
		 );
    
    effectsRegistry.put("JetPilot",
		 new BooleanParamParser("JetPilot", "Jet pilot", "Filter audio to simulate \"jet pilot\" sound.", MergeStrategy.ADD, false, true)
		 );

    TParamParser t;
    t = new TParamParser("FIRFilter", "FIR filtering", "Filters the input signal by an FIR filter.", MergeStrategy.OVERWRITE);
    EnumParamParser en = new EnumParamParser("type", "Type of filter", "Type of filter", MergeStrategy.OVERWRITE);
    en.addElement("lowpass", new Integer(1));
    en.addElement("highpass", new Integer(2));
    en.addElement("bandreject", new Integer(4));
    en.addElement("bandpass", new Integer(3));
    t.addParameter(en);
    t.addParameter(new DoubleParamParser("fc", "Cutoff frequency", "Cutoff frequency in Hz for lowpass and highpass filters. It should be in the range [0, fs/2.0] where fs is the sampling rate in Hz.", MergeStrategy.OVERWRITE, 0, 0, 1000000, 500));
    t.addParameter(new DoubleParamParser("fc1", "Lower frequency cutoff", "Lower frequency cutoff in Hz for bandpass and bandreject filters. It should be in the range [0, fs/2.0] where fs is the sampling rate in Hz.", MergeStrategy.OVERWRITE, 0, 0, 1000000, 500));
    t.addParameter(new DoubleParamParser("fc2", "Higher frequency cutoff", "Higher frequency cutoff in Hz for bandpass and bandreject filters. It should be in the range [0, fs/2.0] where fs is the sampling rate in Hz.", MergeStrategy.OVERWRITE, 0, 0, 1000000, 2000));
    effectsRegistry.put("FIRFilter", t);

    t = new TParamParser("Chorus", "Multi-Tap Chorus", "Adds chorus effect by summing up the original signal with delayed and amplitude scaled versions. The parameters should consist of delay and amplitude pairs for each tap. A variable number of taps (max 20) can be specified by simply defining more delay-amplitude pairs. Each tap outputs a delayed and gain-scaled version of the original signal. All tap outputs are summed up with the oiginal signal with appropriate gain normalization.", MergeStrategy.OVERWRITE);
    t.addParameter(new IntegerParamParser("delay1", "Delay for tap #1", "The amount of delay in miliseconds for tap #1.", MergeStrategy.OVERWRITE, 0, 0, 5000, 466));
    t.addParameter(new DoubleParamParser("amp1", "Amplitude for tap #1", "Relative amplitude of the channel gain as compared to original signal gain for tap #1.", MergeStrategy.OVERWRITE, 1, -5, 5, 0.54));
    t.addParameter(new IntegerParamParser("delay2", "Delay for tap #2", "The amount of delay in miliseconds for tap #2.", MergeStrategy.OVERWRITE, 0, 0, 5000, 600));
    t.addParameter(new DoubleParamParser("amp2", "Amplitude for tap #2", "Relative amplitude of the channel gain as compared to original signal gain for tap #2.", MergeStrategy.OVERWRITE, 1, -5, 5, -0.1));
    t.addParameter(new IntegerParamParser("delay3", "Delay for tap #3", "The amount of delay in miliseconds for tap #3.", MergeStrategy.OVERWRITE, 0, 0, 5000, 250));
    t.addParameter(new DoubleParamParser("amp3", "Amplitude for tap #3", "Relative amplitude of the channel gain as compared to original signal gain for tap #3.", MergeStrategy.OVERWRITE, 1, -5, 5, 0.3));
    effectsRegistry.put("Chorus", t);
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
      }
    }

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
	  }
	}
	HashMap<String, Object> effects_values = new HashMap<String, Object>();
	if (utteranceStyleEffects.length()>0) {
	  JSONArray effects = new JSONArray( utteranceStyleEffects );
	  for(int i = 0; i < effects.length(); i++) {
	    JSONObject obj = effects.getJSONObject(i);
	    parseEffectsIntoHashMap(effectsRegistry, effects_values, obj);
	    // System.out.println(toOldStyleEffectsString(registry, effects_values));
	  }
	  // System.out.println(toOldStyleEffectsString(registry, effects_values));
	}
	if (utteranceEffects.length()>0) {
	  JSONArray effects = new JSONArray( utteranceEffects );
	  for(int i = 0; i < effects.length(); i++) {
	    JSONObject obj = effects.getJSONObject(i);
	    parseEffectsIntoHashMap(effectsRegistry, effects_values, obj);
	    // System.out.println(toOldStyleEffectsString(registry, effects_values));
	  }
	  // System.out.println(toOldStyleEffectsString(registry, effects_values));
	}
	utteranceEffects = toOldStyleEffectsString(effectsRegistry, effects_values);
	if (utteranceEffects.length()>0)
	  logger.debug("Audio effects requested: " + utteranceEffects);
	else
	  logger.debug("No audio effects requested");
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


  private static void parseEffectsIntoHashMap(HashMap<String,ParamParser> registry, HashMap<String, Object> effects_values, JSONObject effects) {
    for(String name: JSONObject.getNames(effects)) {
      // System.out.println("----------");
      // System.out.println(name);
      ParamParser parser  =  registry.get(name);
      if(parser != null) {
	Object param = parser.parse(effects.get(name));
	// System.out.println(param);
	if(effects_values.containsKey(name)){
	  Object o = effects_values.get(name);
	  param = parser.merge(o, param);
	}
	// System.out.println(param);
	effects_values.put(name, param);
      }
    }
  }
  private static String toOldStyleEffectsString(HashMap<String,ParamParser> registry, HashMap<String, Object> effects_values) {
    StringBuilder sb = new StringBuilder();
    for(String name: effects_values.keySet()) {
      Object param = effects_values.get(name);
      if(param != null) {
	ParamParser parser  =  registry.get(name);
	if(parser != null) {
	  param = parser.limit(param);
	  String s = parser.toOldStyleString(param);
	  if(s != null) {
	    if(sb.length()>0) {
	      sb.append('+');
	    }
	    sb.append(s);
	  }
	}
      }
    }
    return sb.toString();
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

