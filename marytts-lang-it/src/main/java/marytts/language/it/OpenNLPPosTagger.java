package marytts.language.it;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import opennlp.tools.postag.POSDictionary;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.TagDictionary;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.traversal.NodeIterator;
import org.w3c.dom.traversal.TreeWalker;

import marytts.datatypes.MaryData;
import marytts.datatypes.MaryXML;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.NoSuchPropertyException;
import marytts.server.MaryProperties;
import marytts.util.dom.MaryDomUtils;

/*
 * Extension of OpenNLPPosTagger that creates two attributes:
 * pos_full: POS which includes morphological information gender and number
 * pos: standard grammatical class POS 
 */
public class OpenNLPPosTagger extends marytts.modules.OpenNLPPosTagger {

	
	private Pattern uppercase_part;
	
	public OpenNLPPosTagger(String locale, String propertyPrefix)
			throws Exception {
		super(locale, propertyPrefix);
		// TODO Auto-generated constructor stub
		// Regular expression to split the uppercase part from the other one
		uppercase_part = Pattern.compile("([A-Z]+)([^A-Z]+)");
	}
	
  
	protected void assignPOSTagger() throws NoSuchPropertyException, MaryConfigurationException, IOException
	{
		InputStream modelStream = MaryProperties.needStream(propertyPrefix+ "model");
		ItalianPOSTaggerFactory factory = new ItalianPOSTaggerFactory();
		if (MaryProperties.getProperty(propertyPrefix + "deterministic_symbols_tagdict") != null) {
			InputStream deterministic_symbols_tagdict_Stream = MaryProperties.needStream(propertyPrefix + "deterministic_symbols_tagdict");
			// case sensitiveness? POSDictionary.create does not take into
			// account... or take this automatically into account?
			TagDictionary deterministic_symbols_tagdict = POSDictionary.create(deterministic_symbols_tagdict_Stream);
			POSModel model = new POSModel(modelStream);
			factory.setTagDictionary(model.getTagDictionary());
			tagger = new ItalianPOSTaggerME(model, factory.getSequenceValidator(deterministic_symbols_tagdict));
		} else {
			//System.out.println("ItalianPOSTaggerME without deterministic_symbols_tagdict");
			tagger = new ItalianPOSTaggerME(new POSModel(modelStream));
		}
		modelStream.close();
	}
	
	@SuppressWarnings("unchecked")
	public MaryData process(MaryData d) throws Exception {

		Document doc = d.getDocument();
		NodeIterator sentenceIt = MaryDomUtils.createNodeIterator(doc, doc,
				MaryXML.SENTENCE);
		Element sentence;
		while ((sentence = (Element) sentenceIt.nextNode()) != null) {
			TreeWalker tokenIt = MaryDomUtils.createTreeWalker(sentence,
					MaryXML.TOKEN);
			List<String> tokens = new ArrayList<String>();
			Element t;
			while ((t = (Element) tokenIt.nextNode()) != null) {
				tokens.add(MaryDomUtils.tokenText(t));
			}
			List<String> partsOfSpeech = null;
			synchronized (this) {
				partsOfSpeech = tagger.tag(tokens);
			}
			tokenIt.setCurrentNode(sentence); // reset treewalker so we can walk
												// through once again
			Iterator<String> posIt = partsOfSpeech.iterator();
			while ((t = (Element) tokenIt.nextNode()) != null) {
				assert posIt.hasNext();
				String pos = posIt.next();
				if (posMapper != null) {
					String gpos = posMapper.get(pos);
					if (gpos == null)
						logger.warn("POS map file incomplete: do not know how to map '"
								+ pos + "'");
					else
						pos = gpos;
				}
				t.setAttribute("pos_full", pos);

				Matcher uppercase_matcher = uppercase_part.matcher(pos);
				if (uppercase_matcher.matches())
					t.setAttribute("pos", uppercase_matcher.group(1));
				else
					t.setAttribute("pos",pos); // is some case (E) the full_pos and pos are equal 
			}
		}

		MaryData output = new MaryData(outputType(), d.getLocale());
		output.setDocument(doc);
		return output;
	}

}
