#include<iostream>
#include<math.h>
#include<sys/time.h>
#include<stdlib.h>
#include<iomanip>

using namespace std;

void show( struct timeval *ti ) {
   struct timeval tf;
   static struct timeval tl = { 0,0 };
  
   gettimeofday( &tf, NULL ); 

   cout << "Delta T = " << ( tf.tv_sec - ti->tv_sec + ( tf.tv_usec - ti->tv_usec ) * 0.000001 ) ; 

   if ( tl.tv_sec ) {
      cout << " ( " << ( tf.tv_sec - tl.tv_sec + ( tf.tv_usec - tl.tv_usec ) * 0.000001 ) << " ) "; 
   }

   gettimeofday( &tl, NULL );
   cout << endl;
}

int main( void ) {
  const int size = 500000000;

  double *x = new double[ size ];

  struct timeval ti;
  gettimeofday( &ti, NULL );

  cout << "Inicjacja tablicy x " << endl;
  x[ 0 ] = 10.0;
  for ( int i = 1; i < size; i++ ) {
     x[ i ] = x[ i - 1 ] * 0.999 + ( (double)random() / RAND_MAX ) - 0.5;
  }
  cout << "Inicjacja x - koniec" << endl;
  show( &ti );

  cout << "MAX" << endl;

/////////// TEN FRAGMENT KODU CHCEMY ZROWNOLEGLIC !!!!
  double max = x[0];
  for ( int i = 1; i < size; i++ ) {
     if ( max < x[ i ] ) max = x[ i ];
  }
  show( &ti );

  cout << "MAX = " << fixed << setw(20) << setprecision(12) <<  max << endl;

}

