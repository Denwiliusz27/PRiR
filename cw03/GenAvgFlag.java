class Flag {
    public  boolean write; // program działa z volatile
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
            if ( flag.write ) {
                for ( int i = 1; i < table.length; i++ )
                   table[ i ] = counter;
                counter++;
                flag.write = false;
                System.out.println( "Flaga na false ");
                System.out.println( "Oto dowód: " + flag.write );
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
            if ( ! flag.write ) {
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
                flag.write = true;
                System.out.println( "Flaga na true ");
                System.out.println( "Oto dowód: " + flag.write );
            }
        }
    }
}

class Main {
    public static void main( String ... arg ) {
        int[] table = new int[ 1000 ];
        Flag flag = new Flag();
        flag.write = true;
        new Thread( new Generator( table, flag ) ).start();
        new Thread( new Avg( table, flag ) ).start();
    }
}
