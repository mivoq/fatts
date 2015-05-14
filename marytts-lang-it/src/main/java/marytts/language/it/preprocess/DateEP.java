/**
 * Copyright 2002 DFKI GmbH.
 * Copyright 2013 MIVOQ S.R.L.
 *
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

package marytts.language.it.preprocess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import marytts.util.MaryUtils;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * An expansion pattern implementation for Italian date patterns.
 *
 * @author Giulio Paci
 */

public class DateEP extends ExpansionPattern
{
    private final String[] _knownTypes = {
        "date",
        "date:dmy",
		"date:ymd",
        "date:dm",
		"date:md",
        "date:my",
        "date:HM",
        "date:HMs"/*,
        "date:y",
		"date:m",
		"date:d",
		"date:mdy"*/
    };
    /**
     * Every subclass has its own list knownTypes, 
     * an internal string representation of known types.
     * These are possible values of the <code>type</code> attribute to the
     * <code>say-as</code> element, as defined in MaryXML.dtd.
     * If there is more than one known type, the first type
     * (<code>knownTypes[0]</code>) is expected to be the most general one,
     * of which the others are specialisations.
     */
    private final List<String> knownTypes = Arrays.asList(_knownTypes);
    public List<String> knownTypes() { return knownTypes; }

    // Domain-specific primitives:
    protected final static String sDay = "(?:0?[1-9]|[12][0-9]|3[01])";
    protected final static String sMonth = "(?:0?[1-9]|1[0-2]"
    		+"|gennaio|febbraio|marzo|aprile|maggio|giugno|luglio|agosto|settembre|ottobre|novembre|dicembre"
    		+"|(?:gen|feb|mar|apr|mag|giu|lug|ago|set|ott|nov|dic)[\\.]?"
    			+")";
    protected final static String sYear = "\\d{2,4}";
    protected final static String sSeparators = "[\\./_-]";
    protected final static String sHour = "(?:[01]?[1-90]|2[1-40])";
    protected final static String sMin = "(?:[1-50]?[1-90])";

    protected final Pattern reHourMin = 
            Pattern.compile("(" + sHour + ")" + "[:._-]" +
                         "(" + sMin + ")");

    protected final Pattern reHourMinSec = 
            Pattern.compile("(" + sHour + ")" + "[:._-]" +
                         "(" + sMin + ")" + "[:._-]" +
                         "(" + sMin + ")");

    protected final Pattern reDayMonthYear = 
            Pattern.compile("(" + sDay + ")" + sSeparators +
                         "(" + sMonth + ")" + sSeparators +
                         "(" + sYear + ")");

    protected final Pattern reYearMonthDay = 
            Pattern.compile("(" + sYear + ")" + sSeparators +
                         "(" + sMonth + ")" + sSeparators +
                         "(" + sDay + ")");

    protected final Pattern reDayMonth = 
            Pattern.compile("(" + sDay + ")" + "(?:" + sSeparators +")" +
                         "(" + sMonth + ")");

    protected final Pattern reMonthDay = 
            Pattern.compile("(" + sMonth + ")" + "(?:" + sSeparators +")" +
                         "(" + sDay + ")");

    protected final Pattern reMonthYear = 
            Pattern.compile("(" + sMonth + ")" + "(?:" + sSeparators +")" +
                         "(" + sYear + ")");
    public Pattern reDigits = Pattern.compile("\\d+");

    private final Pattern reMatchingChars = Pattern.compile("[a-zA-Z0-9./-_]");
    public Pattern reMatchingChars() { return reMatchingChars; }

    /**
     * Every subclass has its own logger.
     * The important point is that if several threads are accessing
     * the variable at the same time, the logger needs to be thread-safe
     * or it will produce rubbish.
     */
    private Logger logger = MaryUtils.getLogger("DateEP");

    public DateEP()
    {
        super();
    }
    private final String[] _monthabbr = {
            "gen", "Gennaio",
            "feb", "Febbraio",
            "mar", "Marzo",
            "apr", "Aprile",
            "mag", "Maggio",
            "giu", "Giugno",
            "lug", "Luglio",
            "ago", "Agosto",
            "set", "Settembre",
            "ott", "Ottobre",
            "nov", "Novembre",
            "dic", "Dicembre",
            "1", "Gennaio",
            "2", "Febbraio",
            "3", "Marzo",
            "4", "Aprile",
            "5", "Magio",
            "6", "Giugno",
            "7", "Luglio",
            "8", "Agosto",
            "9", "Settembre",
            "10", "Ottobre",
            "11", "Novembre",
            "12", "Dicembre"
    };
	private final Map<String, String> monthabbr = MaryUtils
			.arrayToMap(_monthabbr);

    protected int canDealWith(String s, int type){
        return match(s, type);
    }

