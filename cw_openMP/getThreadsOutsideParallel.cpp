#include<iostream>
#include<omp.h>

using namespace std;

int main( void ) {
   cout << "Liczba wątków: " << omp_get_num_threads() << endl;

    #pragma omp parallel
    {
    #pragma omp single 
    {
        cout << "parallel" << endl;
        cout << "Liczba wątków: " << omp_get_num_threads() << endl;    
    }
    } // parallel

}
