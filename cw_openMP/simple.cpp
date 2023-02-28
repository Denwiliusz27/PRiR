#include <iostream>
#include <iomanip>
#include <stdlib.h>
#include <math.h>

using namespace std;

int main( void ) {

   long suma = 0;
   const long ITER = 10000000L;

   for ( long l = 0; l < ITER; l++ )
     suma++;

   cout << "SUMA : " << suma << endl;

} // main

