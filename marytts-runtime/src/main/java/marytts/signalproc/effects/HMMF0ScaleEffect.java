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
package marytts.signalproc.effects;

import marytts.util.data.DoubleDataSource;
import marytts.util.math.MathUtils;

/**
 * @author Oytun T&uumlrk
 */
public class HMMF0ScaleEffect extends BaseAudioEffect {
    public float amount;
    public static float NO_MODIFICATION = 1.0f;
    public static float DEFAULT_AMOUNT = 2.0f;
    public static float MAX_AMOUNT = 3.0f;
    public static float MIN_AMOUNT = 0.0f;
    
    public HMMF0ScaleEffect()
    {
        super(16000);
        
        setHMMEffect(true);
        
        setExampleParameters("amount" + chParamEquals + Float.toString(DEFAULT_AMOUNT) + chParamSeparator);
    }
    
    public void parseParameters(String param)
    {
        super.parseParameters(param);
        
        amount = expectFloatParameter("amount");

	// Backward compatibility with MaryTTS
        if (amount == NULL_FLOAT_PARAM)
	  amount = expectFloatParameter("f0Scale");
        
        if (amount == NULL_FLOAT_PARAM)
            amount = DEFAULT_AMOUNT;
        
        amount = MathUtils.CheckLimits(amount, MIN_AMOUNT, MAX_AMOUNT);
    }
    
    //Actual processing is done within the HMM synthesizer so do nothing here
    public DoubleDataSource process(DoubleDataSource input)
    {
        return input;
    }

    public String getHelpText() {
        
        String strHelp = "F0 scaling effect for HMM voices:" + strLineBreak +
                         "All voiced f0 values are multiplied by <amount> for HMM voices." + strLineBreak +
                         "This operation effectively scales the range of f0 values." + strLineBreak +
                         "Note that mean f0 is preserved during the operation." + strLineBreak +
                         "Parameter:" + strLineBreak +
                         "   <amount>" +
                         "   Definition : Scale ratio for modifying the dynamic range of the f0 contour" + strLineBreak +
                         "                If amount>1.0, the range is expanded (i.e. voice with more variable pitch)" + strLineBreak +
                         "                If amount<1.0, the range is compressed (i.e. more monotonic voice)" + strLineBreak +
                         "                If amount=1.0 results in no changes in range" + strLineBreak +
                         "   Range      : [" + String.valueOf(MIN_AMOUNT) + "," + String.valueOf(MAX_AMOUNT) + "]" + strLineBreak +
                         "Example:" + strLineBreak +
                         getExampleParameters();
                        
        return strHelp;
    }

    public String getName() {
        return "F0Scale";
    }
}

