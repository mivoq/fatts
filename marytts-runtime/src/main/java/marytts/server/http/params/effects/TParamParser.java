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
import java.util.Vector;
import java.util.HashMap;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.Set;


/**
 * 
 */
public class TParamParser extends ParamParser {
  private static class Attributes extends AbstractMap<String, Object> {
    final Map<String, Object> content = new HashMap<String, Object>();

    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
      return content.entrySet();
    }

    @Override
    public Set<String> keySet() {
      return content.keySet();
    }

    @Override
    public Collection<Object> values() {
      return content.values();
    }

    @Override
    public Object put(final String key, final Object value) {
      return content.put(key, value);
    }
  }
  
  Vector<ParamParser> vs = new Vector<ParamParser>();

  public TParamParser( String id, String name, String description, MergeStrategy mergestrategy ) {
    super( id, name, description, mergestrategy, Type.T);
  }

  public void addParameter(ParamParser v) {
    vs.add(v);
  }

  protected void addToJSONObject(JSONObject o) {
    super.addToJSONObject(o);
    for(ParamParser v : vs) {
      JSONObject ov = new JSONObject();
      v.addToJSONObject(ov);
      o.append("attributes", ov);
    }
  }

  public Object parse(Object v) {
    Attributes ret = new Attributes();
    if (v instanceof JSONObject) {
      for(ParamParser parser : vs) {
	Object t = ((JSONObject)v).get(parser.id);
	if(t != null) {
	  t = parser.parse(t);
	  ret.put(parser.id, t);
	}
      }
      return ret;
    } else {
      throw new java.lang.IllegalArgumentException("Expected T value not found.");
    }
  }

  public String toOldStyleString(Object v) {
    StringBuilder sb = new StringBuilder();
    for(Map.Entry<String, Object> entry: ((Attributes)v).entrySet()) {
      if(sb.length()>0) {
	sb.append(';');
      } else {
	sb.append(this.id);
	sb.append('(');
      }
      sb.append(entry.getKey() + ":" + entry.getValue());
    }
    if(sb.length()>0) {
      sb.append(')');
    }
    return sb.toString();
  }

}
