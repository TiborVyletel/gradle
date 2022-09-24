/*
 * Copyright 2016 the original author or authors.
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
package org.gradle.initialization;

import org.gradle.api.Action;
import org.gradle.api.initialization.dsl.ScriptHandler;
import org.gradle.groovy.scripts.DefaultScript;

public abstract class InitScript extends DefaultScript {
    public ScriptHandler getInitscript() {
        return getBuildscript();
    }

    /**
     * Configures the classpath for this init script.
     *
     * @param action closure to configure the classpath
     *
     * @see #buildscript(Action)
     *
     * @since 8.0
     */
    public void initscript(Action<? super ScriptHandler> action) {
        buildscript(action);
    }

    public String toString() {
        return "initialization script";
    }
}
