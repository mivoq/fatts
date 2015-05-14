package marytts.language.it;

import opennlp.tools.postag.TagDictionary;
import opennlp.tools.util.SequenceValidator;

public class ItalianPOSTaggerFactory extends opennlp.tools.postag.POSTaggerFactory {
	
	  public SequenceValidator<String> getSequenceValidator(TagDictionary deterministic_symbols_tagdict) {
		    return new ItalianPOSSequenceValidator(getTagDictionary(), deterministic_symbols_tagdict);
		  }
}
