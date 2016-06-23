/*
 * Copyright 2012 The Sainfy Open Software.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sainfy.jxvs.launcher;

import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.TimeZone;

import org.Constants;
import org.sainfy.jxvs.JXVSServer;

/**
 *
 * @author luoaz <luoanzhu@gmail.com>
 */
public class Launcher {

    private static final String DEFAULT_LIB_DIR = "../lib";
    private static final String JXVSSERVER = "org.sainfy.jxvs.JXVSServer";
    public static void main(String[] args) {
        new Launcher().start();
    }
    
    private void start() {
        TimeZone.setDefault(TimeZone.getTimeZone(System.getProperty("timeZone", "GMT-0")));
        String jxvsHome = System.getProperty(Constants.JXVS_HOME);
        if (jxvsHome == null || jxvsHome.length() <= 0) {
            jxvsHome = System.getProperty("user.dir") + File.separator + "src";
            System.setProperty(Constants.JXVS_HOME, jxvsHome);
            //System.setProperty("user.dir", jxvsHome + "/bin");
        }
        String javaLibPath = System.getProperty("java.library.path");
        System.setProperty("java.library.path", javaLibPath
                + File.pathSeparator + jxvsHome + "/resources/sigar");
        
        //log path
        String logsPath = new File(jxvsHome, "logs").getAbsolutePath();
        System.setProperty(Constants.JXVS_LOGS_DIR, logsPath);
        
        final ClassLoader parent = findParentClassLoader();
        String libPath = System.getProperty(Constants.JXVS_LIB_DIR);
        File libDir = null;
        if (libPath != null) {
            libDir = new File(libPath);
        }
        if (libDir == null || !libDir.exists()) {
            libDir = new File(DEFAULT_LIB_DIR);
        }
        
        try {
            File file = new File(jxvsHome, "conf");
            ClassLoader confLoader = null;
            if (file.exists()) {
                confLoader = new ConfClassLoader(parent, file);
                Thread.currentThread().setContextClassLoader(confLoader);
            }
            if (libDir.exists()) {
                ClassLoader loader = new JXVSClassLoader(confLoader, libDir);
                Thread.currentThread().setContextClassLoader(loader);
                Class<?> containerClass = loader.loadClass(JXVSSERVER);
                containerClass.newInstance();
            } else {
                new JXVSServer();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ClassLoader findParentClassLoader() {
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        if (parent == null) {
            parent = this.getClass().getClassLoader();
            if (parent == null) {
                parent = ClassLoader.getSystemClassLoader();
            }
        }
        return parent;
    }
    
    class JXVSClassLoader extends URLClassLoader {

        /**
         * Constructs the classloader.
         * 
         * @param parent
         *            the parent class loader (or null for none).
         * @param libDir
         *            the directory to load jar files from.
         * @throws java.net.MalformedURLException
         *             if the libDir path is not valid.
         */
        JXVSClassLoader(ClassLoader parent, File libDir)
                throws MalformedURLException {
            super(new URL[] { libDir.toURI().toURL() }, parent);
            File[] jars = libDir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    boolean accept = false;
                    String smallName = name.toLowerCase();
                    if (smallName.endsWith(".jar")) {
                        accept = true;
                    } else if (smallName.endsWith(".zip")) {
                        accept = true;
                    }
                    return accept;
                }
            });

            // Do nothing if no jar or zip files were found
            if (jars == null) {
                return;
            }

            for (int i = 0; i < jars.length; i++) {
                if (jars[i].isFile()) {
                    addURL(jars[i].toURI().toURL());
                }
            }

        }
    }
    
    
    class ConfClassLoader extends URLClassLoader {

        /**
         * Constructs the classloader.
         * 
         * @param parent
         *            the parent class loader (or null for none).
         * @param confDir
         *            the directory to load jar files from.
         * @throws java.net.MalformedURLException
         *             if the libDir path is not valid.
         */
        ConfClassLoader(ClassLoader parent, File confDir)
                throws MalformedURLException {
            super(new URL[] { confDir.toURI().toURL() }, parent);

            File[] files = confDir.listFiles();
            for (File file : files) {
                if (!file.isDirectory()) {
                    addURL(file.toURI().toURL());
                }
            }
        }
    }
    
}
