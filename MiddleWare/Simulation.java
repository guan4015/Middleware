package MiddleWare;

import MonteCarlo.Option;

public class Simulation {
	
	private static Option<Integer, Integer> _option;
	private static Option<Integer, Integer> _option1;
	
    /**
     * Run the runnable with new Thread
     * @param runnable
     * @param daemon use daemon or not
     */
    public static void thread( Runnable runnable, boolean daemon ) {
        Thread brokerThread = new Thread( runnable );
        brokerThread.setDaemon( daemon );
        brokerThread.start();
    }
    
    /**
     * To setup the Options we used to test program
     */
    public static void generateSamplePath(){
    	
    	/**
    	 * Setup the parameters for the first option
    	 */
    	String optionName = "IBM"; 
		int optionDurations = 252;
		double initialPrice = 152.35;
		double interestRate = 0.0001;
		double volatility = 0.01;
		double strikePrice = 165;
		String payOutType = "European";
		
		/**
		 * Setup the first option
		 */
		_option = new Option<Integer, Integer>( optionName, 
				payOutType, interestRate, initialPrice, volatility, strikePrice );
		_option.setDuration( optionDurations );
		
		/**
		 * Setup the parameters for the second option
		 */
		String optionName1 = "IBM"; 
		int optionDurations1 = 252;
		double initialPrice1 = 152.35;
		double interestRate1 = 0.0001;
		double volatility1 = 0.01;
		double strikePrice1 = 164;
		String payOutType1 = "Asian";
		
		/**
		 * Setup the second option
		 */
		_option1 = new Option<Integer, Integer>( optionName1, 
				payOutType1, interestRate1, initialPrice1, volatility1, strikePrice1 );
		_option1.setDuration( optionDurations1 );
		
    }
    
    public static void main( String[] args ) throws Exception {
    	// Specify the confidence level
		double accuPoss = 0.96;
		// Specify the tolerance
		double accuRate = 0.1;
		
		generateSamplePath();
		
		// Create new threads
        thread( new MonteCarloServer( _option, accuPoss, accuRate ), false );
        thread( new MonteCarloServer( _option1, accuPoss, accuRate ), false );
        thread( new MonteCarloClient(), false );
        thread( new MonteCarloClient(), false );
        //Thread.sleep(5000);
    }
   
}
