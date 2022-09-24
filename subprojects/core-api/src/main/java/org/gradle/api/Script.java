/*
 * Copyright 2017 the original author or authors.
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
package org.gradle.api;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.SyncSpec;
import org.gradle.api.initialization.dsl.ScriptHandler;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.LoggingManager;
import org.gradle.api.plugins.ObjectConfigurationAction;
import org.gradle.api.provider.Provider;
import org.gradle.api.resources.ResourceHandler;
import org.gradle.api.tasks.WorkResult;
import org.gradle.process.ExecResult;
import org.gradle.process.ExecSpec;
import org.gradle.process.JavaExecSpec;

import java.io.File;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * <p>This interface is implemented by all Gradle Groovy DSL scripts to add in some Gradle-specific methods. As your compiled
 * script class will implement this interface, you can use the methods and properties declared by this interface
 * directly in your script.</p>
 *
 * <p>Generally, a {@code Script} object will have a delegate object attached to it. For example, a build script will
 * have a {@link Project} instance attached to it, and an initialization script will have a {@link
 * org.gradle.api.invocation.Gradle} instance attached to it. Any property reference or method call which is not found
 * on this {@code Script} object is forwarded to the delegate object.</p>
 */
public interface Script {

    /**
     * <p>Configures the delegate object for this script using plugins or scripts.
     *
     * <p>The given action is used to configure an {@link org.gradle.api.plugins.ObjectConfigurationAction} which is
     * then used to configure the delegate object.</p>
     *
     * @param action The action to configure the {@code ObjectConfigurationAction}.
     * @since 8.0
     */
    void apply(Action<? super ObjectConfigurationAction> action);

    /**
     * <p>Configures the delegate object for this script using plugins or scripts.
     *
     * <p>The following options are available:</p>
     *
     * <ul><li>{@code from}: A script to apply to the delegate object. Accepts any path supported by {@link
     * #uri(Object)}.</li>
     *
     * <li>{@code plugin}: The id or implementation class of the plugin to apply to the delegate object.</li>
     *
     * <li>{@code to}: The target delegate object or objects.</li></ul>
     *
     * <p>For more detail, see {@link org.gradle.api.plugins.ObjectConfigurationAction}.</p>
     *
     * @param options The options to use to configure the {@code ObjectConfigurationAction}.
     */
    void apply(Map<String, ?> options);

    /**
     * Returns the script handler for this script. You can use this handler to manage the classpath used to compile and
     * execute this script.
     *
     * @return the classpath handler. Never returns null.
     */
    ScriptHandler getBuildscript();

    /**
     * Configures the classpath for this script.
     *
     * <p>The given action is executed against this script's {@link ScriptHandler}. </p>
     *
     * @param action the action to use to configure the script classpath.
     * @since 8.0
     */
    void buildscript(Action<? super ScriptHandler> action);

    /**
     * <p>Resolves a file path relative to the directory containing this script. This works as described for {@link
     * Project#file(Object)}</p>
     *
     * @param path The object to resolve as a File.
     * @return The resolved file. Never returns null.
     */
    File file(Object path);

    /**
     * <p>Resolves a file path relative to the directory containing this script and validates it using the given scheme.
     * See {@link PathValidation} for the list of possible validations.</p>
     *
     * @param path An object to resolve as a File.
     * @param validation The validation to perform on the file.
     * @return The resolved file. Never returns null.
     * @throws InvalidUserDataException When the file does not meet the given validation constraint.
     */
    File file(Object path, PathValidation validation) throws InvalidUserDataException;

    /**
     * <p>Resolves a file path to a URI, relative to the directory containing this script. Evaluates the provided path
     * object as described for {@link #file(Object)}, with the exception that any URI scheme is supported, not just
     * 'file:' URIs.</p>
     *
     * @param path The object to resolve as a URI.
     * @return The resolved URI. Never returns null.
     */
    URI uri(Object path);

    /**
     * <p>Returns a {@link ConfigurableFileCollection} containing the given files. This works as described for {@link
     * Project#files(Object...)}. Relative paths are resolved relative to the directory containing this script.</p>
     *
     * @param paths The paths to the files. May be empty.
     * @return The file collection. Never returns null.
     */
    ConfigurableFileCollection files(Object... paths);

