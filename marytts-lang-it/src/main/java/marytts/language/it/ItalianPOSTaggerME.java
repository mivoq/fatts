package marytts.language.it;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerFactory;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.util.BeamSearch;
import opennlp.tools.util.SequenceValidator;

public class ItalianPOSTaggerME extends POSTaggerME {

	public ItalianPOSTaggerME(POSModel model, SequenceValidator<String> sequenceValidator) {
		super(model,DEFAULT_BEAM_SIZE, 0, sequenceValidator);
		// TODO Auto-generated constructor stub
	}
	
	public ItalianPOSTaggerME(POSModel model) {
		super(model,DEFAULT_BEAM_SIZE, 0);
		// TODO Auto-generated constructor stub
	}
	
}
