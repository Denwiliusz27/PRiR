#include <iostream>
#include <iomanip>
#include <stdlib.h>
#include <math.h>

using namespace std;

int main( void ) {

   long suma = 0;
   const long ITER = 10000000L;

#pragma omp parallel for reduction( +:suma )
   for ( long l = 0; l < ITER; l++ )
     suma++;

// operacje na wspoldzielonej zmiennej sa chronione !!! Wynik jest poprawny !!!

   cout << "SUMA : " << suma << endl;

} // main

