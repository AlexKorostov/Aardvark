package gw.vark;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        AardvarkBootstrapTest.class,
        AardvarkOptionsTest.class,
        AardvarkProcessTest.class,
        AardvarkShellTest.class,
        TestprojectTest.class
})
public class AardvarkSuite {
}
