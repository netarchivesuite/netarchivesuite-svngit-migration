/* $Id$
 * $Date$
 * $Revision$
 * $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.common.distribute;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.QueueReceiver;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;

import com.sun.messaging.ConnectionConfiguration;
import com.sun.messaging.ConnectionFactory;
import com.sun.messaging.QueueConnectionFactory;
import com.sun.messaging.TopicConnectionFactory;


import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.UnknownID;


/**
 * Handles the communication with a Sun JMS broker.
 *
 * Note on Thread-safety: the methods and fields of JMSConnection are
 * not accessed by multiple threads (though JMSConnection itself creates
 * threads).
 * Thus no synchronization is needed on methods and fields of JMSConnection.
 * A shutdown hook is also added, which closes the connection.
 *
 */
public class JMSConnectionSunMQ extends JMSConnection {
    /** Singleton pattern is be used for this class. This is the one instance. */
    protected static JMSConnectionSunMQ instance = null;
       
    /** The errorcode for failure of the JMSbroker to acknowledge a message. */
    final static String PACKET_ACK_FAILED = "C4000";
    /** The errorcode signifying that the current session to the JMSbroker 
     * has been closed by the jmsbroker. 
     * One of the reasons: that the JMSbroker has been shutdown previously.
     */
    final static String SESSION_IS_CLOSED = "C4059";

    /**
     * The errorcode signifying that the JMSbroker 
     * has been shutdown. This errorcode is issued by the JMS-client.
     */
    final static String RECEIVED_GOODBYE_FROM_BROKER = "C4056";

    /**
     * Constructor.
     */
    private JMSConnectionSunMQ() {
        super();
        log.info("Creating instance of " + getClass().getName());
        initConnection(true);
    }

    /**
     * Intialises a Open Message Queue JMS connection.
     *
     * @return A JMSConnection
     * @throws IOFailure when connection to JMS broker failed
     */
    public static synchronized JMSConnectionSunMQ getInstance() throws UnknownID, IOFailure {
        if (instance == null) {
            instance = new JMSConnectionSunMQ();
        }
        return instance;
    }

    /**
     * Returns a new QueueConnectionFactory. This is an SunMQ
     * implementation of QueueConnectionFactory.
     *
     * Notice: The return type is explicitly defined with package prefix to
     * avoid name collision with javax.jms.QueueConnectionFactory
     *
     * @throws JMSException
     * @return QueueConnectionFactory
     */
    protected QueueConnectionFactory getQueueConnectionFactory()
    throws JMSException {
        QueueConnectionFactory cFactory = new QueueConnectionFactory();
        ((ConnectionFactory) cFactory).setProperty(
                ConnectionConfiguration.imqBrokerHostName,
                getHost());
        ((ConnectionFactory) cFactory).setProperty(
                ConnectionConfiguration.imqBrokerHostPort,
                String.valueOf(getPort()));
        ((ConnectionFactory) cFactory).setProperty(
                ConnectionConfiguration.imqConsumerFlowLimit,
                "1");
        /*((com.sun.messaging.ConnectionFactory) cFactory).setProperty(com.sun.messaging.ConnectionConfiguration.imqReconnectEnabled, "true");
        ((com.sun.messaging.ConnectionFactory) cFactory).setProperty(com.sun.messaging.ConnectionConfiguration.imqReconnectAttempts, "2");
        ((com.sun.messaging.ConnectionFactory) cFactory).setProperty(com.sun.messaging.ConnectionConfiguration.imqReconnectInterval, "10");*/
        return cFactory;
    }

    /**
     * Returns a new TopicConnectionFactory. This is an SunMQ
     * implementation of TopicConnectionFactory.
     *
     * Notice: The return type is explicitly defined with package prefix to
     * avoid name collision with javax.jms.TopicConnectionFactory
     *
     * @throws JMSException
     * @return TopicConnectionFactory
     */
    protected TopicConnectionFactory getTopicConnectionFactory() throws JMSException {
        TopicConnectionFactory cFactory = new TopicConnectionFactory();
        ((ConnectionFactory) cFactory).setProperty(
                ConnectionConfiguration.imqBrokerHostName,
                getHost());
        ((ConnectionFactory) cFactory).setProperty(
                ConnectionConfiguration.imqBrokerHostPort,
                String.valueOf(getPort()));
        ((ConnectionFactory) cFactory).setProperty(
                ConnectionConfiguration.imqConsumerFlowLimit,
                "1");
        /*((com.sun.messaging.ConnectionFactory) cFactory).setProperty(com.sun.messaging.ConnectionConfiguration.imqReconnectEnabled, "true");
        ((com.sun.messaging.ConnectionFactory) cFactory).setProperty(com.sun.messaging.ConnectionConfiguration.imqReconnectAttempts, "2");
        ((com.sun.messaging.ConnectionFactory) cFactory).setProperty(com.sun.messaging.ConnectionConfiguration.imqReconnectInterval, "10");*/

        return cFactory;
    }