    protected int match(String s, int type)
    {
        switch (type) {
        case 0:
            if (matchDateDMY(s)) return 1;
            if (matchDateYMD(s)) return 2;
            if (matchDateDM(s)) return 3;
            if (matchDateMD(s)) return 4;
            if (matchDateMY(s)) return 5;
            if (matchHourHM(s)) return 6;
            if (matchHourHMs(s)) return 7;
            break;
        case 1:
            if (matchDateDMY(s)) return 1;
            break;
        case 2:
            if (matchDateYMD(s)) return 2;
            break;
        case 3:
            if (matchDateDM(s)) return 3;
            break;
        case 4:
            if (matchDateMD(s)) return 4;
            break;
        case 5:
            if (matchDateMY(s)) return 5;
            break;
        case 6:
            if (matchHourHM(s)) return 6;
            break;
        case 7:
            if (matchHourHMs(s)) return 7;
            break;
        }
        return -1;
    }

    protected List<Element> expand(List<Element> tokens, String s, int type)
    {
        if (tokens == null) 
            throw new NullPointerException("Received null argument");
        if (tokens.isEmpty()) 
            throw new IllegalArgumentException("Received empty list");
        Document doc = ((Element)tokens.get(0)).getOwnerDocument();
        // we expect type to be one of the return values of match():
        List<Element> expanded = null;
        switch (type) {
        case 1:
            expanded = expandDateDMY(doc, s);
            break;
        case 2:
            expanded = expandDateYMD(doc, s);
            break;
        case 3:
            expanded = expandDateDM(doc, s);
            break;
        case 4:
            expanded = expandDateMD(doc, s);
            break;
        case 5:
            expanded = expandDateMY(doc, s);
            break;
        case 6:
            expanded = expandHourHM(doc, s);
            break;
        case 7:
            expanded = expandHourHMs(doc, s);
            break;
        }
        replaceTokens(tokens, expanded);
        return expanded;
    }

    protected boolean matchHourHM(String s)
    {
        return reHourMin.matcher(s).matches();
    }

    protected boolean matchHourHMs(String s)
    {
        return reHourMinSec.matcher(s).matches();
    }

    protected boolean matchDateDMY(String s)
    {
        return reDayMonthYear.matcher(s).matches();
    }
    
    protected boolean matchDateYMD(String s)
    {
        return reYearMonthDay.matcher(s).matches();
    }

    protected boolean matchDateDM(String s)
    {
        return reDayMonth.matcher(s).matches();
    }

    protected boolean matchDateMD(String s)
    {
        return reMonthDay.matcher(s).matches();
    }

    protected boolean matchDateMY(String s)
    {
        return reMonthYear.matcher(s).matches();
    }

    
    protected List<Element> expandDateDMY(Document doc, String s)
    {
        Matcher reMatcher = reDayMonthYear.matcher(s);
        boolean found = reMatcher.find();
        if (!found) {
            return null;
        }
        String day = reMatcher.group(1);
        String month = reMatcher.group(2);
        String year = reMatcher.group(3);
        return makeNewTokens(doc, expandDateDay(day, true) + " " + expandDateMonth(month) + " " + expandDateYear(year), true, s);
    }

    protected List<Element> expandDateYMD(Document doc, String s)
    {
        Matcher reMatcher = reYearMonthDay.matcher(s);
        boolean found = reMatcher.find();
        if (!found) {
            return null;
        }
        String month = reMatcher.group(2);
        String day = reMatcher.group(3);
        String year = reMatcher.group(1);
        return makeNewTokens(doc, expandDateYear(year) + " " + expandDateMonth(month) + " " + expandDateDay(day, false), true, s);
    }

    protected List<Element> expandDateMY(Document doc, String s)
    {
        Matcher reMatcher = reMonthYear.matcher(s);
        boolean found = reMatcher.find();
        if (!found) {
            return null;
        }
        String month = reMatcher.group(1);
        String year = reMatcher.group(2);
        return makeNewTokens(doc, expandDateMonth(month) + " " + expandDateYear(year), true, s);
    }

    protected List<Element> expandDateDM(Document doc, String s)
    {
        Matcher reMatcher = reDayMonth.matcher(s);
        boolean found = reMatcher.find();
        if (!found) {
            return null;
        }
        String day = reMatcher.group(1);
        String month = reMatcher.group(2);
        return makeNewTokens(doc, expandDateDay(day, true) + " " + expandDateMonth(month), true, s);
    }

    protected List<Element> expandDateMD(Document doc, String s)
    {
        Matcher reMatcher = reMonthDay.matcher(s);
        boolean found = reMatcher.find();
        if (!found) {
            return null;
        }
        String day = reMatcher.group(2);
        String month = reMatcher.group(1);
        return makeNewTokens(doc, expandDateMonth(month) + " " + expandDateDay(day, false), true, s);
    }

    protected List<Element> expandHourHM(Document doc, String s)
    {
        Matcher reMatcher = reHourMin.matcher(s);
        boolean found = reMatcher.find();
        if (!found) {
            return null;
        }
        String min = reMatcher.group(2);
        String hour = reMatcher.group(1);
        return makeNewTokens(doc, number.expandInteger(hour) + " e " + number.expandInteger(min), true, s);
    }

