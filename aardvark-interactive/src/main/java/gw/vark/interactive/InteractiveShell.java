/*
 * Copyright (c) 2011 Guidewire Software, Inc.
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

package gw.vark.interactive;

import gw.lang.launch.ArgInfo;
import gw.lang.parser.exceptions.ParseResultsException;
import gw.util.GosuExceptionUtil;
import gw.vark.Aardvark;
import gw.vark.AardvarkOptions;
import jline.ConsoleReader;
import jline.Terminal;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * Interactive vark shell.
 */
public class InteractiveShell {

  private static final String VARK_PROMPT = "vark> ";
  private final ReloadManager _reloadManager;

  InteractiveShell(ArgInfo.IProgramSource programSource) {
    _reloadManager = new ReloadManager(programSource);
  }

  public void run() {
    try {
      Runtime.getRuntime().addShutdownHook(new Thread() {
        public void run()
        {
          System.out.println();
          System.out.flush();
        }
      });

      Terminal.setupTerminal();
      ConsoleReader console = new ConsoleReader();
      console.setDefaultPrompt(VARK_PROMPT);

      while (true) {
        String command = console.readLine();

        if (command == null || command.equals("quit") || command.equals("exit")) {
          break;
        }
        else if (command.isEmpty()) {
          continue;
        }

        try {
          _reloadManager.detectAndReloadChangedResources();
          AardvarkOptions options = new AardvarkOptions(command.split("\\s"));
          try {
            _reloadManager.getAardvarkProject().runBuild(options.getTargetCalls(), false);
          }
          catch (BuildException e) {
            Aardvark.getProject().log(e.getMessage(), Project.MSG_ERR);
          }
        } catch (ParseResultsException e) {
          Aardvark.getProject().log(e.getFeedback(), Project.MSG_ERR);
        }
      }
    }
    catch (Exception e) {
      throw GosuExceptionUtil.forceThrow(e);
    }
  }
}
