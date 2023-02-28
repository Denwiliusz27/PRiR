#include<iostream>
#include<omp.h>

using namespace std;

int main( void ) {
    #pragma omp parallel
    {
        cout << "parallel" << endl;

    #pragma omp parallel for
        for ( int i = 0; i < 12; i++ ) {
            cout << "i = " << i << " id " << omp_get_thread_num() << endl;
        }


    } // parallel


}
