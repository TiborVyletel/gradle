/*
 * Copyright 2020 the original author or authors.
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

package org.gradle.internal.enterprise

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.plugin.management.internal.autoapply.AutoAppliedGradleEnterprisePlugin

import static org.gradle.internal.enterprise.GradleEnterprisePluginConfig.BuildScanRequest.NONE
import static org.gradle.internal.enterprise.GradleEnterprisePluginConfig.BuildScanRequest.REQUESTED
import static org.gradle.internal.enterprise.GradleEnterprisePluginConfig.BuildScanRequest.SUPPRESSED

class GradleEnterprisePluginConfigIntegrationTest extends AbstractIntegrationSpec {

    def plugin = new GradleEnterprisePluginCheckInFixture(testDirectory, mavenRepo, createExecuter())

    def setup() {
        settingsFile << plugin.pluginManagement()
        plugin.publishDummyPlugin(executer)
        buildFile << """
            task t
        """
    }

    def "has none requestedness if no switch present"() {
        given:
        settingsFile << plugin.plugins()

        when:
        succeeds "t"

        then:
        plugin.assertBuildScanRequest(output, NONE)
        plugin.assertAutoApplied(output, false)
    }

    def "is requested with --scan"() {
        given:
        if (!autoApplied) {
            settingsFile << plugin.plugins()
        }

        when:
        succeeds "t", "--scan"

        then:
        plugin.assertBuildScanRequest(output, REQUESTED)
        plugin.assertAutoApplied(output, autoApplied)

        where:
        autoApplied << [true, false]
    }

    def "is suppressed with --no-scan"() {
        given:
        settingsFile << plugin.plugins()

        when:
        succeeds "t", "--no-scan"

        then:
        plugin.assertBuildScanRequest(output, SUPPRESSED)
        plugin.assertAutoApplied(output, false)
    }

    def "is not auto-applied when added to classpath via buildscript block"() {
        given:
        def coordinates = "${groupId}:${artifactId}:${plugin.runtimeVersion}"
        settingsFile << """
            buildscript {
                repositories {
                    maven { url '${mavenRepo.uri}' }
                }
                dependencies {
                    classpath("${coordinates}")
                }
            }

            apply plugin: 'com.gradle.enterprise'
        """

        when:
        succeeds "t", "--scan"

        then:
        plugin.assertAutoApplied(output, false)

        where:
        groupId                                 | artifactId
        'com.gradle'                            | 'gradle-enterprise-gradle-plugin'
        AutoAppliedGradleEnterprisePlugin.ID.id | "${AutoAppliedGradleEnterprisePlugin.ID.id}.gradle.plugin"
    }

    def "is auto-applied when --scan is used despite init script"() {
        given:
        def pluginArtifactId = "com.gradle:gradle-enterprise-gradle-plugin:${plugin.runtimeVersion}"
        def initScript = file("build-scan-init.gradle") << """
            initscript {
                repositories {
                    maven { url '${mavenRepo.uri}' }
                }
                dependencies {
                    classpath("${pluginArtifactId}")
                }
            }
            gradle.settingsEvaluated { settings ->
                if (settings.pluginManager.hasPlugin('${plugin.id}')) {
                    logger.lifecycle("${plugin.id} is already applied")
                } else {
                    logger.lifecycle("Applying ${plugin.className} via init script")
                    settings.pluginManager.apply(initscript.classLoader.loadClass('${plugin.className}'))
                }
            }
        """

        when:
        succeeds "t", "--scan", "--init-script", initScript.absolutePath

        then:
        plugin.assertAutoApplied(output, true)
        outputContains("${plugin.id} is already applied")
    }

}