    /**
     * <p>Creates a new {@code ConfigurableFileCollection} using the given paths. The file collection is configured
     * using the given action. This method works as described for {@link Project#files(Object, Action)}.
     * Relative paths are resolved relative to the directory containing this script.</p>
     *
     * @param paths The contents of the file collection. Evaluated as per {@link #files(Object...)}.
     * @param action The action to use to configure the file collection.
     * @return the configured file tree. Never returns null.
     *
     * @see Project#files(Object, Action)
     * @since 8.0
     */
    ConfigurableFileCollection files(Object paths, Action<? super ConfigurableFileCollection> action);

    /**
     * <p>Returns the relative path from the directory containing this script to the given path. The given path object
     * is (logically) resolved as described for {@link #file(Object)}, from which a relative path is calculated.</p>
     *
     * @param path The path to convert to a relative path.
     * @return The relative path. Never returns null.
     */
    String relativePath(Object path);

    /**
     * <p>Creates a new {@code ConfigurableFileTree} using the given base directory. The given baseDir path is evaluated
     * as per {@link #file(Object)}.</p>
     *
     * <p>The returned file tree is lazy, so that it scans for files only when the contents of the file tree are
     * queried. The file tree is also live, so that it scans for files each time the contents of the file tree are
     * queried.</p>
     *
     * @param baseDir The base directory of the file tree. Evaluated as per {@link #file(Object)}.
     * @return the file tree. Never returns null.
     */
    ConfigurableFileTree fileTree(Object baseDir);

    /**
     * <p>Creates a new {@code ConfigurableFileTree} using the provided map of arguments.  The map will be applied as
     * properties on the new file tree.  Example:</p>
     * <pre>
     * fileTree(dir:'src', excludes:['**&#47;ignore/**','**&#47;.svn/**'])
     * </pre>
     * <p>The returned file tree is lazy, so that it scans for files only when the contents of the file tree are
     * queried. The file tree is also live, so that it scans for files each time the contents of the file tree are
     * queried.</p>
     *
     * @param args map of property assignments to {@code ConfigurableFileTree} object
     * @return the configured file tree. Never returns null.
     */
    ConfigurableFileTree fileTree(Map<String, ?> args);

    /**
     * <p>Creates a new {@code ConfigurableFileTree} using the given base directory. The given baseDir path is evaluated
     * as per {@link #file(Object)}. The action will be used to configure the new file tree.
     * Example:</p>
     *
     * <pre>
     * fileTree('src') {
     *    exclude '**&#47;.svn/**'
     * }.copy { into 'dest'}
     * </pre>
     *
     * <p>The returned file tree is lazy, so that it scans for files only when the contents of the file tree are
     * queried. The file tree is also live, so that it scans for files each time the contents of the file tree are
     * queried.</p>
     *
     * @param baseDir The base directory of the file tree. Evaluated as per {@link #file(Object)}.
     * @param action Action to configure the {@code ConfigurableFileTree} object.
     * @return the configured file tree. Never returns null.
     *
     * @see Project#fileTree(Object, Action)
     *
     * @since 8.0
     */
    ConfigurableFileTree fileTree(Object baseDir, Action<? super ConfigurableFileTree> action);

    /**
     * <p>Creates a new {@code FileTree} which contains the contents of the given ZIP file. The given zipPath path is
     * evaluated as per {@link #file(Object)}. You can combine this method with the {@link #copy(Action)}
     * method to unzip a ZIP file.</p>
     *
     * <p>The returned file tree is lazy, so that it scans for files only when the contents of the file tree are
     * queried. The file tree is also live, so that it scans for files each time the contents of the file tree are
     * queried.</p>
     *
     * @param zipPath The ZIP file. Evaluated as per {@link #file(Object)}.
     * @return the file tree. Never returns null.
     */
    FileTree zipTree(Object zipPath);

    /**
     * Creates a new {@code FileTree} which contains the contents of the given TAR file. The given tarPath path can be:
     * <ul>
     *   <li>an instance of {@link org.gradle.api.resources.Resource}</li>
     *   <li>any other object is evaluated as per {@link #file(Object)}</li>
     * </ul>
     *
     * The returned file tree is lazy, so that it scans for files only when the contents of the file tree are
     * queried. The file tree is also live, so that it scans for files each time the contents of the file tree are
     * queried.
     * <p>
     * Unless custom implementation of resources is passed, the tar tree attempts to guess the compression based on the file extension.
     * <p>
     * You can combine this method with the {@link #copy(Action)}
     * method to untar a TAR file:
     *
     * <pre class='autoTested'>
     * task untar(type: Copy) {
     *   from tarTree('someCompressedTar.gzip')
     *
     *   //tar tree attempts to guess the compression based on the file extension
     *   //however if you must specify the compression explicitly you can:
     *   from tarTree(resources.gzip('someTar.ext'))
     *
     *   //in case you work with unconventionally compressed tars
     *   //you can provide your own implementation of a ReadableResource:
     *   //from tarTree(yourOwnResource as ReadableResource)
     *
     *   into 'dest'
     * }
     * </pre>
     *
     * @param tarPath The TAR file or an instance of {@link org.gradle.api.resources.Resource}.
     * @return the file tree. Never returns null.
     */
    FileTree tarTree(Object tarPath);

