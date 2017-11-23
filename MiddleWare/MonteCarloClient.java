package MiddleWare;

import java.io.IOException;
import java.io.InputStreamReader;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;

/**
 * The Client side for Monte Carlo method which makes listener to finish the job
 * @author liuhao
 *
 */
public class MonteCarloClient implements Runnable, ExceptionListener {
	
	protected String _url = "vm://localhost?broker.persistent=false&broker.useJmx=false";
	
    public void run() {
        try {

            // Create a ConnectionFactory
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory( _url );

            // Create a Connection
            Connection connection = connectionFactory.createConnection();
            connection.start();
            
            // Set the exception listener
            connection.setExceptionListener( this );

            // Create a Session
            Session session = connection.createSession( false, Session.AUTO_ACKNOWLEDGE );

            // Create the destination (Topic or Queue)
            Destination destination = session.createQueue( "simulation request" );

            
            // Create a MessageConsumer from the Session to the Topic or Queue
            MessageConsumer consumer = session.createConsumer( destination );
            
            // Create message listener
            RequestListener listener = new RequestListener( session );
            consumer.setMessageListener( listener );
            
            //start connection
            connection.start();
            
            InputStreamReader inputStreamReader = new InputStreamReader( System.in );
            char answer = '\0';
            while (!((answer == 'q') || (answer == 'Q'))) {
                try {
                    answer = (char) inputStreamReader.read();
                } catch (IOException e) {
                    System.err.println( "I/O exception: " + e.toString() );
                }
            }
            
            System.out.println( "Quiting the Client!" );
            System.exit(1);
            consumer.close();
            session.close();
            connection.close();
        } catch (Exception e) {
            System.out.println( "Caught: " + e );
            e.printStackTrace();
        }
    }

	@Override
    public synchronized void onException(JMSException ex) {
        System.out.println( "JMS Exception occured.  Shutting down client." );
    }
}