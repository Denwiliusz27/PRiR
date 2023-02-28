#include <unistd.h>
#include <stdio.h>
#include <iostream>
#include "mpi.h"

using namespace std;

const int SIZE = 100000;
int * arrayI = new int[ SIZE ];
int my_rank;
int processes;

void arrayInit() {
   for ( int i = 0; i < SIZE; i++ )
      arrayI[ i ] = i;
}

void sendArray( int destination ) {
   MPI_Send(arrayI, SIZE, MPI_INT, destination, 0, MPI_COMM_WORLD);
}

void receiveArray( int source ) {
  MPI_Status status;
  MPI_Recv(arrayI, SIZE, MPI_INT, source, 0, MPI_COMM_WORLD, &status);
}

int main(int argc, char **argv) {
  
  MPI_Init(&argc, &argv);
  MPI_Comm_rank(MPI_COMM_WORLD, &my_rank);
  MPI_Comm_size(MPI_COMM_WORLD, &processes);
  
  cout << "Oto ja " << my_rank << "/" << processes << endl;

  if ( ! my_rank ) arrayInit(); // w procesie o numerze 0 inicjacja tablicy

  int repetitions = 50;
  double t0 = MPI_Wtime();
  for ( int i = 0; i < repetitions; i++ ) {
    if (! my_rank) {
        sendArray( 1 );
        receiveArray( 1 );
    } else {
        receiveArray( 0 );
        sendArray( 0 );
    }
  }
  double tf = MPI_Wtime() - t0;
 
  MPI_Finalize();

  if ( ! my_rank) {
      double avg = tf / repetitions;
      cout << "Średni ping " << avg << endl;
      cout << "Średni transfer około " << 
             ( 2 * SIZE * sizeof( int ) / avg ) / 1024 / 1024 << "MBps" << endl;
  }
}