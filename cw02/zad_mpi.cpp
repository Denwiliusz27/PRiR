#include <unistd.h>
#include <stdio.h>
#include <iostream>
#include "mpi.h"

using namespace std;

const int SIZE = 1000000;
double * d_array = new double[ SIZE ];
int my_rank;
int processes;

int main(int argc, char **argv){

    for(i=0; i < SIZE; i++){
        d_array[i] = i;
    }

    MPI_Init(&argc, &argv);
    MPI_Comm_rank(MPI_COMM_WORLD, &my_rank);
    MPI_Comm_size(MPI_COMM_WORLD, &processes);

    cout << "Proces " << my_rank << " z " << processes << endl;

    

}