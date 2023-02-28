class Flag {
    private boolean write = true;

    public  void swap() { // działa jeśli do wszystkich funkcji dopiszemy "synchronized"
        write = !write;
    }

    public  boolean canWrite() { // synchronized
        return write;
    }

    public  boolean canRead() { // synchronized
        return !write;
    }

}

class Generator implements Runnable {
    private final int[] table;
    private final Flag flag;
    private int counter = 1;

    Generator( int[] table, Flag flag ) {
        this.table = table;
        this.flag = flag;
    }

    public void run() {
        while ( true ) {
            if ( flag.canWrite() ) {
                for ( int i = 1; i < table.length; i++ )
                   table[ i ] = counter;
                counter++;
                flag.swap();
                System.out.println( "Teraz odczyt");
            }
        }
    }
}

class Avg implements Runnable {
    private final int[] table;
    private final Flag flag;
    private int sum;
    private double lastAvg;

    Avg( int[] table, Flag flag ) {
        this.table = table;
        this.flag = flag;
    }

    public void run() {
        while ( true ) {
            if ( flag.canRead() ) {
                sum = 0;
                for ( int i = 1; i < table.length; i++ ) {
                    sum += table[ i ] ;
                }
                double newAvg =  (double)sum / ( table.length -1 );
                System.out.println(" avg = " + newAvg );
                if ( Math.abs( newAvg - lastAvg - 1.0 ) > 0.00001 ) {
                    System.out.println( "BŁĄD");
                    System.exit(1);
                }
                lastAvg = newAvg;
                flag.swap();
                System.out.println( "Teraz zapis");
            }
        }
    }
}

class Main {
    public static void main( String ... arg ) {
        int[] table = new int[ 1000 ];
        Flag flag = new Flag();
        new Thread( new Generator( table, flag ) ).start();
        new Thread( new Avg( table, flag ) ).start();
    }
}
