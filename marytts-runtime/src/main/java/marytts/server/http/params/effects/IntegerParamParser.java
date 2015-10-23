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
import org.json.*;

/**
 * 
 */
public class IntegerParamParser extends ParamParser {
  long min;
  long max;
  long sample;
  long identity;

  public IntegerParamParser( String id, String name, String description, MergeStrategy mergestrategy, long identity, long min, long max, long sample ) {
    super( id, name, description, mergestrategy, Type.INTEGER);
    this.min = min;
    this.max = max;
    this.sample = sample;
    this.identity = identity;
  }

  public Object parse(Object v) {
    if (v instanceof Long) {
      return v;
    } else if (v instanceof Integer) {
      return new Long(((Integer)v).longValue());
    } else {
      throw new java.lang.IllegalArgumentException("Expected Double value not found.");
    }
  }

  public Object merge(Object prev, Object next) {
    return super.mergestrategy.merge((Long)prev, (Long)next);
  }

  public Object limit(Object value) {
    return new Long(this.limit(((Long)value).longValue()));
  }
    
  public long limit(long v) {
    if(v < this.min)
      v = this.min;
    else if(v > this.max)
      v = this.max;
    return v;
  }

  public long merge( long prev, long next ) {
    return super.mergestrategy.merge(prev, next);
  }

  protected void addToJSONObject(JSONObject o) {
    super.addToJSONObject(o);
    o.put("min", this.min);
    o.put("max", this.max);
    o.put("sample", this.sample);
    o.put("identity", this.identity);
  }
}
