/*
 * Copyright 2012 the original author or authors.
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

package org.gradle.integtests.tooling

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.tooling.fixture.ToolingApiDistribution
import org.gradle.integtests.tooling.fixture.ToolingApiDistributionResolver

class ToolingApiClasspathIntegrationTest extends AbstractIntegrationSpec {

    def "tooling api classpath contains only tooling-api jar and slf4j"() {
        when:
        ToolingApiDistributionResolver resolver = new ToolingApiDistributionResolver().withDefaultRepository().withExternalToolingApiDistribution()
        ToolingApiDistribution resolve = resolver.resolve(distribution.getVersion().version)

        then:
        resolve.classpath.size() == 2
        resolve.classpath.any {it.name ==~ /slf4j-api-.*\.jar/}
        resolve.classpath.find { it.name ==~ /gradle-tooling-api.*\.jar/ }.size() < 1.88 * 1024 * 1024

        cleanup:
        resolver.stop()
    }
}
