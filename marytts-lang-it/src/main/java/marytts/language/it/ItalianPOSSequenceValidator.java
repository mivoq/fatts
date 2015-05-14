package marytts.language.it;

import java.util.Arrays;
import org.apache.commons.lang.ArrayUtils;
import opennlp.tools.postag.TagDictionary;
import opennlp.tools.util.SequenceValidator;

/**
 * Implement a POSSequenceValidator to support deterministic symbols dictionary.
 * It permits to add constraints on tokens allowed to have deterministic labels.
 * It is useful to force punctuation symbols to have a specific label.
 * The following line is an example of a row in the deterministic dictionary file:
 * $PUNCT , . ! ? ;
 * 
 * In this way only the tokens , . ! ? ; are allowed to have the POS $PUNCT, and no others 
 * word are allowed to have the $PUNCT POS.
 * 
 * @author Fabio Tesser
 */
public class ItalianPOSSequenceValidator implements SequenceValidator<String> {
	// Classic tagDictionary
	protected TagDictionary tagDictionary;

	/**
	 * Inverse Tag dictionary used for deterministic token like punctuation
	 * symbols The dictionary file format is inverted to respect the
	 * tagdictionary the first column is for the POS label, the following are a
	 * list of ONLY tokens admitted to have the previous POS label
	 */
	protected TagDictionary deterministicSymbolsTagDictionary;
	
	protected String[] all_determiministic_elements_list;
	protected String[] determinist_pos_tag_list = {"FB","FC","FF","FS"};
	

	public ItalianPOSSequenceValidator(TagDictionary tagDictionary, TagDictionary deterministic_symbols_tagdict) {
		this.tagDictionary = tagDictionary;
		this.deterministicSymbolsTagDictionary = deterministic_symbols_tagdict;
		
		//String [] determinist_pos_tag_list = {"FB","FC","FF","FS"};
				
		all_determiministic_elements_list = null;
		for (int i =0; i<determinist_pos_tag_list.length; i++){
		    all_determiministic_elements_list = (String[]) ArrayUtils.addAll(all_determiministic_elements_list, this.deterministicSymbolsTagDictionary.getTags(determinist_pos_tag_list[i]));
		}

	    /*all_determiministic_elements_list = (String[]) ArrayUtils.addAll(all_determiministic_elements_list, this.deterministicSymbolsTagDictionary.getTags("FB"));
	    all_determiministic_elements_list = (String[]) ArrayUtils.addAll(all_determiministic_elements_list, this.deterministicSymbolsTagDictionary.getTags("FC"));
	    all_determiministic_elements_list = (String[]) ArrayUtils.addAll(all_determiministic_elements_list, this.deterministicSymbolsTagDictionary.getTags("FF"));
	    all_determiministic_elements_list = (String[]) ArrayUtils.addAll(all_determiministic_elements_list, this.deterministicSymbolsTagDictionary.getTags("FS"));
		*/
	}

	// DefaultPOSSequenceValidator 
	 /* public boolean validSequence(int i, String[] inputSequence, String[] outcomesSequence, String outcome) {
		if (tagDictionary == null) {
			System.out.println("tagDictionary = null");
			return true;
		} else {
			String[] tags = tagDictionary.getTags(inputSequence[i].toString());
			if (tags == null) {
				return true;
			} else {
				return Arrays.asList(tags).contains(outcome);
			}
		}
	}
	*/
	
	/*
	 * (non-Javadoc)
	 * deterministicSymbolsTagDictionary SequenceValidator for punctuation
	 * @see opennlp.tools.util.SequenceValidator#validSequence(int, T[], java.lang.String[], java.lang.String)
	 */
	public boolean validSequence(int i, String[] inputSequence, String[] outcomesSequence, String outcome) {
		String[] tags= null;
		boolean tmp = false;
		
		//System.out.println("BEGIN");
		//System.out.println("inputSequence[i].toString():" + inputSequence[i].toString() + " ### " + " outcome: "+ outcome);
		//System.out.println("outcomesSequence.length: " + outcomesSequence.length);
		
		if (deterministicSymbolsTagDictionary != null) {
			//System.out.println("deterministicSymbolsTagDictionary != null");
			// check if word belong to all_determiministic_elements_list
			tmp = Arrays.asList(all_determiministic_elements_list).contains(inputSequence[i].toString());
			if (tmp) {
				// OK the word is a deterministic symbol
				tags = deterministicSymbolsTagDictionary.getTags(outcome);
				if (tags != null) {
					// OK we are talking about deterministic POS (i.e $PUNCT)
					//System.out.println("tags deterministicSymbolsTagDictionary: " + tags);
					tmp = Arrays.asList(tags).contains(inputSequence[i].toString());
					if (!tmp) {
						// if det_tagDictionary (i.e $PUNCT) does not contain the
						// input sequence (i.e. ",")
						//System.out.println("tags deterministicSymbolsTagDictionary not punct: returning false");
						return false;
					} else
					{
						//System.out.println("tags deterministicSymbolsTagDictionary yes punct: returning true");
						return true;
					}
				}
				// the hypothesis is not in the deterministicSymbolsTagDictionary returning false
				//System.out.println("The hypothesis is not in the deterministicSymbolsTagDictionary: returning false");
				return false;
			}
			else 
			{
				// check if the outcome is "FB","FC","FF","FS"
				if (Arrays.asList(determinist_pos_tag_list).contains(outcome))
				{
					//System.out.println("The outcome is inside determinist_pos_tag_list: returning false");
					return false;
				}
				
			}
			// OK the det_tagDictionary contains the correct input sequence
			// (i.e. ",") or tags == null
			// check for normal tag dict
		}
		if (tagDictionary == null) {
			//System.out.println("tagDictionary = null: returning true");
			return true;
		} else {
			//System.out.println("tagDictionary != null");
			tags = tagDictionary.getTags(inputSequence[i].toString());
			if (tags == null) {
				//System.out.println("tags tagDictionary == null: returning true");
				// we are not talking about probabilistic POS (i.e adjective,
				// verbs
				// ... )
				return true;
			} else {
				//System.out.println("tags tagDictionary != null: returning if...");
				// we are talking about about probabilistic POS (i.e adjective,
				// verbs ... )
				// return true if the outcome is OK with that
				tmp =  Arrays.asList(tags).contains(outcome);
				if (tmp)
				{
					//System.out.println("returning true");
					return true;
				} else
				{
					//System.out.println("returning false");
					return false;
				}
				//return Arrays.asList(tags).contains(outcome);
			}
		}
	}

}


