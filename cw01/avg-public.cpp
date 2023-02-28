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
#include "common.h"
#include <sys/shm.h>

using namespace std;

int main( void )
{
  key_t key = ftok( COMMON_FILE_NAME, 'A' );
  cout << "Key 1 : " << key << endl;
  
  int msgid = msgget( key, 0666 | IPC_CREAT ); // kolejka komunikatow
  size_t msgsz = sizeof( struct info_struct ); // rozmiar struktury do przesylania danych
  struct info_msgbuf im; 
 
  int shmid = shmget( key, BLOCK_SIZE * sizeof( double ), 0644 | IPC_CREAT ); 
  double *ptr = (double *)shmat( shmid, (void *) 0, 0 ); // podlaczamy pamiec wspoldzielona
  if ( ptr == (double *)(-1) ) {
    cerr << "Blad" << endl; return 1; } 
 
  while( 1 )  {
     cout << "Odbieramy" << endl;
     msgrcv( msgid, &im, msgsz, 0, 0 ); // odbieramy komunikaty od kazdego nadawcy
     cout << "Odebrane: PID : " << im.info.pid  // PID nadawcy
          << "       OFFSET : " << im.info.offset
          << "         SIZE : " << im.info.size << endl;
          
     cout << "Licze srednia: " << flush;

    // tutaj komentarz napisany lokalnie
     
     double s = 0.0;
     for ( int i = 0; i < im.info.size; i++ ) {
        s += ptr[ im.info.offset + i ];
     }
     cout << ( s / im.info.size ) << endl;

     cout << "Wysylam sygnal USR1" << endl;     
     sleep(2);                            // to jest do usuniecia, po poprawie programu
     kill( im.info.pid, SIGUSR1 ); // wysylamy sygnal do generatora
  }  
  return 0;
}



