package weka;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;         

/**
 * Test class for all tests in this directory. Run from the command line 
 * with:<p>
 * java weka.AllTests
 *
 * @author <a href="mailto:len@webmind.com">Len Trigg</a>
 * @version $Revision: 1.1 $
 */
public class AllTests extends TestSuite {

  public static Test suite() {
    TestSuite suite = new TestSuite();

    // Core components
//      suite.addTest(weka.core.AllTests.suite());
//      suite.addTest(weka.estimators.AllTests.suite());
    suite.addTest(weka.filters.AllTests.suite());


    // The main ML components
//      suite.addTest(weka.associations.AllTests.suite());
//      suite.addTest(weka.attributeSelection.AllTests.suite());
    suite.addTest(weka.classifiers.AllTests.suite());
//      suite.addTest(weka.clusterers.AllTests.suite());


    // High level applications
//      suite.addTest(weka.experiment.AllTests.suite());
//      suite.addTest(weka.gui.AllTests.suite());

    return suite;
  }

  public static void main(String []args) {
    junit.textui.TestRunner.run(suite());
  }
}
