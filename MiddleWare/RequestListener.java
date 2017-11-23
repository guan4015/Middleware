package MiddleWare;

import java.util.HashMap;


import javax.jms.DeliveryMode;
import javax.jms.MapMessage;
import javax.jms.MessageListener;
import javax.jms.Message;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

import MonteCarlo.*; 


/**
 * The RequestListener implements <code>MessageListener</code> to 
 * casts the message to a MapMessage and make a path and calculate its payout.
 */
public class RequestListener implements MessageListener {

	private Session _session;
	private HashMap< String, StockPath > _generatorsMap = new HashMap< String, StockPath >();
	private String _returnTopicName;
	private Option<Integer, Integer> _option;
	/**
	 * Construct the Listener with given session.
	 * @param session
	 */
    public RequestListener( Session session ){
    	this._session = session;
    }
    /**
     * Casts the message to a MapMessage and make a path and calculate its payout.
     *
     * @param message     the incoming message
     */
    public void onMessage( Message message ) {
    	MapMessage mapMessage = null;
        try {
        	if (message instanceof MapMessage) {
                mapMessage = (MapMessage) message;
                

                //Get option info from the message
                _option = this.assembleOptionFromMessage( mapMessage );
                _returnTopicName = mapMessage.getString( "returnTopicName" );
                
                //Make a path and find the payout
                double payout = makePathFindPayout();
                
                // Create the destination (Topic or Queue)
                Topic returnTopic = _session.createTopic( _returnTopicName );
                
                
                //Create a producer to reply the payout
                MessageProducer producer = _session.createProducer( returnTopic );
                producer.setDeliveryMode( DeliveryMode.NON_PERSISTENT );

                // Create a messages
                TextMessage returnMessage = _session.createTextMessage(Double.toString(payout));
                producer.send( returnMessage );
                producer.close();
            } else if( message instanceof TextMessage ) {
            	if(( (TextMessage) message).getText().equals("ending") ){
            		System.out.println("Received: ending message");
            		
            	} else {
            		System.out.println("Received: error message");
            		System.exit(1);
            	}
            }
        } catch (JMSException e) {
            System.err.println("JMSException in onMessage(): " + e.toString());
        } catch (Throwable t) {
            System.err.println("Exception in onMessage():" + t.getMessage());
        }
    }
    
    /**
     * A function translate the message into Option Object
     * @param message the MapMessage used to assemble Option 
     * @return The assembled Option
     * @throws JMSException 
     * @see Option
     */
    public Option<Integer, Integer> assembleOptionFromMessage( MapMessage message ) throws JMSException{
    		Option<Integer, Integer> option = new Option<Integer, Integer>();
	    	option.setInterestRate( message.getDouble( "interestRate" ) );
	    	option.setVolatility( message.getDouble( "volatility" ) );
	    	option.setStrikePrice( message.getDouble( "strikePrice" ) );
			option.setDuration( message.getInt("duration") );
			option.setStartPrice( message.getDouble( "initialPrice" ) );
			option.setOptionName( message.getString("optionName") );
			option.setPayOutType( message.getString("payOutType") );
			return option;	
    }
    
    /**
     * Make a path and find the payout
     * @return the payout
     */
    public double makePathFindPayout() {
    	
    	//Generate or fetch a path
        StockPath pathes = null;
        if ( _generatorsMap.containsKey( _returnTopicName ) ){
        	pathes = _generatorsMap.get( _returnTopicName );
        } else {
        	RandomVectorGenerator randomvec = new NormalRandomVectorGenerator(
        			_option.getDuration() );
        	pathes = new StockPathExponentialBrownian( _option, randomvec );
        	_generatorsMap.put( _returnTopicName, pathes ); 
        }
        
        if( _generatorsMap.size() > 200 )
        	_generatorsMap = new HashMap<String, StockPath>();
        
        // Get Payout
        PayOut payOutFunction = null;
        payOutFunction = new CallPayOut(_option.getStrikePrice(),
        			                    _option.getPayOutType() );
        
        return payOutFunction.getPayout(pathes);
        
    }
    
	public Session getSession() {
		return this._session;
	}
	public void setSession(Session session) {
		this._session = session;
	}

}