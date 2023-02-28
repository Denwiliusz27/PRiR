#include <iostream>
#include <stdlib.h>
#include <math.h>
#include <omp.h>

using namespace std;

int main( void )
{
  int report_steps = 100000000;
  double* x = new double[ 128 ]; // wsp. x marynarza
  double* y = new double[ 128 ]; // wsp. y marynarza
  double angle;   // pod tym katem marynarz dokona jednego kroku
  double multipl = 6.28318531 / ( RAND_MAX + 1.0 );
  int threads;
  
  for ( int i = 0; i < 128; i++ ) {
    x[ i ] = y[i] = 0.0;
  }

  #pragma omp parallel private( angle )
  {
    threads = omp_get_num_threads();
    angle = 0.0;

  #pragma omp for
    for ( int thread = 0; thread < threads; thread++) {
      for ( int i = 0; i < report_steps; i++ ) {
        angle += 0.001; 
        x[thread*8] += sin( angle );  // 8 * sizeof( double ) = 64B
        y[thread*8] += cos( angle );  // patrz: cat /proc/cpuinfo
      }
    }
  }  // parallel

  double distanceSum = 0;
  for ( int thread = 0; thread < threads; thread++ ) {
     distanceSum += sqrt( x[thread * 8] * x[thread *8 ] + y[thread *8] * y[thread * 8]);
  }

  cout << "Raport po " << report_steps << " odleglosc / sqrt( liczba krokow ) = " 
    << ( distanceSum / threads ) / sqrt( report_steps ) << endl;
}
