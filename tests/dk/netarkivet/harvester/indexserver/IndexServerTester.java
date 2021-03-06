package dk.netarkivet.harvester.indexserver;

import dk.netarkivet.common.distribute.ChannelsTester;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.harvester.indexserver.IndexServerApplication;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.preconfigured.PreserveStdStreams;
import dk.netarkivet.testutils.preconfigured.PreventSystemExit;
import junit.framework.TestCase;

public class IndexServerTester extends TestCase {
    
    
    public void setUp() {
        ChannelsTester.resetChannels();
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
    }
    
    public void tearDown() {
        JMSConnectionMockupMQ.clearTestQueues();
        ChannelsTester.resetChannels();
    }

    /**
     * Ensure, that the application dies if given the wrong input.
     */
    public void testApplication() {
        ReflectUtils.testUtilityConstructor(IndexServerApplication.class);

        PreventSystemExit pse = new PreventSystemExit();
        PreserveStdStreams pss = new PreserveStdStreams(true);
        pse.setUp();
        pss.setUp();
        
        try {
            IndexServerApplication.main(new String[]{"ERROR"});
            fail("It should throw an exception ");
        } catch (SecurityException e) {
            // expected !
        }

        pss.tearDown();
        pse.tearDown();
        
        assertEquals("Should give exit code 1", 1, pse.getExitValue());
        assertTrue("Should tell that no arguments are expected.", 
                pss.getOut().contains("This application takes no arguments"));
    }
        
//    /**
//     * Test the basic class.
//     * TODO IT DOES SOMETHING SO THE PREVIOUS UNIT TESTS DOES NOT WORK 
//     */
//    public void testIndexServer() {
//        IndexServer.getInstance().cleanup();
//    }
}
