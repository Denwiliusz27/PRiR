#include <iostream>
#include <mpi.h>
#include <stdlib.h>
#include <sys/time.h>
#include <unistd.h>

using namespace std;

int main(int argc, char ** argv)
{
	MPI_Init(&argc,&argv);

        int rank ;
        MPI_Comm_rank( MPI_COMM_WORLD, &rank );
        double cyferka;
        MPI_Request mpi_r;
        MPI_Status mpi_s;

        if ( ! rank ) {
           cyferka = 3.1415;
           cout << "To ja proces 0. Cyferka to: " << cyferka << endl;
	   MPI_Isend( &cyferka, 1, MPI_DOUBLE, 1, 100, MPI_COMM_WORLD, &mpi_r );
           cyferka *= 2;
           cout << "To ja proces 0. Cyferka to: " << cyferka << endl;
           int flag = 0;
           do {
              MPI_Test( &mpi_r, &flag, &mpi_s );
              cout << "To ja proces 0. Cyferka to: " << cyferka << endl;
              cout << "To ja proces 0 - Czekam..." << endl;              
              sleep(1);
           } while ( ! flag );
           cout << "To ja proces 0 - Juz nie czekam..." << endl;
           cyferka *= 2;
           cout << "To ja proces 0. Cyferka to: " << cyferka << endl;
        }
        if ( rank == 1 ) {
          cout << "To ja proces 1 - ide spac na 10 sekund. Dobranoc !" << endl;
          sleep( 10 );
          cout << "To ja proces 1 - Uruchamiam MPI_Irecv()" << endl;
          MPI_Irecv( &cyferka, 1, MPI_DOUBLE, 0, 100, MPI_COMM_WORLD, &mpi_r );
          cout << "To ja proces 1 - Poczekam na dane..." << endl;
          MPI_Wait( &mpi_r, &mpi_s );
          cout << "To ja proces 1 - Oto dane : " << cyferka << endl;
        }
        
	MPI_Finalize();
}

