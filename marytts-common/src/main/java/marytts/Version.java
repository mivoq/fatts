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
package marytts;

import java.io.IOException;
import java.io.InputStream;

import marytts.util.io.FileUtils;


/**
 * Provide Version information for the Mary server and client.
 * @author Marc Schr&ouml;der
 *
 */
public class Version {
    private static String specificationVersion;
    private static String implementationVersion;
    private static String implementationVersionTime;
    private static String implementationBuildTime;

    static {
        InputStream specVersionStream = Version.class.
            getResourceAsStream("specification-version.txt");
        if (specVersionStream != null) {
            try {
                specificationVersion = FileUtils.getStreamAsString(specVersionStream, "UTF-8").trim();
            } catch (IOException e) {
                specificationVersion = "undeterminable";
            }
        } else {
            specificationVersion = "unknown";
        }
        
        InputStream implVersionStream = Version.class.
            getResourceAsStream("git.properties");
        if (implVersionStream != null) {
            try {
	      java.util.Properties properties = new java.util.Properties();
	      properties.load(implVersionStream);
	      implementationVersion = properties.getProperty("git.commit.id", "missing");
	      implementationVersionTime = properties.getProperty("git.commit.time", "missing");
	      implementationBuildTime = properties.getProperty("git.build.time", "missing");
            } catch (IOException e) {
                implementationVersion = "undeterminable";
		implementationVersionTime = "undeterminable";
		implementationBuildTime = "undeterminable";
            }
        } else {
            implementationVersion = "unknown";
	    implementationVersionTime = "unknown";
	    implementationBuildTime = "unknown";
        }
	
    }

    /** Specification version */
    public static String specificationVersion() {
        return specificationVersion;
    }
    /** Implementation version */
    public static String implementationVersion() {
        return implementationVersion;
    }
    /** Implementation version */
    public static String implementationVersionTime() {
        return implementationVersionTime;
    }
    /** Implementation version */
    public static String implementationBuildTime() {
        return implementationBuildTime;
    }

}

