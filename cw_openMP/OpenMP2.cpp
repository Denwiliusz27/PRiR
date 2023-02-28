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

   cout << "--------  -----  --- ->  Od startu programu: " << ( tf.tv_sec - ti->tv_sec + ( tf.tv_usec - ti->tv_usec ) * 0.000001 ) << " s" ; 

   if ( tl.tv_sec ) {
      cout << " ( do poprzedniego komunikatu: " << ( tf.tv_sec - tl.tv_sec + ( tf.tv_usec - tl.tv_usec ) * 0.000001 ) << " s ) "; 
   }

   gettimeofday( &tl, NULL );
   cout << endl;
}

int main( void ) {
  const int size = 20000000;
  const int delta_size = 300000;

  double *x = new double[ size ];
  double *y = new double[ size ];
  double *z = new double[ size ];
  int *delta = new int[ delta_size ];

  struct timeval ti;
  gettimeofday( &ti, NULL );

  cout << "Inicjacja tablic x, y " << endl;
  x[ 0 ] = 10.0;
  y[ 0 ] = 20.0;
  for ( int i = 1; i < size; i++ ) {
     x[ i ] = x[ i - 1 ] * 0.999 + ( (double)random() / RAND_MAX ) - 0.5;
     if ( x[ i ] < 0.0001 ) x[ i ] = 5.0;
     y[ i ] =  -y[ i - 1 ] * 0.999 + ( ( double )random() / RAND_MAX ) - 0.5;
     if ( fabs( y[ i ] ) < 0.0001 ) y[ i ] = 15.0;
  }
  cout << "Inicjacja tablic x, y - koniec" << endl;
  show( &ti );

  cout << "Inicjacja tablicy z" << endl;
  z[ 0 ] = 0.0;
  for ( int i = 1; i < size; i++ ) {
     z[ i ] = 1.0;
  }
  cout << "Inicjacja tablicy z - koniec" << endl;
  show( &ti );

  cout << "Inicjacja tablicy delta" << endl;
  for ( int i = 0; i < delta_size; i++ ) {
     delta[ i ] = 1 + (int)( 500.0 * ( double ) random() / (double)RAND_MAX ) ;
     if ( i > ( delta_size / 2 ) ) delta[ i ] += 250;
  } 
  cout << "Inicjacja tablicy delta - koniec" << endl;
  show( &ti );

//                                O B L I C Z E N I A  Z
  cout << "Obliczenia z" << endl;
  for( int i = 0; i < size; i++ ) {
     z[ i ] += sin( x[ i ] ) + sin( -y[ i ] ) + cos( x[ i ] - y[ i ] );
  }
  cout << "Obliczenia z - koniec" << endl;
  show( &ti );

//                               O B L I C Z E N I A  - F A Z A  2
  cout << "Obliczenia z - faza 2" << endl;
  for( int i = 0; i < size; i += size / 30 ) 
    for ( int j = 0; j < i; j++ )
      z[ i ] += sin( x[ j ] - y[ j ] );
  cout << "Obliczenia z - faza 2 - koniec" << endl;
  show( &ti );

//                               O B L I C Z E N I A  - F A Z A  3
  cout << "Obliczenia z - faza 3" << endl;
  for( int i = 0; i < delta_size; i++ ) 
    for ( int j = 0; j < i; j += delta[ i ] ) 
      z[ j ] += sin( x[ j ] - y[ j ] );
  cout << "Obliczenia z - faza 3 - koniec" << endl;
  show( &ti );

//                               O B L I C Z E N I A  - F A Z A  4
  cout << "Obliczenia z - faza 4" << endl;
  for( int i = 0; i < delta_size; i++ ) 
    for ( int j = 0; j < i; j += delta[ i ] ) 
      z[ i ] += sin( x[ j ] - y[ j ] );
  cout << "Obliczenia z - faza 4 - koniec" << endl;
  show( &ti );

//                               O B L I C Z E N I A  - F A Z A  5
  cout << "Obliczenia z - faza 5" << endl;
  for( int i = 0; i < size; i += size / 30 ) 
    for ( int j = size-1; j >= i; j-- )
      z[ i ] += sin( x[ j ] + y[ j ] );
  cout << "Obliczenia z - faza 5 - koniec" << endl;
  show( &ti );

//                               O B L I C Z E N I A  - S U M A  Z
  cout << "Obliczenia suma z" << endl;
  double suma = 0.0;
  for ( int j = 0; j < size; j++ )
     suma += z[ j ];
  cout << "Obliczenia suma z - koniec" << endl;
  show( &ti );

//                               P O S Z U K I W A N I E   M A X
  cout << "Poszukiwanie MAX" << endl;
  double max = z[ 0 ];
  for ( int j = 1; j < size; j++ )
    if ( max < z[ j ] ) {
      max = z[ j ];
    }
  cout << "Poszukiwanie MAX - koniec" << endl;
  show( &ti );

  cout << "Wynik  max       : " << fixed << setw( 15 ) << setprecision( 10 ) << ( max - 174000.0 ) << endl;
  cout << "Wynik suma / max : " << fixed << setw( 15 ) << setprecision( 10 ) << suma / max << endl;
}
