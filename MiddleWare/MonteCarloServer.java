package MiddleWare;

import MonteCarlo.*;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import org.apache.activemq.ActiveMQConnectionFactory;


/**
 * The Server class for Monte Carlo simulation. 
 * <code>Option</code> and accuracy requests needed.
 * Each server only make one simulation.
 * @author xiaog
 *
 */
public class MonteCarloServer implements Runnable {
	
	/**
	 * Specify the class member variables.
	 */

	private int _batchSize = 100;
	private Option<Integer, Integer> _option;
	private ConnectionFactory _connectionFactory;
	private Session _session;
	private Connection _connection;
	private MessageProducer _producer;
	private MessageConsumer _consumer;
	private MapMessage _message;
	private StatsCollector _stat;
	private double _accuPoss = 0.96;
	private double _accuRate = 0.01;
	private double _bound;
	private double _price;
	protected String _url = "vm://localhost?broker.persistent=false&broker.useJmx=false"; 
	
	
    /**
     * Compute the value of z_p level of confidence
     * @param t
     * @return
     */
	public double RationalApproximation( double t )
	{

	    double c[] = { 2.515517, 0.802853, 0.010328 };
	    double d[] = { 1.432788, 0.189269, 0.001308 };
	    return t - ( ( c[2]*t + c[1] )*t + c[0] ) /
	                ( ( ( d[2]*t + d[1] )*t + d[0] )*t + 1.0 );
	}
	/**
	 * 
	 * @param p
	 * @return compute the two-sided z-score of normal distribution
	 */
	public double NormalCDFInverse( double p )
	{
	    if ( p <= 0.0 || p >= 1.0 )
	    {
	    	throw new IllegalArgumentException( "p must be between 0.0 and 1.0" );
	    }
	 
	    // See article above for explanation of this section.
	    if ( p < 0.5 )
	    {
	        return -RationalApproximation( Math.sqrt( -2.0 * Math.log(p) ) );
	    }
	    else
	    {
	        // F^-1(p) = G^-1(1-p)
	        return RationalApproximation( Math.sqrt( -2.0 * Math.log(1-p) ) );
	    }
	}
	
	// Constructor
	
	public MonteCarloServer(Option<Integer, Integer> option){
		this._option = option;
		this._bound = NormalCDFInverse( _accuPoss + ( 1 - _accuPoss ) / 2.0 );
	}
	
	/**
	 * Construct the server with given Option and accuracy tolerances
	 * @param option <code>Option</code> to simulate
	 * @param accuPoss Possibility tolerance of the simulation
	 * @param accuRate Absolute error tolerance of the simulation
	 */
	public MonteCarloServer( Option<Integer, Integer> option, 
			                 double accuPoss, 
			                 double accuRate ){

		try {
			this._option = option;
			this._accuPoss = accuPoss;
			this._accuRate = accuRate;
			this._bound = NormalCDFInverse(_accuPoss + (1 - _accuPoss) / 2.0); 
			this._stat = new StatsCollector();
		}
		catch ( Exception e ) {
			System.out.println( "Caught: " + e );
	        e.printStackTrace();
	    }
        
	}
	

	/**
	 * Run the thread/server
	 */
	
    public void run() {
        try {
        	//Make connection queues and request message
        	makeConnection();
        	
            // Begin the simulation
        	simulation();
         	
            // Clean up
            _session.close();
            _connection.close();
            System.out.println( "ending thread" );
        }
        catch ( Exception e ) {
            System.out.println( "Caught: " + e );
            e.printStackTrace();
        }
    }
    
    /**
     * Assemble Message with the information from Option
     * @return assembled message
     */
    public MapMessage assembleMessageFromOption(){
    	try {
			MapMessage message = _session.createMapMessage();
			message.setDouble( "interestRate", _option.getInterestRate() );
			message.setDouble( "volatility", _option.getVolatility() );
			message.setDouble( "strikePrice", _option.getStrikePrice() );
			message.setInt( "duration", _option.getDuration() );
			message.setDouble( "initialPrice", _option.getStartPrice() );
			message.setString( "optionName", _option.getOptionName() );
			message.setString( "payOutType", _option.getPayOutType() );
			return message;
		} catch (JMSException e) {
			e.printStackTrace();
		}
    	return null;
    }
    
    /**
     * Make connection queues and request message
     * @throws JMSException
     */
	public void makeConnection() throws JMSException{
		// Create a ConnectionFactory
        _connectionFactory = new ActiveMQConnectionFactory( _url );

        // Create a Connection
        _connection = _connectionFactory.createConnection();
        _connection.start();

        // Create a Session
        this._session = _connection.createSession( false, Session.AUTO_ACKNOWLEDGE ) ;

        // Create the destination (Topic or Queue)
        Destination destination = _session.createQueue( "simulation request" );
        
        String returnTopicName = "Simulation result for server on thread  " + 
        Thread.currentThread().getId() + " for Option " + _option.getOptionName()
        + "; type: " + _option.getPayOutType();
        
        Topic returnTopic = _session.createTopic( returnTopicName );
        
        System.out.println( returnTopicName );
        
        // Create a MessageProducer from the Session to the Topic or Queue
        _producer = _session.createProducer( destination );
        _producer.setDeliveryMode( DeliveryMode.NON_PERSISTENT );
        _consumer = _session.createConsumer( returnTopic );
   	 	
        // Create a messages
        _message = this.assembleMessageFromOption();
        _message.setString( "returnTopicName", returnTopicName );
       
	}
	
	/**
	 * Doing the simulation
	 * @throws JMSException
	 */
    public double simulation() throws JMSException {
    	// Iteration numbers
    	int num = 0;
        while ( true ) {
        	//Send the requests
            for ( int i = 0; i < this._batchSize; i++ )
         		_producer.send(_message);
            
            //get the returned payout
         	for ( int i = 0; i < this._batchSize; i++ ){
	            TextMessage returnMessage= (TextMessage) _consumer.receive();
	            double payOut = Double.parseDouble( returnMessage.getText() );
	            _stat.update( payOut ); 
	            ++num;
         	}
         	
         	//Check the termination condition
         	if ( num % 10000 == 0 )
         		System.out.println( num );
         	
         	if ( _bound * _stat.getStd() / Math.sqrt(num) < _accuRate && num > 20 ) {
				System.out.println( num + " times simulations to converge!" );
				break;
			}
        }
        _price = _stat.getMean() * Math.exp(- this._option.getInterestRate() *
        		                         this._option.getDuration());
        StdOut.printf( "The %s type option of company %s has price: %f\n", 
        		       this._option.getPayOutType(),
        		       this._option.getOptionName(),
        		       _price );
        _producer.close();
        _consumer.close();
        return _stat.getMean();
    }
}