package org.apache.activemq.artemis.jms.example;

import java.io.IOException;
import java.util.Properties;
import javax.jms.*;
import java.io.InputStream;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AMQPublisher {
  private static final Logger LOG = LoggerFactory.getLogger(AMQPublisher.class);
  private  Queue queue;
  private  Connection connection = null;
  private  JmsConnectionFactory connectionFactory;
  private  Properties properties;
  private  MessageProducer sender = null;

  public  void connectToAMQ() throws JMSException {
    properties = new Properties();
    try {
      InputStream inputStream = getClass().getClassLoader().getResourceAsStream("amq.properties");
      properties.load(inputStream);
    } catch (IOException ex) {
      LOG.warn("amq.properties not found, defaulting to packaged version.");
      throw new JMSException("unable to read properties");
    }
    connectionFactory = new JmsConnectionFactory();
    connectionFactory.setUsername(properties.getProperty("broker.user"));
    connectionFactory.setPassword(properties.getProperty("broker.passwd"));
    connectionFactory.setRemoteURI(properties.getProperty("broker.uri"));
    connectionFactory.setSendTimeout(Long.parseLong(properties.getProperty("broker.sendTimeout")));
    try {
      // Step 1. Create an amqp qpid 1.0 connection
      connection = connectionFactory.createConnection(properties.getProperty("broker.user"), properties.getProperty("broker.passwd"));

      // Step 2. Create a session
      Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

      // Step 3. Create a sender
      queue = session.createQueue(properties.getProperty("broker.destination"));
      sender = session.createProducer(queue);
    } catch (JMSException ex) {
      LOG.error("unable to established connection with AMQ", ex);
      throw ex;
    }
  }

  public void sendMessage(Message message) throws JMSException {
    try {
      System.out.println("sending message : " );
      sender.send(message);
    } catch (JMSException ex) {
      LOG.error("failed to send messages to AMQ", ex);
      throw ex;
    }
  }

  public  void closeConnection() throws JMSException  {
    if (connection != null) {
      // Step 9. close the connection
      connection.close();
    }
  }
}
