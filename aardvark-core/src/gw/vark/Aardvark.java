/*
 * Copyright (c) 2010 Guidewire Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gw.vark;

import gw.lang.Gosu;
import gw.lang.mode.GosuMode;
import gw.lang.mode.RequiresInit;
import gw.lang.parser.exceptions.ParseResultsException;
import gw.lang.reflect.IMethodInfo;
import gw.lang.reflect.IType;
import gw.lang.reflect.TypeSystem;
import gw.util.GosuExceptionUtil;
import gw.util.StreamUtil;
import gw.vark.typeloader.AntlibTypeLoader;
import org.apache.tools.ant.*;
import org.apache.tools.ant.util.ClasspathUtils;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;

// TODO - gosu - expose system properties from ArgInfo?
// TODO - gosu - better help support
// TODO - gosu - pass in a default program source from gosulaunch.properties
// TODO - gosu - a way for us to add tools.jar into the bootstrap classpath
// TODO - find way to set default vark file if none is given at command line
// TODO - test that the project base dir is right if we're using a URL-based program source
@RequiresInit
public class Aardvark extends GosuMode
{
  public static final int GOSU_MODE_PRIORITY_AARDVARK_HELP = 0;
  public static final int GOSU_MODE_PRIORITY_AARDVARK_VERSION = 1;
  public static final int GOSU_MODE_PRIORITY_AARDVARK_INTERACTIVE = 2;
  public static final int GOSU_MODE_PRIORITY_AARDVARK = 3;

  private static final String DEFAULT_BUILD_FILE_NAME = "build.vark";
  private static AardvarkProgram _aardvarkProjectInstance;

  static final int EXITCODE_VARKFILE_NOT_FOUND = 4;
  static final int EXITCODE_GOSU_VERIFY_FAILED = 8;

  private static String RAW_VARK_FILE_PATH = "";

  public static Project getProject() {
    if (_aardvarkProjectInstance == null) {
      throw new IllegalStateException("no current Aardvark project instance");
    }
    return _aardvarkProjectInstance.getProject();
  }

  public static String getRawVarkFilePath() {
    return RAW_VARK_FILE_PATH;
  }

  private BuildLogger _logger;

  // this is a convenience when working in a dev environment when we might not want to use the Launcher
  public static void main( String... args ) throws Exception {
    Gosu.main(args);
  }

  public Aardvark() {
    this(new DefaultLogger());
  }

  Aardvark(BuildLogger logger) {
    logger.setMessageOutputLevel( Project.MSG_INFO );
    logger.setOutputPrintStream(System.out);
    logger.setErrorPrintStream(System.err);
    _logger = logger;
  }

  @Override
  public int getPriority() {
    return GOSU_MODE_PRIORITY_AARDVARK;
  }

  @Override
  public boolean accept() {
    return true;
  }

  @Override
  public int run() throws Exception {
    RAW_VARK_FILE_PATH = _argInfo.getProgramSource().getValue();

    AardvarkOptions options = new AardvarkOptions(_argInfo);
    File varkFile;
    AardvarkProgram aardvarkProject;

    if (options.getLogger() != null) {
      newLogger(options.getLogger());
    }
    _logger.setMessageOutputLevel(options.getLogLevel().getLevel());

    if ("true".equals(System.getProperty("aardvark.dev"))) {
      log("aardvark.dev is on");
      AntlibTypeLoader loader = new AntlibTypeLoader(TypeSystem.getCurrentModule());
      TypeSystem.pushTypeLoader(loader);
    }

    varkFile = _argInfo.getProgramSource().getFile();
    log("Buildfile: " + varkFile);

      try {
        aardvarkProject = AardvarkProgram.parseWithTimer(_argInfo.getProgramSource());
        aardvarkProject.getProject().addBuildListener(_logger);
        _aardvarkProjectInstance = aardvarkProject;
      }
      catch (ParseResultsException e) {
        logErr(e.getMessage());
        return EXITCODE_GOSU_VERIFY_FAILED;
      }

      int exitCode = 1;
      try {
        try {
          aardvarkProject.runBuild(varkFile, options.getTargetCalls(), options.isHelp());
          exitCode = 0;
        } catch (ExitStatusException ese) {
          exitCode = ese.getStatus();
          if (exitCode != 0) {
            throw ese;
          }
        }
      } catch (BuildException e) {
        //printMessage(e); // (logger should have displayed the message along with "BUILD FAILED"
      } catch (Throwable e) {
        e.printStackTrace();
        printMessage(e);
      }
      return exitCode;
  }

/*
  public void resetProject(BuildLogger logger) {
    _project = new Project();
    setLogger(logger != null ? logger : _logger);
    setProject(_project);
  }
*/

  private void printMessage(Throwable t) {
    String message = t.getMessage();
    if (message != null) {
      logErr(message);
    }
  }

  public static boolean isTargetMethod(IType gosuProgram, IMethodInfo methodInfo) {
    return methodInfo.isPublic()
            && (methodInfo.hasAnnotation(TypeSystem.get(gw.vark.annotations.Target.class))
                    || (methodInfo.getParameters().length == 0 && methodInfo.getOwnersType().equals( gosuProgram )));
  }

  private BuildLogger newLogger(String loggerClassName) {
    try {
      return (BuildLogger) ClasspathUtils.newInstance(loggerClassName, Aardvark.class.getClassLoader(), BuildLogger.class);
    }
    catch (BuildException e) {
      logErr("The specified logger class " + loggerClassName + " could not be used because " + e.getMessage());
      throw e;
    }
  }

  private void log(String message) {
    getProject().log(message);
  }

  private void logVerbose(String message) {
    getProject().log(message, Project.MSG_VERBOSE);
  }

  private void logWarn(String message) {
    getProject().log(message, Project.MSG_WARN);
  }

  private void logErr(String message) {
    getProject().log(message, Project.MSG_ERR);
  }

  public static String getVersion() {
    URL versionResource = Aardvark.class.getResource("/gw/vark/version.txt");
    try {
      Reader reader = StreamUtil.getInputStreamReader(versionResource.openStream());
      String version = StreamUtil.getContent(reader).trim();
      return "Aardvark version " + version;
    } catch (IOException e) {
      throw GosuExceptionUtil.forceThrow(e);
    }
  }
}
