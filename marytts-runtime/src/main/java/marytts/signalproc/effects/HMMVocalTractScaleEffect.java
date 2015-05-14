package marytts.signalproc.effects;

import marytts.util.data.DoubleDataSource;
import marytts.util.math.MathUtils;

/**
 * @author Fabio Tesser 
 */

/*
 *  HMMVocalTractScaleEffect is a Vocal Tract Scaling Effect
 *  that operate in the mel cepstrum domain
 *  If an HMM voice is used this is less computational expensive.
 *  Moreover it provides a better results as source and spectral envelopes are separated.
 */
public class HMMVocalTractScaleEffect extends BaseAudioEffect {
    public float amount;
    public static float NO_MODIFICATION = 1.0f;
    public static float DEFAULT_AMOUNT = 1.5f;
    public static float MAX_AMOUNT = 4.0f;
    public static float MIN_AMOUNT = 0.25f;
    
    public HMMVocalTractScaleEffect()
    {
        super(16000);
        
        setHMMEffect(true);
        
        setExampleParameters("amount" + chParamEquals + Float.toString(DEFAULT_AMOUNT) + chParamSeparator);        
    }
    
    public void parseParameters(String param)
    {
        super.parseParameters(param);
        
        amount = expectFloatParameter("amount");
        
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

        String strHelp = "MCEP Vocal Tract Scaling Effect:" + strLineBreak +
                         "Creates a shortened or lengthened vocal tract effect by shifting the formants." + strLineBreak +
                         "Parameter:" + strLineBreak +
                         "   <amount>" +
                         "   Definition : The amount of formant shifting" + strLineBreak +
                         "   Range      : [" + String.valueOf(MIN_AMOUNT) + "," + String.valueOf(MAX_AMOUNT) + "]" + strLineBreak +
                         "   For values of <amount> less than 1.0, the formants are shifted to lower frequencies" + strLineBreak +
                         "       resulting in a longer vocal tract (i.e. a deeper voice)." + strLineBreak +
                         "   Values greater than 1.0 shift the formants to higher frequencies." + strLineBreak +
                         "       The result is a shorter vocal tract.\n" + strLineBreak +
                         "Example:" + strLineBreak +
                         getExampleParameters();
                        
        return strHelp;
    }
    	
    	

    public String getName() {
        return "HMMTractScaler";
    }


}