    protected List<Element> expandHourHMs(Document doc, String s)
    {
        Matcher reMatcher = reHourMinSec.matcher(s);
        boolean found = reMatcher.find();
        if (!found) {
            return null;
        }
        String sec = reMatcher.group(3);
        String min = reMatcher.group(2);
        String hour = reMatcher.group(1);
        return makeNewTokens(doc, number.expandInteger(hour) + ", " + number.expandInteger(min) + " e " + number.expandInteger(sec), true, s);
    }


    protected String expandDateYear(String s)
    {
		return number.expandInteger(s);
    }

	protected String expandDateMonth(String s) {
		Matcher reMatcher = reDigits.matcher(s);
		if (reMatcher.matches()) {
			return number.expandInteger(s);
		} else {
			String key = s.toLowerCase(Locale.ITALIAN).replaceAll("[.\\s]+$",
					"");
			if (this.monthabbr.containsKey(key)) {
				return this.monthabbr.get(key);
			} else {
				return s;
			}
		}
	}
    
    protected String expandDateDay(String s, boolean ordinal_if_one)
    {
		return number.expandInteger(s);
    }
    public static void main(String[] args)
    {
    	DateEP ep = new DateEP();
    	Matcher m;
		String tmp;
		System.err.println("YEARS:");
		tmp = "1";
    	m = Pattern.compile(sYear).matcher(tmp);
    	if(m.matches()){
    		System.err.println(tmp + " matches");
    	} else{
    		System.err.println(tmp + " does not match");
    	}
		tmp = "10";
    	m = Pattern.compile(sYear).matcher(tmp);
    	if(m.matches()){
    		System.err.println(tmp + " matches");
    	} else{
    		System.err.println(tmp + " does not match");
    	}
		tmp = "100";
    	m = Pattern.compile(sYear).matcher(tmp);
    	if(m.matches()){
    		System.err.println(tmp + " matches");
    	} else{
    		System.err.println(tmp + " does not match");
    	}
		tmp = "1000";
    	m = Pattern.compile(sYear).matcher(tmp);
    	if(m.matches()){
    		System.err.println(tmp + " matches");
    	} else{
    		System.err.println(tmp + " does not match");
    	}
		tmp = "10000";
    	m = Pattern.compile(sYear).matcher(tmp);
    	if(m.matches()){
    		System.err.println(tmp + " matches");
    	} else{
    		System.err.println(tmp + " does not match");
    	}
		System.err.println("SEPARATORS:");
		tmp = "/";
    	m = Pattern.compile(sSeparators).matcher(tmp);
    	if(m.matches()){
    		System.err.println(tmp + " matches");
    	} else{
    		System.err.println(tmp + " does not match");
    	}
		tmp = "-";
    	m = Pattern.compile(sSeparators).matcher(tmp);
    	if(m.matches()){
    		System.err.println(tmp + " matches");
    	} else{
    		System.err.println(tmp + " does not match");
    	}
		tmp = ".";
    	m = Pattern.compile(sSeparators).matcher(tmp);
    	if(m.matches()){
    		System.err.println(tmp + " matches");
    	} else{
    		System.err.println(tmp + " does not match");
    	}
		System.err.println("DMY:");
		tmp = "12/10/1980";
		ep.reDayMonthYear.matcher(tmp);
    	if(m.matches()){
    		System.err.println(tmp + " matches");
    	} else{
    		System.err.println(tmp + " does not match");
    	}
		tmp = "12-10-1980";
		ep.reDayMonthYear.matcher(tmp);
    	if(m.matches()){
    		System.err.println(tmp + " matches");
    	} else{
    		System.err.println(tmp + " does not match");
    	}
		tmp = "12.10.1980";
		ep.reDayMonthYear.matcher(tmp);
    	if(m.matches()){
    		System.err.println(tmp + " matches");
    	} else{
    		System.err.println(tmp + " does not match");
    	}
		tmp = "12.10.80";
		ep.reDayMonthYear.matcher(tmp);
    	if(m.matches()){
    		System.err.println(tmp + " matches");
    	} else{
    		System.err.println(tmp + " does not match");
    	}
		tmp = "12.ott.80";
		ep.reDayMonthYear.matcher(tmp);
    	if(m.matches()){
    		System.err.println(tmp + " matches");
    	} else{
    		System.err.println(tmp + " does not match");
    	}
		tmp = "12.ottobre.80";
		ep.reDayMonthYear.matcher(tmp);
    	if(m.matches()){
    		System.err.println(tmp + " matches");
    	} else{
    		System.err.println(tmp + " does not match");
    	}
		tmp = "12/ott./80";
		ep.reDayMonthYear.matcher(tmp);
    	if(m.matches()){
    		System.err.println(tmp + " matches");
    	} else{
    		System.err.println(tmp + " does not match");
    	}
		System.err.println("DM:");
		tmp = "12/10";
		ep.reDayMonthYear.matcher(tmp);
    	if(m.matches()){
    		System.err.println(tmp + " matches");
    	} else{
    		System.err.println(tmp + " does not match");
    	}
    	tmp = "12001";
    	int ret = ep.match(tmp, 0);
		System.err.println(tmp + " matches " + ret);
    }
    
    
}
