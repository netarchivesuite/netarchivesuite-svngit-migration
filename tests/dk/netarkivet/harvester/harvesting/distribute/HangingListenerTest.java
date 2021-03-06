/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package dk.netarkivet.harvester.harvesting.distribute;

import com.sun.messaging.QueueConnectionFactory;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.ChannelsTester;
import dk.netarkivet.common.distribute.JMSConnectionSunMQ;
import dk.netarkivet.common.utils.RememberNotifications;
import dk.netarkivet.common.utils.Settings;
import junit.framework.TestCase;

import javax.jms.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A test of the behaviour if onMessage() hangs when there is more than one
 * listener to a queue.
 */
public class HangingListenerTest extends TestCase {

    public static AtomicInteger messages_received = new AtomicInteger(0);

    public void setUp(){
        Settings.set(CommonSettings.JMS_BROKER_CLASS, JMSConnectionSunMQ.class.getName());
        //JMSConnection.getInstance();
        ChannelsTester.resetChannels();
        /* Do not send notification by email. Print them to STDOUT. */
        Settings.set(CommonSettings.NOTIFICATIONS_CLASS, 
                RememberNotifications.class.getName());
    }

    public void tearDown() {
        //JMSConnection.getInstance().cleanup();
    }

    /**
     * Tests what happens if we have a blocking listener. It appears that by
     * default, a listener may pre-queue messages even while it is blocked
     * processing the previous message. This is not desirable behaviour.
     * @throws InterruptedException
     * @throws JMSException
     */
// Out commented to avoid reference to archive module from harvester module.
//    public void testNotListeningWhileProcessingSunMQ() throws InterruptedException, JMSException {
//        if (!Settings.get(CommonSettings.JMS_BROKER_CLASS).equals(JMSConnectionSunMQ.class.getName())) {
//            fail("Wrong message queue for test");
//        }
//        JMSConnectionFactory.getInstance().cleanup();
//        JMSConnectionFactory.getInstance();
//        long blockingTime = 1000l;
//        int messagesSent = 10;
//        BlockingListener nonBlocker = new BlockingListener();
//        BlockingListener blocker = new BlockingListener(true, blockingTime);
//        ChannelID theQueue = Channels.getTheBamon();
//        JMSConnection con = JMSConnectionFactory.getInstance();
//        MiniConnectionSunMQ con2 = new MiniConnectionSunMQ();
//        // Set the production JMS connection to listen with the blocking
//        // listener
//        con.setListener(theQueue, blocker);
//        con2.setListener(theQueue, nonBlocker);
//        for (int i = 0; i < messagesSent; i++) {
//            NetarkivetMessage msg = new BatchMessage(theQueue, new ChecksumJob(), "ONE");
//            con.send(msg);
//        }
//        while(HangingListenerTest.messages_received.get() < messagesSent) {}
//        Thread.sleep(2*blockingTime);
//        assertEquals("Blocking listener should only have been called once", 1, blocker.called);
//        System.out.println("Repeat:");
//        for (int i = 0; i < messagesSent; i++) {
//            NetarkivetMessage msg = new BatchMessage(theQueue, new ChecksumJob(), "ONE");
//            con.send(msg);
//        }
//        while(HangingListenerTest.messages_received.get() < messagesSent) {}
//        Thread.sleep(2*blockingTime);
//        assertEquals("Blocking listener should now have been called twice", 2, blocker.called);
//        con.cleanup();
//        con2.cleanup();
//    }


    public static class MiniConnectionSunMQ {

        QueueSession myQSess;
        QueueConnection myQConn;

        public MiniConnectionSunMQ() throws JMSException {
            String host = Settings.get(JMSConnectionSunMQ.JMS_BROKER_HOST);
            String port = Settings.get(JMSConnectionSunMQ.JMS_BROKER_PORT);
            QueueConnectionFactory cFactory = new QueueConnectionFactory();
            ((com.sun.messaging.ConnectionFactory) cFactory).setProperty(
                    com.sun.messaging.ConnectionConfiguration.imqBrokerHostName, host);
            ((com.sun.messaging.ConnectionFactory) cFactory).setProperty(
                    com.sun.messaging.ConnectionConfiguration.imqBrokerHostPort, String.valueOf(port));
            ((com.sun.messaging.ConnectionFactory) cFactory).setProperty(
                    com.sun.messaging.ConnectionConfiguration.imqConsumerFlowLimit, "1");
            myQConn = cFactory.createQueueConnection();
            myQSess = myQConn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            myQConn.start();
        }

        public void setListener(ChannelID mq, MessageListener ml) throws JMSException {
            Queue queue = new com.sun.messaging.Queue(mq.getName());
            QueueReceiver myQueueReceiver = myQSess.createReceiver(queue);
            myQueueReceiver.setMessageListener(ml);
        }

        public void cleanup() throws JMSException {
            myQConn.close();
        }
    }

    public static class BlockingListener implements MessageListener {

        public int called = 0;
        long timeToBlockMS;
        boolean isBlocking;

        public BlockingListener() {
            this(false, 0);
        }

        public BlockingListener(boolean block, long timeToBlockMS) {
            this.timeToBlockMS = timeToBlockMS;
            isBlocking = block;
        }

        public void onMessage(Message message) {
            called++;
            HangingListenerTest.messages_received.addAndGet(1);
            if (!isBlocking) {
                System.out.println("Message received by non-blocking listener at " + System.currentTimeMillis());
                return;
            }
            System.out.println("Message received by blocking listener at " + System.currentTimeMillis());
            try {
                Thread.sleep(timeToBlockMS);
            } catch (InterruptedException e) {
                //Expected ??
            }
            System.out.println("Blocking listener returned at " + System.currentTimeMillis());
        }
    }

}
