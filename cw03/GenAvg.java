class Generator implements Runnable {
    private volatile int[] table; // bez volatile a z final - nie działa
    private int counter;

    Generator( int[] table ) {
        this.table = table;
    }

    public void run() {
        while ( true ) {
            if ( table[0] == 0 ) {
                for ( int i = 1; i < table.length; i++ )
                   table[ i ] = counter;
                counter++;
                table[0] = 1;
                System.out.println( "Wpisano 1 ");
                System.out.println( "Oto dowód: " + table[ 0 ]);
            }
        }
    }
}

class Avg implements Runnable {
    private volatile int[] table; // final
    private int sum;

    Avg( int[] table ) {
        this.table = table;
    }

    public void run() {
        while ( true ) {
            if ( table[0] == 1 ) {
                sum = 0;
                for ( int i = 1; i < table.length; i++ ) {
                    sum += table[ i ] ;
                }
                System.out.println(" avg = " + ( (double)sum / ( table.length -1 ) ));
                table[ 0 ] = 0;
            }
        }
    }
}

class Main {
    public static void main( String ... arg ) {
        int[] table = new int[ 1025 ];
        new Thread( new Generator( table ) ).start();
        new Thread( new Avg( table ) ).start();
    }
}
