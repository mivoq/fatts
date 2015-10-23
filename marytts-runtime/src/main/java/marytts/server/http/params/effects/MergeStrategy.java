/**
 * Copyright: 2015, Giulio Paci <giuliopaci@gmail.com>
 * License: MIT/Expat
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

package marytts.server.http.params.effects;
/**
 * 
 */
public enum MergeStrategy {
  ADD("add"),
  MULTIPLY("multiply"),
  OVERWRITE("overwrite"),
  NATURAL("natural"),
  CUSTOM("custom");

  private String string;
      
  private MergeStrategy(String name){string = name;}
      
  @Override
  public String toString() {
    return string;
  }

  public double merge(double prev, double next) {
    switch(this){
    case ADD:
      return prev+next;
    case MULTIPLY:
      return prev*next;
    case OVERWRITE:
      return next;
    }
    return prev;
  }

  public long merge(long prev, long next) {
    switch(this){
    case ADD:
      return prev+next;
    case MULTIPLY:
      return prev*next;
    case OVERWRITE:
      return next;
    }
    return prev;
  }

  public boolean merge(boolean prev, boolean next) {
    switch(this){
    case ADD:
      return prev || next;
    case MULTIPLY:
      return prev && next;
    case OVERWRITE:
      return next;
    }
    return prev;
  }

  public Double merge(Double prev, Double next) {
    return new Double(this.merge(prev.doubleValue(), next.doubleValue()));
  }

  public Long merge(Long prev, Long next) {
    return new  Long(this.merge(prev.longValue(), next.longValue()));
  }

  public Boolean merge(Boolean prev, Boolean next) {
    return new  Boolean(this.merge(prev.booleanValue(), next.booleanValue()));
  }

  public Object merge(Object prev, Object next) {
    if((prev instanceof Boolean) && (next instanceof Boolean)) {
      return this.merge((Boolean)prev, (Boolean)next);

    }
    else if((prev instanceof Double) && (next instanceof Double)) {
      return this.merge((Double)prev, (Double)next);
    }
    else if((prev instanceof Long) && (next instanceof Long)) {
      return this.merge((Long)prev, (Long)next);
    }
    if(this == OVERWRITE) {
      return next;
    }
    return prev;
  }
}
