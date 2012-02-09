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

import gw.lang.parser.IExpression;
import gw.lang.reflect.*;
import gw.lang.reflect.gs.IGosuProgram;
import gw.lang.reflect.gs.IProgramInstance;
import gw.lang.reflect.java.JavaTypes;
import gw.vark.annotations.Depends;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Class description...
 *
 * @author bchang
 */
public class ProjectHelper {

  public static void configureProject(Project project, AardvarkProgram gosuProgram, LinkedHashMap<String, TargetCall> targetCalls) throws BuildException {
    try
    {
      gosuProgram.maybeEvaluate();
      addTargets(project, gosuProgram, targetCalls);
    }
    catch( Exception e )
    {
      throw new BuildException(e);
    }
  }

  private static void addTargets( Project project, AardvarkProgram gosuProgram, LinkedHashMap<String, TargetCall> targetCalls )
  {
    List<Target> targets = new ArrayList<Target>(gosuProgram.getRuntimeGeneratedTargets());

    for ( final IMethodInfo methodInfo : gosuProgram.get().getTypeInfo().getMethods() )
    {
      if ( Aardvark.isTargetMethod(gosuProgram.get(), methodInfo) )
      {
        String rawTargetName = stripParens(methodInfo.getName());
        String hyphenatedTargetName = camelCaseToHyphenated(rawTargetName);

        TargetCall targetCall = targetCalls.get(rawTargetName);
        if (targetCall == null) {
          targetCall = targetCalls.get(hyphenatedTargetName);
        }

        AardvarkTarget target = new AardvarkTarget(methodInfo, gosuProgram.getProgramInstance(), targetCall);
        target.setProject( project );
        target.setName( hyphenatedTargetName );
        target.setDescription( methodInfo.getDescription() );

        IAnnotationInfo dependsAnnotation = methodInfo.getAnnotation( TypeSystem.get( Depends.class ) );
        if (dependsAnnotation != null) {
          Depends dependsAnnotationValue = (Depends) dependsAnnotation.getInstance();
          String[] dependencies = dependsAnnotationValue.value();
          for ( String dependencyTarget : dependencies ) {
            target.addDependency( camelCaseToHyphenated(dependencyTarget) );
          }
        }

        targets.add(target);

        if (!rawTargetName.equals(hyphenatedTargetName)) {
          Target camelcaseTarget = new Target();
          camelcaseTarget.setName(rawTargetName);
          camelcaseTarget.addDependency(hyphenatedTargetName);
          project.addTarget(camelcaseTarget);
        }
      }
    }

    for (Target target : targets) {
      project.addTarget(target);
    }
  }

  private static String stripParens(String str) {
    int openParenIdx = str.lastIndexOf("(");
    return str.substring(0, openParenIdx);
  }

  private static boolean hasUpperCase(String str) {
    for (int i = 0; i < str.length(); i++) {
      if (Character.isUpperCase(str.charAt(i))) {
        return true;
      }
    }
    return false;
  }

