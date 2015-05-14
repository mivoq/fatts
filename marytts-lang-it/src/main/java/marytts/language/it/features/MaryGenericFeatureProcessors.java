/**
 * Portions Copyright 2006-2007 DFKI GmbH.
 * Portions Copyright 2001 Sun Microsystems, Inc.
 * Portions Copyright 1999-2001 Language Technologies Institute, 
 * Carnegie Mellon University.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * Permission is hereby granted, free of charge, to use and distribute
 * this software and its documentation without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of this work, and to
 * permit persons to whom this work is furnished to do so, subject to
 * the following conditions:
 * 
 * 1. The code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 * 2. Any modifications must be clearly marked as such.
 * 3. Original authors' names are not deleted.
 * 4. The authors' names are not used to endorse or promote products
 *    derived from this software without specific prior written
 *    permission.
 *
 * DFKI GMBH AND THE CONTRIBUTORS TO THIS WORK DISCLAIM ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL DFKI GMBH NOR THE
 * CONTRIBUTORS BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
 * ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */

package marytts.language.it.features;


import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import marytts.datatypes.MaryXML;
import marytts.features.ByteValuedFeatureProcessor;

import marytts.unitselection.select.Target;
import marytts.util.dom.MaryDomUtils;
import marytts.util.string.ByteStringTranslator;

import opennlp.tools.util.featuregen.AdditionalContextFeatureGenerator;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.TreeWalker;

/**
 * A collection of feature for Italian processors that operate on Target objects. 

 * 
 * @author Fabio Tesser
 * 
 */
