#include <unistd.h>
#include <stdio.h>
#include <string.h>
#include "mpi.h"

   int main(int argc, char **argv) {
     int my_rank;
     int p;
     int source;
     int dest;
     int tag=50;
     char message[100];
     MPI_Status status;
     char hostname[256];

     MPI_Init(&argc, &argv);
     MPI_Comm_rank(MPI_COMM_WORLD, &my_rank);
     MPI_Comm_size(MPI_COMM_WORLD, &p);
   
     if (my_rank != 0) {
       sprintf(message, "Hello from process %d.", my_rank);
       dest = 0;
       MPI_Send(message, strlen(message)+1, MPI_CHAR, dest,
                tag, MPI_COMM_WORLD);
     }
     else
       for (source=1; source<p; source++) {
         MPI_Recv(message, 100, MPI_CHAR, source, tag,
                  MPI_COMM_WORLD, &status);
         printf("%s\n", message);
       }
     
     gethostname(hostname,255);
     printf("Hello world!  I am process number: %d on host %s\n", my_rank, hostname);

     MPI_Finalize();
   }
