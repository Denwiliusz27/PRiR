#include<stdio.h>
#include<stdlib.h>
#include<unistd.h>
#include<signal.h>
#include<iostream>
#include<fcntl.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <sys/stat.h>
#include <sys/file.h>
#include <sys/msg.h>
#include <sys/shm.h>
#include "common.h"

using namespace std;

void signal_handler( int s ) {
 signal( SIGUSR1, signal_handler );
}

int main( void )
{
  key_t key = ftok( COMMON_FILE_NAME, 'A' );
  cout << "Key 1 : " << key << endl;
  
  int shmid = shmget( key, BLOCK_SIZE * sizeof( double ), 0644 | IPC_CREAT );  // tworzymy blok pamieci wspoldzelonej
  double *ptr = (double *)shmat( shmid, (void *) 0, 0 ); // podlaczamy blok pamieci wspoldzielonej do naszego programu
  if ( ptr == (double *)(-1) ) {
    cerr << "Blad" << endl; return 1; }

  signal( SIGUSR1, signal_handler ); // obsluga sygnalu USR1
  
  int offset = 0;
  int size = BLOCK_SIZE;

  struct info_msgbuf im; 
  size_t msgsz = sizeof( struct info_struct ); // rozmiar komunikatu
  int msgid = msgget( key, 0666 | IPC_CREAT ); // tu zakladamy kolejke komunikatow
  im.mtype = getpid(); // typ wiadomosci = PID generatora
  im.info.pid = getpid(); // tu powielamy PID generatora
  im.info.offset = offset; // poczatek obszaru z danymi do przetworzenia
  im.info.size = size; // rozmiar obszaru gotowego do przetworzenia

  srand( offset );

  double counter = 0.0;
  while(1) {
     cout << "Wypelniam pamiec" << endl;
     for ( int i = 0; i < size; i++ ) {
        ptr[ offset + i ] = counter + 0.01 * ( random() / ( 1.0 + (double)RAND_MAX ) - 0.5 );
     }
     cout << "Wysylam wiadomosc" << endl;
     msgsnd( msgid, &im, msgsz, 0 ); // wiadomosc -> kolejka komunikatow
     cout << "Ide spac" << endl;
     counter += 0.1;
     sleep(10);	// do usuniecia po poprawie programu - ta linijka bardzo spowalnia dzialanie
     // sleep jest przerywany przyjsciem sygnalu
  }

  return 0;
}

