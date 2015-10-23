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
public class ParamParser {
  String id;
  String name;
  String description;
  Type type;
  MergeStrategy mergestrategy;
  public ParamParser(String id, String name, String description, MergeStrategy mergestrategy, Type type) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.type = type;
    this.mergestrategy = mergestrategy;
  }

  protected void addToJSONObject(JSONObject o) {
    o.put("id", this.id);
    o.put("name", this.name);
    o.put("description", this.description);
    o.put("type", this.type.toString());
    o.put("merge", this.mergestrategy.toString());
  }

  public JSONObject toJSONObject() {
    JSONObject o = new JSONObject();
    this.addToJSONObject(o);
    return o;
  }

  public Object getValueFromJSONString(String s) {
    JSONObject obj = new JSONObject(s);
    return this.getValueFromJSONObject(obj, this.id);
  }

  protected Object getValueFromJSONObject(JSONObject obj) {
    return this.getValueFromJSONObject(obj, this.id);
  }
  private Object getValueFromJSONObject(JSONObject obj, String valueid) {
    if(obj.has(valueid)) {
      switch(this.type) {
      case BOOLEAN:
	return new Boolean(obj.getBoolean(valueid));
      case DOUBLE:
	return new Double(obj.getDouble(valueid));
      case INTEGER:
	return new Integer(obj.getInt(valueid));
      }
    }
    return null;
  }
  
  public String toOldStyleString(String id, Object value) {
    if(value == null)
      return null;
    switch(this.type) {
    case BOOLEAN:
      if(((Boolean)value).booleanValue()) {
	return id+"()";
      }
      return null;
    case DOUBLE:
      return id+"(amount:"+ value +")";
    case INTEGER:
      return id+"(amount:"+ value +")";
    case ENUM:
      return id+"("+ value +")";
    }
    return null;
  }

  public String toOldStyleString(Object value) {
    return this.toOldStyleString(this.id, value);
  }

  public Object parse(Object value) {
    return value;
  }

  public Object limit(Object value) {
    return value;
  }

  public Object merge(Object prev, Object next) {
    return this.mergestrategy.merge(prev, next);
  }

  @Override
  public String toString() {
    return this.toJSONObject().toString();
  }
}
