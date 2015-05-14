/**
 * Copyright 2000-2008 DFKI GmbH.
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

/**
 * Override or add new features to MaryGenericFeatureProcessors for Italian
 * Features described in marytts.language.it.features.MaryGenericFeatureProcessors override the marytts.features.MaryGenericFeatureProcessors ones
 * 
 * @author Fabio Tesser
 * 
 */

package marytts.language.it.features;

import marytts.language.it.features.MaryGenericFeatureProcessors;
import java.util.Locale;
import marytts.exceptions.MaryConfigurationException;
import marytts.modules.synthesis.Voice;
import marytts.util.MaryUtils;

public class FeatureProcessorManager extends
        marytts.features.FeatureProcessorManager {
    
    /**
     * Builds a new FeatureProcessorManager for Italian. 
     * This manager uses ...
     * All feature processors loaded are language specific.
     */

	public FeatureProcessorManager(String localeString)
			throws MaryConfigurationException {
		this(MaryUtils.string2locale(localeString));
	}

	public FeatureProcessorManager(Locale locale)
			throws MaryConfigurationException {
		super(locale);
		setupAdditionalFeatureProcessors();

	}
    
    /**
     * Constructor called from a Voice in Locale IT that has its own acoustic models
     * 
     * @param voice
     */
    public FeatureProcessorManager(Voice voice)
    throws MaryConfigurationException {
        super(voice.getLocale());
        setupAdditionalFeatureProcessors();
        registerAcousticModels(voice);
    }

	/**
	 * Specific FeatureProcessors setup for Italian 
	 */
	private void setupAdditionalFeatureProcessors() {
		try {

			// set the targets 
			MaryGenericFeatureProcessors.TargetElementNavigator syllable = new MaryGenericFeatureProcessors.SyllableNavigator();
			MaryGenericFeatureProcessors.TargetElementNavigator sentence = new MaryGenericFeatureProcessors.SentenceNavigator();
			
			// override of the following 
			// TODO: exploring the composite and subordinate sentences like "Ciao amici lontani e vicini, e tu come ti chiami?"
			addFeatureProcessor(new MaryGenericFeatureProcessors.Selection_Prosody(syllable));
			
			// add sentence_type features 
			//TODO: is that necessary ? should be better the POS of the first sentence word?  
			addFeatureProcessor(new MaryGenericFeatureProcessors.SentenceType(sentence));

		} catch (Exception e) {
			e.printStackTrace();
			throw new Error("Problem building additional FeatureProcessors");
		}
	}

    @Override
    public Locale getLocale()
    {
        return Locale.ITALIAN;
    }
 
}