    /**
     * Copy the specified files.  The given action is used to configure a {@link org.gradle.api.file.CopySpec}, which
     * is then used to copy the files. Example:
     * <pre>
     * copy {
     *    from configurations.runtimeClasspath
     *    into 'build/deploy/lib'
     * }
     * </pre>
     * Note that CopySpecs can be nested:
     * <pre>
     * copy {
     *    into 'build/webroot'
     *    exclude '**&#47;.svn/**'
     *    from('src/main/webapp') {
     *       include '**&#47;*.jsp'
     *       filter(ReplaceTokens, tokens:[copyright:'2009', version:'2.3.1'])
     *    }
     *    from('src/main/js') {
     *       include '**&#47;*.js'
     *    }
     * }
     * </pre>
     *
     * @param action Action to configure the CopySpec
     * @return {@link org.gradle.api.tasks.WorkResult} that can be used to check if the copy did any work.
     *
     * @see Project#copy(Action)
     *
     * @since 8.0
     */
    WorkResult copy(Action<? super CopySpec> action);

    /**
     * Synchronizes the contents of a destination directory with some source directories and files.
     * The given action is used to configure a {@link SyncSpec}, which is then used to synchronize the files.
     *
     * @param action Action to configure the SyncSpec
     * @return {@link org.gradle.api.tasks.WorkResult} that can be used to check if the sync did any work.
     *
     * @see Project#sync(Action)
     *
     * @since 8.0
     */
    WorkResult sync(Action<? super SyncSpec> action);

    /**
     * Creates a {@link org.gradle.api.file.CopySpec} which can later be used to copy files or create an archive. The
     * given action is used to configure the {@link org.gradle.api.file.CopySpec} before it is returned by this
     * method.
     *
     * @param action Action to configure the CopySpec
     * @return The CopySpec
     *
     * @since 8.0
     * @see Project#copySpec(Action)
     */
    CopySpec copySpec(Action<? super CopySpec> action);

    /**
     * Creates a directory and returns a file pointing to it.
     *
     * @param path The path for the directory to be created. Evaluated as per {@link #file(Object)}.
     * @return the created directory
     * @throws org.gradle.api.InvalidUserDataException If the path points to an existing file.
     */
    File mkdir(Object path);

    /**
     * Deletes files and directories.
     *
     * @param paths Any type of object accepted by {@link org.gradle.api.Project#files(Object...)}
     * @return true if anything got deleted, false otherwise
     */
    boolean delete(Object... paths);

    /**
     * Executes a Java main class.
     *
     * @param action The action for configuring the execution.
     * @return the result of the execution
     */
    ExecResult javaexec(Action<? super JavaExecSpec> action);

    /**
     * Executes an external command.
     *
     * @param action The action for configuring the execution.
     * @return the result of the execution
     */
    ExecResult exec(Action<? super ExecSpec> action);

    /**
     * Returns the {@link org.gradle.api.logging.LoggingManager} which can be used to receive logging and to control the
     * standard output/error capture for this script. By default, System.out is redirected to the Gradle logging system
     * at the QUIET log level, and System.err is redirected at the ERROR log level.
     *
     * @return the LoggingManager. Never returns null.
     */
    LoggingManager getLogging();

    /**
     * Returns the logger for this script. You can use this in your script to write log messages.
     *
     * @return The logger. Never returns null.
     */
    Logger getLogger();

    /**
     * Provides access to resource-specific utility methods, for example factory methods that create various resources.
     *
     * @return Returned instance contains various resource-specific utility methods.
     */
    ResourceHandler getResources();

    /**
     * Creates a {@code Provider} implementation based on the provided value.
     *
     * @param value The {@code java.util.concurrent.Callable} use to calculate the value.
     * @return The provider. Never returns null.
     * @throws org.gradle.api.InvalidUserDataException If the provided value is null.
     * @see org.gradle.api.provider.ProviderFactory#provider(Callable)
     * @since 4.0
     */
    <T> Provider<T> provider(Callable<T> value);
}
