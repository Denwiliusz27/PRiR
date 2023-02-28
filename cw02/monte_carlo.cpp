#include <stdio.h>
#include <stdlib.h>
#include "mpi.h"
#include <math.h>

/*
Na podstawie kodu ze strony:
https://www.olcf.ornl.gov/tutorials/monte-carlo-pi/
*/

int main(int argc, char* argv[])
{
    long int niter = 500000000;     // Liczba iteracji do wykonania
    double x,y;                     // PoĹoĹźenie x,y
    long int hits=0;                // Liczba trafieĹ
    long int reducedcount;          // CaĹkowita liczba trafieĹ w 1/4 koĹa
    int myid;                       // ID procesu wg. MPI
    int size;                       // Liczba utworzonych procesĂłw

    MPI_Init(&argc, &argv);                  // Start MPI
    MPI_Comm_rank(MPI_COMM_WORLD, &myid);    // Pobieramy myID
    MPI_Comm_size(MPI_COMM_WORLD, &size);    // Pobieramy size

    double z;
    double tStart = MPI_Wtime();             // Zapisujemy moment rozpoczÄcia obliczeĹ

    for (long int i=myid; i<niter; i+=size)  // Tu program wykonuje obliczenia.
    {
        x = (double)random()/RAND_MAX;       // Losujemy poĹoĹźenie x
        y = (double)random()/RAND_MAX;       // Losujemy poĹoĹźenie y
        z = sqrt((x*x)+(y*y));               // Sprawdzamy czy trafiliĹmy w ÄwiartkÄ
        if (z<=1)                            // koĹa wpisanego w kwadrat
        {
            ++hits;                // RoĹnie liczba trafieĹ
        }
    }

    MPI_Reduce(&hits,
               &reducedcount,
               1,
               MPI_LONG,
               MPI_SUM,
               0,
               MPI_COMM_WORLD);         // Obliczamy caĹkowitÄ liczbÄ trafieĹ

    if (myid == 0)                      // Wynik koĹcowy obliczamy i wyĹwietlamy w procesie 0
    {
        double tEnd = MPI_Wtime();      // Zapisujemy moment zakoĹczenia obliczeĹ
        double pi = ((double)reducedcount/(double)niter)*4.0;               //p = 4(m/n)
        printf("size %d Pi: %f\n%ld\n%ld\n", size, pi, reducedcount, niter);
        printf("Czas obliczeĹ: %fs\n", ( tEnd - tStart ) );
    }

    MPI_Finalize();                     // KoĹczymy obliczenia za pomocÄ MPI
    return 0;
}