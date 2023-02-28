//http://www.mcs.anl.gov/research/projects/mpi/www/www3/MPI_Bsend.html

#include <stdlib.h>
#include<iostream>
#include<mpi.h>
#include <unistd.h>

using namespace std;

const int SIZE = 100; //10000000;

void generacja( double *ptr, int size, double counter ) {
   cout << "Wypelniam bufor" << endl;
   for ( int i = 0; i < size; i++ ) 
      ptr[ i ] = counter + 0.01 * ( random() / ( 1.0 + (double)RAND_MAX ) - 0.5 );
}

int main(int argc, char *argv[]) {
   const int ptr_size = sizeof( double ) * SIZE;
   double *ptr;
  
   int numprocs;
   int myrank;
 
   MPI_Init(&argc,&argv); 
   MPI_Comm_rank(MPI_COMM_WORLD, &myrank); 
   MPI_Alloc_mem( ptr_size, MPI_INFO_NULL, &ptr );
 
   if ( myrank == 0 ) {  // proc 0 usrednia, pozostale generuja dane
      MPI_Status stat;
      while(1) {
         cout << "Odbieram dane" << endl;
         MPI_Recv(ptr, SIZE, MPI_DOUBLE, MPI_ANY_SOURCE, MPI_ANY_TAG, MPI_COMM_WORLD, &stat);
         double s = 0.0;
         for ( int i = 0; i < SIZE; i++ ) 
            s += ptr[ i ];
         cout << "Po usrednieniu danych : " << s / SIZE << endl;
      }
   } else { // tu zaczyna sie kod generujace dane
     srand( myrank );
     char *buff;
     MPI_Alloc_mem( ptr_size + MPI_BSEND_OVERHEAD, MPI_INFO_NULL, &buff );
     int buff_size;
     
     MPI_Buffer_attach( buff, ptr_size + MPI_BSEND_OVERHEAD ); // przygotowanie dodatkowego bufora     
     
     double counter = 0.0;
     
     generacja( ptr, SIZE, counter );
     
     while(1) {
        cout << "Wysylam wiadomosc" << endl;
        MPI_Bsend( ptr, SIZE, MPI_DOUBLE, 0, myrank, MPI_COMM_WORLD ); // buforowane wysylanie
        generacja( ptr, SIZE, counter );
        counter += 1.0;
        cout << "Synchronizacja" << endl;
        MPI_Buffer_detach( &buff, &buff_size );  // wymiecenie bufora 
        MPI_Buffer_attach( buff, buff_size);  // ponownie wlaczamy buforowanie  
     }
   }

  return 0;  
}

