class Ball {
   private int destination;
   
   public void setDestination( int destination ) {
       this.destination = destination;
   }
   
   public int getDestination() {
      return destination;
   }
}

class Random {
   private final static java.util.Random rnd;
   private final int players;
   static {
      rnd = new java.util.Random();
   }

   public Random( int players ) {
      this.players = players;
   }

   public int getOtherPlayerNumber( int myNumber ) {
      int result;
      do {
         result = rnd.nextInt( players );
      } while ( result == myNumber );
      return result;
   }
}

class Player implements Runnable {
   private int myNumber;
   private Ball ball;
   private Random random;
   
   public Player( int id, Ball ball, Random random ) {
      myNumber = id;
      this.ball = ball;
      this.random = random;
   }
   
   public void run() {
      int destination;
      while ( true ) {
         if ( ball.getDestination( ) == myNumber ) {
            destination = random.getOtherPlayerNumber(myNumber);
            System.out.println( "Player " + myNumber + " to " + destination );
            ball.setDestination(destination);
         } 
      }
   }
}

class Start {
   private static final int NUMBER_OF_PLAYERS = 22;

   public static void main( String[] argv ) throws Exception {
      
      Ball ball = new Ball();
      Random rnd = new Random( NUMBER_OF_PLAYERS );

      for ( int i = 0; i < NUMBER_OF_PLAYERS; i++ ) {
         ( new Thread( new Player( i, ball, rnd ) ) ).start();
      }            
   }
}
