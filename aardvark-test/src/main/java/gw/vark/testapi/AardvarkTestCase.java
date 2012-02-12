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

package gw.vark.testapi;

import junit.framework.TestCase;
import org.fest.assertions.Assertions;
import org.fest.assertions.ListAssert;
import org.fest.assertions.ObjectAssert;
import org.fest.assertions.StringAssert;

import java.util.List;

/**
 */
public abstract class AardvarkTestCase extends TestCase {

  public static StringAssert assertThat(String actual) {
    return Assertions.assertThat(actual);
  }

  public static ListAssert assertThat(List<?> actual) {
    return Assertions.assertThat(actual);
  }

  public static ObjectAssert assertThat(Object actual) {
    return Assertions.assertThat(actual);
  }
}