  static String camelCaseToHyphenated(String str) {
    if (hasUpperCase(str)) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < str.length(); i++) {
        char c = str.charAt(i);
        if (Character.isUpperCase(c)) {
          sb.append('-');
          sb.append(Character.toLowerCase(c));
        }
        else {
          sb.append(c);
        }
      }
      return sb.toString();
    }
    else {
      return str;
    }
  }

  private static class AardvarkTarget extends Target {
    private final IMethodInfo _methodInfo;
    private final IProgramInstance _gosuProgramInstance;
    private final TargetCall _targetCall;

    AardvarkTarget(IMethodInfo methodInfo, IProgramInstance gosuProgramInstance, TargetCall targetCall) {
      _methodInfo = methodInfo;
      _gosuProgramInstance = gosuProgramInstance;
      _targetCall = targetCall;
    }

    @Override
    public void execute() throws BuildException {
      int argArraySize = _methodInfo.getOwnersType() instanceof IGosuProgram ? 1 : 0;
      int offset = argArraySize;
      argArraySize += _methodInfo.getParameters().length;
      Object[] args = new Object[argArraySize];
      Map<String, String> userParams = _targetCall != null ? _targetCall.getParams() : Collections.<String, String>emptyMap();
      IParameterInfo[] parameters = _methodInfo.getParameters();
      for (int i = 0, parametersLength = parameters.length; i < parametersLength; i++) {
        IParameterInfo paramInfo = parameters[i];
        if (paramInfo.getFeatureType().equals(JavaTypes.STRING())) {
          args[offset + i] = determineStringParamVal(paramInfo.getName(), userParams, i);
        }
        else if (paramInfo.getFeatureType().equals(JavaTypes.pBOOLEAN()) || paramInfo.getFeatureType().equals(JavaTypes.BOOLEAN())) {
          args[offset + i] = determineBooleanParamVal(paramInfo.getName(), userParams, i);
        }
        else if (paramInfo.getFeatureType().equals(JavaTypes.pINT()) || paramInfo.getFeatureType().equals(JavaTypes.INTEGER())) {
          args[offset + i] = determineIntParamVal(paramInfo.getName(), userParams, i);
        }
        else {
          throw new IllegalArgumentException("type " + paramInfo.getFeatureType() + " for \"" + paramInfo.getName() + "\" not supported");
        }
      }
      if (userParams.size() > 0) {
        throw new IllegalArgumentException("no parameter named \"" + userParams.keySet().iterator().next() + "\"");
      }
      _methodInfo.getCallHandler().handleCall(_gosuProgramInstance, args);
    }

    private Object determineStringParamVal(String paramName, Map<String, String> userParams, int i) {
      boolean hasUserParam = userParams.containsKey(paramName);
      if (hasUserParam) {
        String userValue = userParams.remove(paramName);
        if (userValue == null) {
          throw new IllegalArgumentException("\"" + paramName + "\" is expected to be followed by a value");
        }
        return userValue;
      }
      else {
        IExpression defaultValue = ((IOptionalParamCapable)_methodInfo).getDefaultValueExpressions()[i];
        if (defaultValue == null) {
          throw new IllegalArgumentException("requires parameter \"" + paramName + "\"");
        }
        return defaultValue.evaluate();
      }
    }

    private Object determineBooleanParamVal(String paramName, Map<String, String> userParams, int i) {
      boolean hasUserParam = userParams.containsKey(paramName);
      if (hasUserParam) {
        String userValue = userParams.remove(paramName);
        if (userValue == null) {
          return true;
        }
        else if (userValue.equals("true")) {
          return Boolean.TRUE;
        }
        else if (userValue.equals("false")) {
          return Boolean.FALSE;
        }
        else {
          throw new IllegalArgumentException("\"" + paramName + "\" value is expected to be a boolean, was \"" + userValue + "\"");
        }
      }
      else {
        IExpression defaultValue = ((IOptionalParamCapable)_methodInfo).getDefaultValueExpressions()[i];
        if (defaultValue == null) {
          return false;
        }
        return defaultValue.evaluate();
      }
    }

    private Object determineIntParamVal(String paramName, Map<String, String> userParams, int i) {
      boolean hasUserParam = userParams.containsKey(paramName);
      if (hasUserParam) {
        String userValue = userParams.remove(paramName);
        if (userValue == null) {
          throw new IllegalArgumentException("\"" + paramName + "\" is expected to be followed by a value");
        }
        try {
          return new Integer(userValue);
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException("\"" + paramName + "\" value is expected to be an int, was \"" + userValue + "\"");
        }
      }
      else {
        IExpression defaultValue = ((IOptionalParamCapable)_methodInfo).getDefaultValueExpressions()[i];
        if (defaultValue == null) {
          throw new IllegalArgumentException("requires parameter \"" + paramName + "\"");
        }
        return defaultValue.evaluate();
      }
    }
  }
}
