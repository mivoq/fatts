package marytts.server.http;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import marytts.features.FeatureProcessorManager;
import marytts.features.FeatureRegistry;
import marytts.modules.synthesis.Voice;
import marytts.util.MaryRuntimeUtils;
import marytts.util.MaryUtils;
import marytts.util.http.Address;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.nio.entity.NStringEntity;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import marytts.util.data.audio.MaryAudioUtils;

import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.Collection;
import java.util.Iterator;
import java.net.URLEncoder;
import java.net.URLDecoder;
import marytts.modules.synthesis.Voice;
import marytts.unitselection.UnitSelectionVoice;
import marytts.unitselection.interpolation.InterpolatingVoice;
import marytts.htsengine.HMMVoice;
import marytts.server.http.params.effects.ParamParser;

public class MivoqServerInfoHandler extends BaseHttpRequestHandler {

	public MivoqServerInfoHandler() {
	  super();
	}

    @Override
	protected void handleClientRequest(String absPath,
			Map<String, String> queryItems, HttpResponse response,
			Address serverAddressAtClient) throws IOException {
        // Individual info request
        String infoResponse = handleInfoRequest(absPath, queryItems, response);
        if (infoResponse == null) { // error condition, handleInfoRequest has set an error message
            return;
        }

        response.setStatusCode(HttpStatus.SC_OK);
	response.setHeader("Access-Control-Allow-Origin", "*");
	// response.setHeader("Access-Control-Allow-Credentials", "true");
	// response.setHeader("Access-Control-Expose-Headers", "Cache-Control,Content-Language,Content-Type,Expires,Last-Modified,Pragma");
        try {
            NStringEntity entity = new NStringEntity(infoResponse, "UTF-8");
            entity.setContentType("application/json; charset=UTF-8");
            response.setEntity(entity);
        } catch (UnsupportedEncodingException e){}
    }



  static private HashMap<String,Object> getVoiceInfo(Voice v){
    if(v == null)
      return null;
    HashMap<String,Object> obj = null;
    if (v instanceof InterpolatingVoice) {
      // do not list interpolating voice
    } else {
      obj = new HashMap<String,Object>();
      ArrayList<String> loc = new ArrayList<String>();
      obj.put("id", v.getName());
      obj.put("gender", v.gender().toString());
      loc.add(v.getLocale().toString());
      if(!v.getLocale().getCountry().isEmpty()){
	if(!v.getLocale().getVariant().isEmpty()){
	  loc.add(v.getLocale().getLanguage() + "_" + v.getLocale().getCountry());
	}
	loc.add(v.getLocale().getLanguage());
      }
      obj.put("locales", loc);
      if (v instanceof UnitSelectionVoice) {
	obj.put("technology", "unitselection");
	obj.put("domain", ((UnitSelectionVoice)v).getDomain());
      }
      else if (v instanceof HMMVoice) {
	obj.put("technology", "hmm");
      }
    }
    return obj;
  }
  static private boolean doesVoiceSupportLocale(Voice v, Locale l){
    if(v == null)
      return false;
    Locale lv = v.getLocale();
    if(!l.getVariant().isEmpty() && !l.getVariant().equals(lv.getVariant())) {
      return false;
    }
    if(!l.getCountry().isEmpty() && !l.getCountry().equals(lv.getCountry())) {
      return false;
    }
    if(!l.getLanguage().isEmpty() && !l.getLanguage().equals(lv.getLanguage())) {
      return false;
    }
    return true;
  }

