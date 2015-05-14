/**
 * Copyright 2000-2006 DFKI GmbH.
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
package marytts.language.it.phonemiser;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;

import marytts.modules.phonemiser.Allophone;
import marytts.modules.phonemiser.AllophoneSet;


public class Syllabifier extends marytts.modules.phonemiser.Syllabifier
{
    private Map<String,String> unstress_map = null;
    private Map<String,String> stress_map = null;
    private static Map<String,String> initUnstressMap(AllophoneSet allophoneSet){
		Map<String,String> unstress_map = new HashMap<String,String>();
		for (Iterator<String> iterator = allophoneSet.getAllophoneNames()
				.iterator(); iterator.hasNext();) {
			String ph_a = iterator.next();
			Allophone a = allophoneSet.getAllophone(ph_a);
			if (a.isVowel()) {
				Map<String, String> a_features = a.getFeatures();
				String a_stressed = a_features.get("stressed");
				if ((a_stressed != null) && (a_stressed.equals("+"))) {
					int a_vheight = -1;
					try {
						a_vheight = Integer.parseInt(a_features.get("vheight"));
					} catch (Exception e) {
					}
					int last_b_vheight = -1;
					for (Iterator<String> n_iterator = allophoneSet
							.getAllophoneNames().iterator(); n_iterator
							.hasNext();) {
						String ph_b = n_iterator.next();
						Allophone b = allophoneSet.getAllophone(ph_b);
						boolean change_to_b = true;
						if (b.isVowel()) {
							Map<String, String> b_features = b.getFeatures();
							String b_stressed = b_features.get("stressed");
							if ((b_stressed != null)
									&& (b_stressed.equals("+"))) {
								change_to_b = false;
							} else {
								for (String s : b_features.keySet()) {
									if (!s.equals("stressed")) {
										if (!b_features.get(s).equals(
												a_features.get(s))) {
											if (s.equals("vheight")) {
												int b_vheight = -1;
												try {
													b_vheight = Integer.parseInt(b_features.get("vheight"));
												} catch (Exception e) {
												}
												if( (b_vheight >= last_b_vheight) && (b_vheight <= a_vheight) ){
													last_b_vheight = b_vheight;
												}
												else{
													change_to_b = false;
												}
											} else {
												change_to_b = false;
											}
										}
									}
								}
							}
							if (change_to_b) {
								unstress_map.put(ph_a, ph_b);
							}
						}
					}
				} else {
					unstress_map.put(ph_a, ph_a);
				}
			} else {
				unstress_map.put(ph_a, ph_a);
			}
		}
		return unstress_map;
    }
    private static Map<String,String> initStressMap(AllophoneSet allophoneSet){
		Map<String,String> stress_map = new HashMap<String,String>();
		for (Iterator<String> iterator = allophoneSet.getAllophoneNames()
				.iterator(); iterator.hasNext();) {
			String ph_a = iterator.next();
			Allophone a = allophoneSet.getAllophone(ph_a);
			if (a.isVowel()) {
				Map<String, String> a_features = a.getFeatures();
				String a_stressed = a_features.get("stressed");
				if ((a_stressed == null) || (!a_stressed.equals("+"))) {
					int a_vheight = -1;
					try {
						a_vheight = Integer.parseInt(a_features.get("vheight"));
					} catch (Exception e) {
					}
					int last_b_vheight = Integer.MAX_VALUE;
					for (Iterator<String> n_iterator = allophoneSet
							.getAllophoneNames().iterator(); n_iterator
							.hasNext();) {
						String ph_b = n_iterator.next();
						Allophone b = allophoneSet.getAllophone(ph_b);
						boolean change_to_b = true;
						if (b.isVowel()) {
							Map<String, String> b_features = b.getFeatures();
							String b_stressed = b_features.get("stressed");
							if ((b_stressed == null)
									|| (!b_stressed.equals("+"))) {
								change_to_b = false;
							} else {
								for (String s : b_features.keySet()) {
									if (!s.equals("stressed")) {
										if (!b_features.get(s).equals(
												a_features.get(s))) {
											if (s.equals("vheight")) {
												int b_vheight = -1;
												try {
													b_vheight = Integer.parseInt(b_features.get("vheight"));
												} catch (Exception e) {
												}
												if( (b_vheight <= last_b_vheight) && (b_vheight >= a_vheight) ){
													last_b_vheight = b_vheight;
												}
												else{
													change_to_b = false;
												}
											} else {
												change_to_b = false;
											}
										}
									}
								}
							}
							if (change_to_b) {
								stress_map.put(ph_a, ph_b);
							}
						}
					}
				} else {
					stress_map.put(ph_a, ph_a);
				}
			} else {
				stress_map.put(ph_a, ph_a);
			}
		}
		return stress_map;
    }
    
    public Syllabifier(AllophoneSet allophoneSet, boolean removeTrailingOneFromPhones)
    {
    	super(allophoneSet, removeTrailingOneFromPhones);
    	this.unstress_map = Syllabifier.initUnstressMap(allophoneSet);
    	this.stress_map = Syllabifier.initStressMap(allophoneSet);
    }
    
    public Syllabifier(AllophoneSet allophoneSet)
    {
    	super(allophoneSet);
    	this.unstress_map = Syllabifier.initUnstressMap(allophoneSet);
    	this.stress_map = Syllabifier.initStressMap(allophoneSet);
    }

	private String unstress_phone(String ph) {
		String ret = this.unstress_map.get(ph);
		if (ret == null) {
			return ph;
		}
		return ret;
	}

	private String stress_phone(String ph) {
		String ret = this.stress_map.get(ph);
		if (ret == null) {
			return ph;
		}
		return ret;
	}
    
    /**
     * For those syllables containing a "1" character, remove that "1"
     * character and add a stress marker ' at the beginning of the syllable.
     */
    protected void correctStressSymbol(LinkedList<String> phoneList)
    {
		int stress_index = -1;
		ListIterator<String> it = phoneList.listIterator(0);
		while (it.hasNext()) {
			String s = it.next();
			if (s.endsWith("1")) {
				if (this.removeTrailingOneFromPhones) {
					it.set(s.substring(0, s.length() - 1)); // delete "1"
				}
				if (stress_index != -1) {
					phoneList.set(stress_index,this.unstress_phone(phoneList.get(stress_index)));
				}
				stress_index = it.nextIndex() - 1;
			}
		}
		if (stress_index != -1) {
			it = phoneList.listIterator(stress_index);
			while (it.hasPrevious()) {
				String t = it.previous();
				if (t.equals("-") || t.equals("_")) { // syllable boundary
					it.next();
					break;
				}
			}
			it.add("'");
		} else {
			// No stressed vowel in word?
            // if the word does not end with a vowel and last syllable contains a vowel, stress the last syllable
        	// otherwise stress the second last syllable (or none)
			it = phoneList.listIterator();
			String s = null;
			int phonenumber = 0;
			int lastVowelIndex = -1;
			int lastLastVowelIndex = -1;
			int syllableToStressIndex = -1;

			while (it.hasNext()) {
				s = it.next();
				if (s != null) {
					Allophone ph = allophoneSet.getAllophone(s);
					if (ph != null) {
						if (ph != null && ph.sonority() >= 5) { // non-schwa
																// vowel
							lastLastVowelIndex = lastVowelIndex;
							lastVowelIndex = phonenumber;
						}
					}
				}
				phonenumber++;
			}

			syllableToStressIndex = lastLastVowelIndex;
			if (lastVowelIndex > -1) {
				it = phoneList.listIterator(lastVowelIndex);
				while (it.hasNext()) {
					s = it.next();
					if (s != null) {
						if (!(s.equals("-") || s.equals("_"))) { // not syllable boundary
							Allophone ph = allophoneSet.getAllophone(s);
							if (ph != null) {
								if (!(ph != null && ph.sonority() >= 5)) { // not non-schwa vowel
									// so the words does not end with vowel
									syllableToStressIndex = lastVowelIndex;
									break;
								}
							}
						}
					}
				}
			}
			if(syllableToStressIndex > -1){
				it = phoneList.listIterator(syllableToStressIndex);
					if(syllableToStressIndex > -1){
					phoneList.set(syllableToStressIndex,this.stress_phone(phoneList.get(syllableToStressIndex)));
				}
				stress_index = it.nextIndex() - 1;
				while (it.hasPrevious()) {
					s = it.previous();
					if (s != null) {
						if ((s.equals("-") || s.equals("_"))) { // syllable boundary
							it.next();
							it.add("'");
							syllableToStressIndex = -1;
							break;
						}
					}
				}
				if(syllableToStressIndex > -1){
					it.add("'");
				}
			}
		}
    }


}

