class WriteRead {
    volatile int v;

    public void write( int v ) {
       Thread th = new Thread( new Runnable() {
          public void run() {
	        WriteRead.this.v = v;
          }
       } );
       
       th.start();
    }
    
    public int read( ) {
       return v;
    }
}


class Start {
   public static void main( String[] argv ) {
      WriteRead wr = new WriteRead();
      
      for ( int i =0; i < 1000; i++ ) {
         wr.write(i);
         int read = wr.read();
         if ( read != i ) {
            System.out.println( "Blad dla i = " + i + " jest " + read );
            break;
         }
      }
      System.out.println( "Ponownie odczyt = " + wr.read() );
   }
}