    private String handleInfoRequest(String absPath, Map<String, String> queryItems, HttpResponse response)
    {
        logger.debug("New info request: "+absPath);
        if (queryItems != null) {
            for (String key : queryItems.keySet()) {
                logger.debug("    "+key+"="+queryItems.get(key));
            }
        }

        assert absPath.startsWith("/info/") : "Absolute path '"+absPath+"' does not start with a slash!";
        String request = absPath.substring(6); // without the initial /info/

        if (request.equals("version")) {
	    JSONObject version = new JSONObject();
	    version.put("vendor", "Mivoq SRL");
	    version.put("product", "FA-TTS (MaryTTS server)");
	    version.put("fa_tts_api_version", "0.0.2");
	    version.put("specification", marytts.Version.specificationVersion());
	    JSONObject implementation = new JSONObject();
	    implementation.put("revision", marytts.Version.implementationVersion());
	    implementation.put("revision-time", marytts.Version.implementationVersionTime());
	    implementation.put("build-time", marytts.Version.implementationBuildTime());
	    version.put("implementation", implementation);
            return version.toString();
        }
        else if (request.startsWith("voices/")){
	  if (request.equals("voices/all")){
	    Collection<Voice> voices = Voice.getAvailableVoices();
	    JSONObject voicesreturnobj = new JSONObject();
	    ArrayList<HashMap<String,Object>> voicesarray = new ArrayList<HashMap<String,Object>>();
	    for (Iterator<Voice> it = voices.iterator(); it.hasNext();) {
	      HashMap<String,Object> obj = this.getVoiceInfo((Voice) it.next());
	      if(obj != null) {
		voicesarray.add(obj);
	      }
	    }
	    voicesreturnobj.put("voices", voicesarray);
	    return voicesreturnobj.toString();
	  }
	  else if (request.startsWith("voices/locale/")){
	    String nrequest = request.substring(14); // without the initial voices/locale/
	    Collection<Voice> voices = Voice.getAvailableVoices();
	    JSONObject voicesreturnobj = new JSONObject();
	    ArrayList<HashMap<String,Object>> voicesarray = new ArrayList<HashMap<String,Object>>();
	    for (Iterator<Voice> it = voices.iterator(); it.hasNext();) {
	      Voice v = (Voice) it.next();
	      if(this.doesVoiceSupportLocale(v, MaryUtils.string2locale(nrequest))) {
		HashMap<String,Object> obj = this.getVoiceInfo(v);
		if(obj != null) {
		  voicesarray.add(obj);
		}
	      }
	    }
	    voicesreturnobj.put("voices", voicesarray);
	    return voicesreturnobj.toString();
	  }
	}
        else if (request.startsWith("locales/")){
	  if (request.equals("locales/all")){
	    HashSet loc = new HashSet();
	    Collection<Voice> voices = Voice.getAvailableVoices();
	    JSONObject voicesreturnobj = new JSONObject();
	    for (Iterator<Voice> it = voices.iterator(); it.hasNext();) {
	      Voice v = (Voice) it.next();
	      Locale l = v.getLocale();
	      loc.add(l.toString());
	      if(!l.getCountry().isEmpty()){
		if(!l.getVariant().isEmpty()){
		  loc.add(l.getLanguage() + "_" + l.getCountry());
		}
		loc.add(l.getLanguage());
	      }
	    }
	    voicesreturnobj.put("locales", new ArrayList<String>(loc));
	    return voicesreturnobj.toString();
	  }
	}
        else if (request.startsWith("voice/")) {
	  String[] parts = request.split("/");
	  String voice_id = null;
	  if(parts.length >= 2 ){
	    try{
	      voice_id = URLDecoder.decode(parts[1], "UTF-8");
	    } catch(Exception e) {
	      voice_id = null;
	    }
	  }
	  if(voice_id != null) {
	    if(parts.length == 4 ){
	      Voice v = Voice.getVoice(voice_id);
	      if(v != null) {
		if(parts[2].equals("outputs")) {
		  if(parts[3].equals("all")) {
		    JSONObject res = new JSONObject();
		    ArrayList<HashMap<String,Object>> formats = new ArrayList<HashMap<String,Object>>();
		    AudioFileFormat.Type[] audioTypes = AudioSystem.getAudioFileTypes();
		    for (int i=0; i<audioTypes.length; i++) {
		      AudioFileFormat.Type audioType = audioTypes[i];
		      String typeName = audioType.toString();
		      boolean isSupported = true;
		      if (typeName.equals("MP3")) isSupported = MaryRuntimeUtils.canCreateMP3();
		      else if (typeName.equals("Vorbis")) isSupported = MaryRuntimeUtils.canCreateOgg();
		      audioType = MaryAudioUtils.getAudioFileFormatType(typeName);
		      if (audioType == null) {
			isSupported = false;
		      }
		      if (isSupported && AudioSystem.isFileTypeSupported(audioType)) {
			HashMap<String,Object> t = new HashMap<String,Object>();
			t.put("id", typeName + "_FILE");
			formats.add(t);
			if (typeName.equals("MP3") || typeName.equals("Vorbis") || typeName.equals("AU")){
			  t = new HashMap<String,Object>();
			  t.put("id", typeName + "_STREAM");
			  formats.add(t);
			}
		      }
		    }
		    JSONObject res1 = new JSONObject();
		    res1.put("formats", formats);
		    res1.put("id", "AUDIO");
		    ArrayList<JSONObject> outputs = new ArrayList<JSONObject>();
		    outputs.add(res1);
		    res.put("outputs", outputs);
		    return res.toString();
		  }
		}
		else if(parts[2].equals("inputs")){
		  if(parts[3].equals("all")) {
		    JSONObject res = new JSONObject();
		    JSONObject res1 = new JSONObject();
		    res1.put("id", "TEXT");
		    ArrayList<JSONObject> inputs = new ArrayList<JSONObject>();
		    inputs.add(res1);
		    res.put("inputs", inputs);
		    return res.toString();
		  }
		}
		else if(parts[2].equals("styles")){
		  if(parts[3].equals("all")) {
		    JSONObject res = new JSONObject();
		    ArrayList<JSONObject> styles = new ArrayList<JSONObject>();
		    if (v instanceof HMMVoice) {
		      HashMap<String,String> m = MivoqSynthesisRequestHandler.fakeStylesByGender.get(v.gender().toString());
		      if(m != null) {
			for(String style: m.keySet()) {
			  JSONObject res1 = new JSONObject();
			  res1.put("id", style);
			  styles.add(res1);
			}
		      }
		    }
		    res.put("styles", styles);
		    return res.toString();
		  }
		}
		else if(parts[2].equals("effects")){
		  if(parts[3].equals("all")) {
		    JSONObject res = new JSONObject();
		    JSONObject res1 = new JSONObject();
		    JSONArray effects = new JSONArray();
		    for (ParamParser value : MivoqSynthesisRequestHandler.effectsRegistry.values()) {
		      effects.put(value.toJSONObject());
		    }
		    res.put("effects", effects);
		    return res.toString();
		  }
		}
	      }
	    }
	  }
	}
	MaryHttpServerUtils.errorFileNotFound(response, request);
	return null;
    }
}
