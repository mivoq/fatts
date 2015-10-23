package marytts.language.en.phonemiser;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.w3c.dom.Element;
import marytts.datatypes.MaryXML;

import marytts.datatypes.MaryDataType;
import marytts.exceptions.MaryConfigurationException;
import marytts.modules.phonemiser.Allophone;
import marytts.util.dom.MaryDomUtils;

import java.util.HashSet;

public class JPhonemiser extends marytts.modules.JPhonemiser {

	public JPhonemiser(String propertyPrefix) throws IOException,
			MaryConfigurationException, SecurityException,
			IllegalArgumentException, NoSuchMethodException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException, InvocationTargetException {
		super(propertyPrefix);
	}

	public JPhonemiser(String componentName, MaryDataType inputType,
			MaryDataType outputType, String allophonesProperty,
			String userdictProperty, String lexiconProperty,
			String ltsProperty, String removetrailingonefromphonesProperty,
			String syllabifierClassProperty) throws IOException,
			MaryConfigurationException, SecurityException,
			NoSuchMethodException, ClassNotFoundException,
			IllegalArgumentException, InstantiationException,
			IllegalAccessException, InvocationTargetException {
		super(componentName, inputType, outputType, allophonesProperty,
				userdictProperty, lexiconProperty, ltsProperty,
				removetrailingonefromphonesProperty, syllabifierClassProperty);
	}


	public JPhonemiser(String componentName, MaryDataType inputType,
			MaryDataType outputType, String allophonesProperty,
			String userdictProperty, String lexiconProperty, String ltsProperty)
			throws IOException, MaryConfigurationException, SecurityException,
			IllegalArgumentException, NoSuchMethodException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException, InvocationTargetException {
		super(componentName, inputType, outputType, allophonesProperty,
				userdictProperty, lexiconProperty, ltsProperty);
	}


  private String getPosForEndingS (Element t, HashSet hSet ) {
    if (t.getPreviousSibling()!=null) {
      // if hSet is null, create it
      if ( hSet == null ) {
	hSet = new HashSet <Element>();
      }
      // put t in hSet
      hSet.add(t);
      // get previous sibling
      Element previousElement = (Element) t.getPreviousSibling();

      if (hSet.contains(previousElement)) {
	// force phonetization
	return "CTX-s";
      }
      
      // if t.getPreviousSibling() is a TOKEN, ok continue
      // if t.getPreviousSibling() is a MTU, get its first child - let's assume it's necessarily a token
      // TODO: check if there can be nested MTUs
      if (previousElement.getTagName().equals(MaryXML.MTU)) {
	previousElement = (Element) previousElement.getFirstChild();
      }
      
      String previousToken = MaryDomUtils.tokenText( previousElement );
      StringBuilder helper = new StringBuilder();
      String previousTokenPhon = this.phonemise( previousToken, getPosTag( previousElement, hSet ), helper );
      String[] parts = previousTokenPhon.split(" ");
      Allophone allo = null;
      for (String part : parts) {
	// the following is to skip the possible "'" symbol (N.B.: allophoneSet.getAllophone("'") would crash)
	if ( allophoneSet.getAllophoneNames().contains( part ) ) {
	  allo = allophoneSet.getAllophone(part);
	}
      }
      if ( allo != null ) {
	// [iz] Travis's , Buzz's , princess's, coach's
	// (when the singular words end up in 's','z'..
	//   <consonant ph="s" ctype="f" cplace="a" - isFricative() && allo.getFeature("cplace").equals("a")
	//   <consonant ph="z" ctype="f" cplace="a" - isFricative() && allo.getFeature("cplace").equals("a")
	// ..or fricatives such as 'S', [NdGS: I add also 'Z'] ..
	//   <consonant ph="S" ctype="f" cplace="p" - isFricative() && allo.getFeature("cplace").equals("p")
	//   <consonant ph="Z" ctype="f" cplace="p" - isFricative() && allo.getFeature("cplace").equals("p")
	// ..'tS', 'dZ')
	//   <consonant ph="tS" ctype="a" - isAffricate()
	//   <consonant ph="dZ" ctype="a" - isAffricate()
	if ( ( allo.isFricative() && allo.getFeature("cplace").equals("a") ) // "s" or "z"
		  || ( allo.isFricative() && allo.getFeature("cplace").equals("p") ) // "S" or "Z"
		  || allo.isAffricate() // "tS" or "dZ"
		  ) {
	  return "CTX-Iz";
	}
	// [z] Laura's Greg's Tom's (voiced consonant or vowel before s)
	else if ( allo.isVoiced() ) {
	  return "CTX-z";
	}
	// s is read voiceless, [s], when it comes after a voiceless consonant
	// [s] Nick's Pope's Stuart's (voiceless consonant before s)
	else {
	  return "CTX-s";
	}
      }
    }
    return "CTX-Iz";
  }

  
	protected String getPosTag(Element t) {
	  return getPosTag( t, null);
	}
  