    /**
     * Returns an Queue. This is an SunMQ implementation of Queue.
     * If no queue exists a new one will be created.
     *
     * @param queueName the name of the queue.
     * @throws JMSException
     * @return Queue
     */
    protected Queue getQueue(String queueName) throws JMSException {
        return new com.sun.messaging.Queue(queueName);
    }

    /**
     * Returns an Topic. This is an SunMQ implementation of Topic.
     * If no topic exists a new one will be created.
     *
     * @param topicName the name of the topic.
     * @throws JMSException
     * @return Topic
     */
    protected Topic getTopic(String topicName) throws JMSException {
        return new com.sun.messaging.Topic(topicName);
    }

    /**
     * Close the connection and reset the singleton
     */
    public void cleanup() {
        synchronized (JMSConnectionSunMQ.class) {
            instance = null;
            super.cleanup();
        }
    }
    
    /**
     * Exceptionhandler for the JMSConnection.
     * Only handles exceptions, if reconnectInProgress is false.
     * Only handles exceptions with errorcode PACKET_ACK_FAILED or
     * SESSION_IS_CLOSED.
     * 
     * @param e an JMSException
     */
    public void onException(JMSException e) {
        ArgumentNotValid.checkNotNull(e, "JMSException e");
        final String errorcode = e.getErrorCode();
        log.warn("JMSException with errorcode '"
                +  errorcode + "' encountered: " + e);
        
        // Try to re-establish connections to the jmsbroker only when
        // - PACKET_ACK_FAILED
        // - SESSION_IS_CLOSED
        // - RECEIVED_GOODBYE_FROM_BROKER
        if (errorcode.equals(PACKET_ACK_FAILED) 
                || errorcode.equals(SESSION_IS_CLOSED)
                || errorcode.equals(RECEIVED_GOODBYE_FROM_BROKER)) {
            if (reconnectInProgress.compareAndSet(false, true)) { 
                performReconnect(e);
            } else {
                log.warn("Ignore exception - reconnect in progress: " + e);
            }
        } else {
            log.warn("Exception not handled. Don't know how to handle exceptions with errorcode "
                    + errorcode);
        }
    }
    
    /**
     * Try to do a reconnect to the JMSbroker.
     * @param e 
     */
    private void performReconnect(JMSException e){
        String errorCode = e.getErrorCode();
        log.info("Perform reconnect due to JMSexception with errorcode '"
                + errorCode + "'" + e);

        log.info("Trying to reconnect to jmsbroker at " 
                + getHost() + ":" + getPort());

        boolean operationSuccessful = false;
        JMSException lastException = null;
        int tries = 0;
        while (!operationSuccessful && tries < 10) {
            tries++;
            try {
                reconnect();
                operationSuccessful = true;
            } catch (JMSException e2) {
                lastException = e2;
                log.warn("Exception during reconnect(): " + e2);
            }
            log.warn("Will sleep now (Thread = " + Thread.currentThread().getId());
            exponentialBackoffSleep(1);

        }
        if (!operationSuccessful) {
            throw new IOFailure("Reconnect failed: " + lastException);
        }

        // Add listeners already stored in the consumers map
        log.info("Add listeners");
        try {
            for (String consumerkey: consumers.keySet()) {
                String channelName = getChannelName(consumerkey);
                boolean isTopic = Channels.isTopic(channelName);
                MessageConsumer mc = consumers.get(consumerkey);
                if (isTopic) {                  
                    TopicSubscriber myTopicSubscriber = myTSess.createSubscriber(getTopic(channelName));
                    myTopicSubscriber.setMessageListener(mc.getMessageListener());
                } else { // the channelName belongs to a queue
                    Queue queue = getQueue(channelName);
                    QueueReceiver myQueueReceiver = myQSess.createReceiver(queue);
                    myQueueReceiver.setMessageListener(mc.getMessageListener());
                }
            }
            log.info("Use this as exceptionshandler for the two JMS Connections");
            myQConn.setExceptionListener(this);
            myTConn.setExceptionListener(this);
        } catch (JMSException e1) {
            // We cannot do anything more at this point
            log.warn(e1);
        }
        reconnectInProgress.compareAndSet(true, false);
        log.info("Reconnect successful");
    }
    
    /**
     * Reconnect to JMSBroker and reestablish sessions.
     * Resets senders and publishers.
     * @throws JMSException
     */
    private void reconnect() throws JMSException {
        myQConnFactory = getQueueConnectionFactory();
        myTConnFactory = getTopicConnectionFactory();

        // Establish a queue connection and a session
        myQConn = myQConnFactory.createQueueConnection();
        myQSess = myQConn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

        // Establish a topic connection and a session
        myTConn = myTConnFactory.createTopicConnection();
        myTSess = myTConn.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
        
        // Reset senders & publishers
        senders.clear();
        publishers.clear();
    }
   
}
