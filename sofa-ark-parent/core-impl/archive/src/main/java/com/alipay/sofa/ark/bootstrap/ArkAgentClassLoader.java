/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alipay.sofa.ark.bootstrap;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;

/**
 * 这个包里面的代码，基本都是用AppClassLoader加载的，一般不要引用其他包，否则容易造成类冲突
 * 例如，这里使用了ClassLoaderUtils，导致no class def error
 * @Author: jessin
 * @Date: 2022/3/24 11:57 上午
 */
public class ArkAgentClassLoader extends URLClassLoader {
    private static ClassLoader classLoader;

    static {
        ClassLoader.registerAsParallelCapable();
    }

    public ArkAgentClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public static ClassLoader getInstance() {
        if (classLoader == null) {
            synchronized (ArkAgentClassLoader.class) {
                if (classLoader == null) {
                    System.out.println("build ArkAgentClassLoader....");
                    classLoader = new ArkAgentClassLoader(getAgentClassPath(), null);
                }
            }
        }
        return classLoader;
    }

    private static final String JAVA_AGENT_MARK        = "-javaagent:";

    private static final String JAVA_AGENT_OPTION_MARK = "=";

    public static URL[] getAgentClassPath() {
        List<String> inputArguments = AccessController
            .doPrivileged(new PrivilegedAction<List<String>>() {
                @Override
                public List<String> run() {
                    return ManagementFactory.getRuntimeMXBean().getInputArguments();
                }
            });

        List<URL> agentPaths = new ArrayList<>();
        for (String argument : inputArguments) {
            if (!argument.startsWith(JAVA_AGENT_MARK)) {
                continue;
            }
            argument = argument.substring(JAVA_AGENT_MARK.length());
            try {
                String path = argument.split(JAVA_AGENT_OPTION_MARK)[0];
                URL url = new File(path).toURI().toURL();
                agentPaths.add(url);
            } catch (Throwable e) {
                throw new RuntimeException("Failed to create java agent classloader", e);
            }
        }
        return agentPaths.toArray(new URL[] {});
    }
}