public class MaryGenericFeatureProcessors
extends marytts.features.MaryGenericFeatureProcessors {
	
	/*
	 * This is used for SentenceType feature
	 */
    public static class SentenceNavigator implements TargetElementNavigator
    {
        public Element getElement(Target target)
        {
            Element segment = target.getMaryxmlElement();
            if (segment == null) return null;
            Element sentence = (Element) MaryDomUtils.getAncestor(segment, MaryXML.SENTENCE);
            return sentence;
        }
    }
	

	/**
	 * Determine the prosodic property of a target for Italian
	 * 
	 * @author Fabio Tesser
	 * 
	 */
	public static class Selection_Prosody implements ByteValuedFeatureProcessor {

		protected TargetElementNavigator navigator;
		protected ByteStringTranslator values = new ByteStringTranslator(
				new String[] { "0", "stressed", "pre-nuclear", "nuclear", 
						
						"nuclear-interrog", "nuclear-interrogW",
						"pre-nuclear-interrog", "pre-nuclear-interrogW",
						"first-tonic", "first-tonic-interrog", "first-tonic-interrogW",
						"initial", "initial-interrog", "initial-interrogW", 
						
						"finalHigh", "finalLow", "final" });
		private Set<String> lowEndtones = new HashSet<String>(
				Arrays.asList(new String[] { "L-", "L-%", "L-L%" }));
		private Set<String> highEndtones = new HashSet<String>(
				Arrays.asList(new String[] { "H-", "!H-", "H-%", "H-L%",
						"!H-%", "H-^H%", "!H-^H%", "L-H%", "H-H%" }));

		public Selection_Prosody(TargetElementNavigator syllableNavigator) {
			this.navigator = syllableNavigator;
		}

		public String getName() {
			return "selection_prosody";
		}

		public String[] getValues() {
			return values.getStringValues();
		}

		/**
		 * Determine the prosodic property of the target
		 * 
		 * @param target
		 *            the target
		 * @return 0 - unstressed, 1 - stressed, 2 - pre-nuclear accent 3 -
		 *         nuclear accent, 4 - phrase final high, 5 - phrase final low,
		 *         6 - phrase final (with unknown high/low status).
		 */
		public byte process(Target target) {
			// first find out if syllable is stressed
			Element syllable = navigator.getElement(target);
			if (syllable == null)
				return (byte) 0;
			boolean stressed = false;
			if (syllable.getAttribute("stress").equals("1")) {
				stressed = true;
			}
			// find out if we have an accent
			boolean accented = syllable.hasAttribute("accent");
			boolean nuclear = true; // relevant only if accented == true
			boolean first_tonic = true; // relevant only if accented == true
			// find out the position of the target
			boolean phraseFinal = false;
			boolean phraseInitial = false;
			String endtone = null;
			Element sentence = (Element) MaryDomUtils.getAncestor(syllable,
					MaryXML.SENTENCE);
			
			if (sentence == null)
				return 0;
			
			// compute sentece_type
			String sentence_type="decl";
			NodeList tokens = sentence.getElementsByTagName(MaryXML.TOKEN);
			sentence_type = getSentenceType(tokens);

			TreeWalker tw = MaryDomUtils.createTreeWalker(sentence,MaryXML.SYLLABLE, MaryXML.BOUNDARY);
			tw.setCurrentNode(syllable);
			Element e = (Element) tw.nextNode();

			if (e != null) {
				if (e.getTagName().equals(MaryXML.BOUNDARY)) {
					phraseFinal = true;
					endtone = e.getAttribute("tone");
				} 
				else {
					// not phraseFinal
					TreeWalker tw1 = MaryDomUtils.createTreeWalker(sentence,MaryXML.SYLLABLE, MaryXML.PHRASE);
					tw1.setCurrentNode(syllable);
					Element p = (Element) tw1.previousNode();
					if (p != null) {
						//System.out.println("p != null: " + p.getTagName());
						
						if (p.getTagName().equals(MaryXML.PHRASE))
						{
							phraseInitial = true;
							//System.out.println("phraseInitial = true;");
						} 
						// ----------------
						
						
						//p = (Element) tw1.nextNode();
						//System.out.println("p != null: " + p.getTagName());
						// ----------------
						if (accented) { // look backward for any accent
							while (p != null) {
								if (p.getTagName().equals(MaryXML.SYLLABLE)
										&& p.hasAttribute("accent")) {
									first_tonic = false;
									break;
								}
								p = (Element) tw1.previousNode();
							}
						 }
						// ----------------
					}
				} // enf not phraseFinal

				if (accented) { // look forward for any accent
					while (e != null) {
						if (e.getTagName().equals(MaryXML.SYLLABLE)
								&& e.hasAttribute("accent")) {
							nuclear = false;
							break;
						}
						e = (Element) tw.nextNode();
					}
				}
			}
			// Now, we know:
			// stressed or not
			// accented or not
			// if accented, nuclear or not
			// if accented, initialnuclear or not
			// if final, the endtone
				

			/*System.out.print("stressed:" + stressed);
			System.out.print(" accented:" + accented);
			System.out.print(" nuclear:" + nuclear);
			System.out.print(" phraseInitial:" + phraseInitial);
			System.out.print(" first_tonic:" + first_tonic);
			System.out.print(" phraseFinal:" + phraseFinal);
			System.out.print(" endtone:" + endtone);
			System.out.println();*/
				
				
			if (accented) {
				if (nuclear) {
					// nuclear
					if (sentence_type.equals("interrog"))
						return values.get("nuclear-interrog");
					else if (sentence_type.equals("interrogW")) 
						return values.get("nuclear-interrogW");
					else
					    return values.get("nuclear");
				} else if (first_tonic)
				{
					if (sentence_type.equals("interrog"))
						return values.get("first-tonic-interrog");
					else if (sentence_type.equals("interrogW"))
						return values.get("first-tonic-interrogW");
					else 		
						return values.get("first-tonic");
				}
				else {
					if (sentence_type.equals("interrog"))
						return values.get("pre-nuclear-interrog");
					else if (sentence_type.equals("interrogW"))
						return values.get("pre-nuclear-interrogW");
					else 				
						return values.get("pre-nuclear");
				}
			} 
			else if (phraseInitial) {
				if (sentence_type.equals("interrog"))
					return values.get("initial-interrog");
				else if (sentence_type.equals("interrogW"))
					return values.get("initial-interrogW");
				else
					return values.get("initial");
			}
			
			else if (phraseFinal) {
				if (endtone != null && highEndtones.contains(endtone)) {
					return values.get("finalHigh");
				} else if (endtone != null && lowEndtones.contains(endtone)) {
					return values.get("finalLow");
				} else {
					return values.get("final");
				}
			}
			else if (stressed) {
				return values.get("stressed");
			}
			return (byte) 0;// return unstressed
		}
	}

	
	public static class SentenceType implements ByteValuedFeatureProcessor {
		
		protected TargetElementNavigator navigator;
		protected ByteStringTranslator values = new ByteStringTranslator(
										 new String[] { "0", "decl", "excl", "interrog", "interrogW" });
		
		
		public SentenceType(TargetElementNavigator sentenceNavigator) {
			this.navigator = sentenceNavigator;
		}
		
		public String getName() {
			return "sentence_type";
		}

		public String[] getValues() {
			return values.getStringValues();
		}

		@Override
		public byte process(Target target) {
			Element sentence = navigator.getElement(target);
			
			if (sentence == null) return (byte)0;
			NodeList tokens = sentence.getElementsByTagName(MaryXML.TOKEN);
	        if (tokens.getLength() < 1) {
	            return (byte)0; // no tokens -- what can we do? return (byte)0;
	        }

	        String ret = getSentenceType(tokens);
	        return values.get(ret);
		}
		
	}

	
	
	
	// ----------------
	protected static String getSentenceType(NodeList tokens) {
    	String sentenceType="decl";
    	
    	for (int i = tokens.getLength() - 1; i >= 0; i--) { // search for sentence finishing punctuation mark
            Element t = (Element) tokens.item(i);
            String punct = MaryDomUtils.tokenText(t);
            if(punct.equals(".")) {
            	sentenceType = "decl";
            	break;
            }
            else if(punct.equals("!")) {
            	sentenceType = "excl";
            	break;
            }
            else if(punct.equals("?")) {
            	sentenceType = "interrog";
            	break;
            }
        }
        
    	if(sentenceType.equals("interrog")) {
    		for (int i=0; i<tokens.getLength()-1; i++) { // search for the first word in sentence
    			Element t = (Element) tokens.item(i);
    			if(!t.getAttribute("ph").equals("")) {
    				Element firstToken = (Element)tokens.item(i);
            	    // To check for italian
    				// setInterrogYN contains possible part of speechs of first word in yes-no question
    				//Set<String> setInterrogYN = (Set<String>) listMap.get("firstPosInQuestionYN");
    				// setInterrogW contains possible part of speechs of first word in wh-question
    				Set<String> setInterrogW = (Set<String>) new HashSet<String>(Arrays.asList("PQ" ,"PR", "DQ" ,"B" ,"E", "CS"));
    				
    				
    						
    				String posFirstWord = firstToken.getAttribute("pos");
    				//if(setInterrogYN != null && setInterrogYN.contains(posFirstWord)) {
    				//	sentenceType = "interrogYN"; // Global interrogative
    				//}
    				if(setInterrogW != null && setInterrogW.contains(posFirstWord)) {
    					sentenceType = "interrogW"; // Partial interrogative
    				}
    				break;
    			}
    		}
    	}
        return sentenceType;
    }

}
