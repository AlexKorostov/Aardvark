package gw.vark;

import gw.util.ShellProcess;
import gw.util.StreamUtil;
import gw.vark.it.ForkedAardvarkProcess;
import gw.vark.testapi.AardvarkAssertions;
import gw.vark.testapi.ForkedAardvarkProcess;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 */
public class InteractiveShellTest extends AardvarkAssertions {

  private static File _testDir;
  private static File _varkFile;
  private static File _userClass;
  private static ShellProcess _proc;
  private static long _mockFSClock = 0;

  public static final String LINE_SEPARATOR = System.getProperty("line.separator");
  private static final String VARK_FILE_0 = "" +
          "classpath \".\"" + LINE_SEPARATOR +
          "uses java.lang.System\n" +
          "gw.vark.Aardvark.getProject().registerTarget(\"@runtime-generated\", null)\n" +
          "var startupTime = System.nanoTime()\n" +
          "@Depends(\"@runtime-generated\")\n" +
          "function hello() {\n" +
          "  print(\"Hello World\")\n" +
          "}\n" +
          "function showStartupTime() {\n" +
          "  log(startupTime)\n" +
          "}\n";
  private static final String VARK_FILE_1 = "" +
          "classpath \".\"\n" +
          "uses testpackage.UserClass\n" +
          "uses java.lang.System\n" +
          "gw.vark.Aardvark.getProject().registerTarget(\"@runtime-generated\", null)\n" +
          "var startupTime = System.nanoTime()\n" +
          "@Depends(\"@runtime-generated\")\n" +
          "function hello() {\n" +
          "  print(UserClass.getFoo())\n" +
          "}\n" +
          "function showStartupTime() {\n" +
          "  log(startupTime)\n" +
          "}\n";
  private static final String USER_CLASS_0 = "" +
          "package testpackage\n" +
          "class UserClass {\n" +
          "  static function getFoo() : String {\n" +
          "    return \"Hello World 2\"\n" +
          "  }\n" +
          "}\n";
  private static final String USER_CLASS_1 = "" +
          "package testpackage\n" +
          "class UserClass {\n" +
          "  static function getFoo() : String {\n" +
          "    return \"Hello World 3\"\n" +
          "  }\n" +
          "}\n";

  @BeforeClass
  public static void setUp() throws Exception {
    File tmpDir = new File(System.getProperty("java.io.tmpdir"));
    _testDir = new File(tmpDir, InteractiveShellTest.class.getSimpleName());
    deleteRecursively(_testDir);

    _testDir.mkdir();
    _varkFile = new File(_testDir, Aardvark.DEFAULT_BUILD_FILE_NAME);
    writeToFile(_varkFile, VARK_FILE_0);

    File packageDir = new File(_testDir, "testpackage");
    packageDir.mkdir();
    _userClass = new File(packageDir, "UserClass.gs");
    writeToFile(_userClass, USER_CLASS_0);

    Process process = new ForkedAardvarkProcess(_varkFile)
            .withArgs("-i")
            .withAdditionalClasspathElement(_testDir.getPath())
            .build();
    _proc = new ShellProcess(process);
    readFromProcess();
  }

  @AfterClass
  public static void tearDown() throws Exception {
    writeToProcessAndRead("quit" + LINE_SEPARATOR);
    deleteRecursively(_testDir);
  }

  public static long advanceAndGetMockFSClock() {
    _mockFSClock += 1000;
    return _mockFSClock;
  }

  @Test
  public void testHello() throws Exception {
    writeToFile(_varkFile, VARK_FILE_0);
    writeToFile(_userClass, USER_CLASS_0);
    String read = writeToProcessAndRead("hello" + LINE_SEPARATOR);
    assertThat(read).startsWith("hello\n\n@runtime-generated:\n\nhello:\nHello World\n\nBUILD SUCCESSFUL\nTotal time: ");
  }

  @Test
  public void testBlankLine() throws Exception {
    writeToFile(_varkFile, VARK_FILE_0);
    writeToFile(_userClass, USER_CLASS_0);
    String read = writeToProcessAndRead(LINE_SEPARATOR);
    assertThat(read).isEqualTo(LINE_SEPARATOR);
  }

  @Test
  public void testRefreshOnVarkFile() throws Exception {
    writeToFile(_varkFile, VARK_FILE_1);
    writeToFile(_userClass, USER_CLASS_0);
    String read = writeToProcessAndRead("hello" + LINE_SEPARATOR);
    assertThat(read).startsWith("hello\n\n@runtime-generated:\n\nhello:\nHello World 2\n\nBUILD SUCCESSFUL\nTotal time: ");
  }

  @Test
  public void testRefreshOnUserClass() throws Exception {
    writeToFile(_varkFile, VARK_FILE_1);
    writeToFile(_userClass, USER_CLASS_1);
    String read = writeToProcessAndRead("hello" + LINE_SEPARATOR);
    assertThat(read).startsWith("hello\n\n@runtime-generated:\n\nhello:\nHello World 3\n\nBUILD SUCCESSFUL\nTotal time: ");
  }

  @Test
  public void testNewProgramInstanceWhenAndOnlyWhenVarkFileIsRefreshed() throws Exception {
    Pattern pattern = Pattern.compile("show-startup-time  show-startup-time: (\\d+)  BUILD SUCCESSFUL.*");
    writeToFile(_varkFile, VARK_FILE_0);
    writeToFile(_userClass, USER_CLASS_0);

    String read = writeToProcessAndRead("show-startup-time" + LINE_SEPARATOR).replace(LINE_SEPARATOR, " ");
    assertThat(read).matches(pattern.pattern());
    Matcher matcher = pattern.matcher(read);
    assertThat(matcher.matches()).isTrue();
    long time = Long.parseLong(matcher.group(1));

    read = writeToProcessAndRead("show-startup-time" + LINE_SEPARATOR).replace(LINE_SEPARATOR, " ");
    assertThat(read).matches(pattern.pattern());
    matcher = pattern.matcher(read);
    assertThat(matcher.matches()).isTrue();
    long time2 = Long.parseLong(matcher.group(1));
    assertThat(time2).isEqualTo(time);

    writeToFile(_varkFile, VARK_FILE_1);
    read = writeToProcessAndRead("show-startup-time" + LINE_SEPARATOR).replace(LINE_SEPARATOR, " ");
    assertThat(read).matches(pattern.pattern());
    matcher = pattern.matcher(read);
    assertThat(matcher.matches()).isTrue();
    time2 = Long.parseLong(matcher.group(1));
    assertThat(time2).isNotEqualTo(time);
  }

  private static String writeToProcessAndRead(String write) {
    writeToProcess(write);
    return readFromProcess();
  }

  private static void writeToProcess(String write) {
    _proc.write(write);
    System.out.print(write);
  }

  private static String readFromProcess() {
    String read = _proc.readUntil("vark> ", false);
    System.out.print(read);
    return read;
  }

  private static void writeToFile(File file, String content) throws IOException {
    BufferedWriter writer = null;
    try {
      writer = new BufferedWriter(new FileWriter(file));
      writer.write(content);
    } finally {
      try {
        StreamUtil.close(writer);
      }
      catch (IOException closeException) {
        closeException.printStackTrace();
      }
    }
    file.setLastModified(advanceAndGetMockFSClock());
  }

  private static void deleteRecursively(File file) {
    if (file.isDirectory()) {
      for (File child : file.listFiles()) {
        deleteRecursively(child);
      }
    }
    file.delete();
  }
}