	protected String getPosTag(Element t, HashSet hSet) {
		// hSet is used to prevent phonetization search loops. It keeps track of those tokens that need
		// another (typically previous or next) token in order to disambiguate their phonetization.
		// When a word that is already in the set is encoutered again, the search is forced to stop.
		// An example of "phonetization search loop" is the multitoken "the's": the word "the" needs the
		// next word, "'s", to disambiguate its phonetization, while "'s" needs the previous one.
		String pos = null;
		if (t != null) {
		        String text = MaryDomUtils.tokenText(t).toLowerCase();
		        if(text.equals("'s")) {
			  return getPosForEndingS(t, hSet);
			}
			// use part-of-speech if available
			if (t.hasAttribute("pos")) {
				pos = t.getAttribute("pos");
				
				// simplify POS tagging in order to match POS tags in the lexicon
				// disambiguate pronunciation of words according to previous or next word
				if ((pos != null) && pos.length() != 0) {
					switch (pos.charAt(0)) {
					case 'V':
					    pos = "VB";
					  break;
					case 'N':
						pos = "NN";
						break;
					case 'J':
						pos = "JJ";
						break;
					case 'R':
						pos = "R";
						break;
					case 'D':
					  if (text.equals("the")) {
					    if (t.getNextSibling()!=null) {

					      // if hSet is null, create it
					      if ( hSet == null ) {
						hSet = new HashSet <Element>();
					      }
					      // put t in hSet
					      hSet.add(t);
					      // get next sibling
					      Element nextElement = (Element) t.getNextSibling();
					      if (hSet.contains(nextElement)) {
						// force phonetization
						pos = "DT-D@";
						break;
					      }
						
					      
					      // if t.getNextSibling() is a TOKEN, ok continue
					      // if t.getNextSibling() is a MTU, get its first child - let's assume it's necessarily a token
					      // TODO: check if there can be nested MTUs
					      if (nextElement.getTagName().equals(MaryXML.MTU)) {
					    	  nextElement = (Element) nextElement.getFirstChild();
					      }
					      
					      String nextToken = MaryDomUtils.tokenText( nextElement );
					      // TODO: nextToken can be "-", for which no phonetization will be given.
					      //       In sentences like "the-game" it might be good to look up the token after "-"
					      
					      // examples
					      // hour | ' au @ -> vowel /thee our/
					      // house | ' h au z -> consonant sound /thuh house/
					      // the university | ' j }u - n @ - ' v @@ - s @ - t Ii -> consonant sound /thuh youniversity/
					      // the umbrella | V m - ' b r E - l @ -> vowel /thee umbrella/
					      StringBuilder helper = new StringBuilder();
					      String nextTokenPhon = this.phonemise( nextToken, getPosTag( nextElement, hSet ), helper );
					      String[] parts = nextTokenPhon.split(" ");
					      for (String part : parts) {
						// the following is to skip the possible "'" symbol (N.B.: allophoneSet.getAllophone("'") would crash)
						if ( allophoneSet.getAllophoneNames().contains( part ) ) {
						  Allophone allo = allophoneSet.getAllophone(part);
						  
						  if ( ! allo.isVowel() ) {
						    pos = "DT-D@";
						  }
						  else {
						    pos = "DT-DIi";
						  }
						  break;
						}
					      }
					    }
					  }
					  break;
					  
					default:
						break;
					}
				}
			}
		}
		return pos;
	}

    /**
     * Look a given text up in the (standard) lexicon. part-of-speech is used 
     * in case of ambiguity.
     * 
     * @param text
     * @param pos
     * @return
     */
    public String lexiconLookup(String text, String pos)
    {
        if (text == null || text.length() == 0) return null;
        String[] entries;
        entries = lexiconLookupPrimitive(text, pos);
        // If entry is not found directly, try the following changes:
        // - lowercase the word
        // - all lowercase but first uppercase
        if (entries.length  == 0) {
            text = text.toLowerCase(getLocale());
            entries = lexiconLookupPrimitive(text, pos);
            if (entries.length  == 0) {
                // - lowercase the word and drop points and '
                entries = lexiconLookupPrimitive(text.replaceAll("[.']", ""), pos);
            }
        }
        if (entries.length  == 0) {
            text = text.substring(0,1).toUpperCase(getLocale()) + text.substring(1);
            entries = lexiconLookupPrimitive(text, pos);
         }
         
         if (entries.length  == 0) return null;
         return entries[0];
    }
	
}
