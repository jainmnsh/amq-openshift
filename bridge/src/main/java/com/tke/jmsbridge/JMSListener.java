package com.tke.jmsbridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Hashtable;
import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * This example shows how to establish a connection to
 * and receive messages from a JMS queue. The classes in this
 * package operate on the same JMS queue. Run the classes together to
 * witness messages being sent and received, and to browse the queue
 * for messages.  This class is used to receive and remove messages
 * from the queue.
 *
 * @author Copyright (c) 1999-2005 by BEA Systems, Inc. All Rights Reserved.
 */
public class JMSListener implements MessageListener {
  // Defines the JNDI context factory.
  public final static String JNDI_FACTORY = "weblogic.jndi.WLInitialContextFactory";
  // Defines the JMS connection factory for the queue.
  public final static String JMS_FACTORY = "jms/TKEAntennaCF";
  // Defines the queue.
  public final static String QUEUE = "jms/TKEAntennaInQueue";
  private QueueConnectionFactory qconFactory;
  private QueueConnection qcon;
  private QueueSession qsession;
  private QueueReceiver qreceiver;
  private Queue queue;
  private boolean quit = false;
  private final static String selector = "appID='FH2.0'";
  private AMQPublisher amqPublisher;
  private static final Logger LOG = LoggerFactory.getLogger(JMSListener.class);

  /**
   * Message listener interface.
   *
   * @param msg message
   */
  public void onMessage(Message msg) {
    try {
      String msgText;
      if (msg instanceof TextMessage) {
        msgText = ((TextMessage) msg).getText();
      } else {
        msgText = msg.toString();
      }
      LOG.debug("Message Received: " + msgText);
      System.out.println("Message Received: " + msgText);
      amqPublisher.sendMessage(msg);
      if (msgText.equalsIgnoreCase("quit")) {
        synchronized (this) {
          quit = true;
          this.notifyAll(); // Notify main thread to quit
        }
      }
    } catch (JMSException jmse) {
      LOG.error("An exception occurred: " , jmse);
    }
    catch (Exception ex){
      LOG.error("An exception while sending the message to aMQ: " , ex);
    }
  }

  /**
   * Creates all the necessary objects for receiving
   * messages from a JMS queue.
   *
   * @param ctx       JNDI initial context
   * @param queueName name of queue
   * @throws NamingException if operation cannot be performed
   * @throws JMSException    if JMS fails to initialize due to internal error
   */
  public void init(Context ctx, String queueName)
    throws NamingException, JMSException {
      amqPublisher = new AMQPublisher();
      amqPublisher.connectToAMQ();
    qconFactory = (QueueConnectionFactory) ctx.lookup(JMS_FACTORY);
    qcon = qconFactory.createQueueConnection();
    qsession = qcon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
    queue = (Queue) ctx.lookup(queueName);
    qreceiver = qsession.createReceiver(queue, selector);
    qreceiver.setMessageListener(this);
    qcon.start();
  }

  /**
   * Closes JMS objects.
   *
   * @throws JMSException if JMS fails to close objects due to internal error
   */
  public void close() throws JMSException {
    qreceiver.close();
    qsession.close();
    qcon.close();
    amqPublisher.closeConnection();
  }

  /**
   * main() method.
   *
   * @param args WebLogic Server URL
   * @throws Exception if execution fails
   */
  public static void main(String[] args) throws Exception {
//    Properties properties = new Properties();
//    try {
//      InputStream inputStream = getClass().getClassLoader().getResourceAsStream("amq.properties");
//      properties.load(inputStream);
//    } catch (IOException ex) {
//      LOG.warn("amq.properties not found, defaulting to packaged version.");
//      properties.load(AMQPublisher.class.getResourceAsStream("/amq.properties"));
//    }
//    InitialContext ic = getInitialContext("t3://vmohsthyk067.oracleoutsourcing.com:40521");
    InitialContext ic = getInitialContext("t3://oraclejms.tkeaws.com:40521");
    JMSListener qr = new JMSListener();
    qr.init(ic, QUEUE);
    System.out.println(
      "JMS Ready To Receive Messages (To quit, send a \"quit\" message).");
    // Wait until a "quit" message has been received.
    synchronized (qr) {
      while (!qr.quit) {
        try {
          qr.wait();
        } catch (InterruptedException ie) {
        }
      }
    }
    qr.close();
  }

  private static InitialContext getInitialContext(String url)
    throws NamingException {
    Hashtable env = new Hashtable();
    env.put(Context.INITIAL_CONTEXT_FACTORY, JNDI_FACTORY);
    env.put(Context.PROVIDER_URL, url);
    env.put(Context.SECURITY_PRINCIPAL, "deployer");
    env.put(Context.SECURITY_CREDENTIALS, "immDzk23D");
    return new InitialContext(env);
  }
}
