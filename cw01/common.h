#define COMMON_FILE_NAME "common.h"

const int BLOCK_SIZE = 4000000; // rozmiar bloku danych

struct info_struct {
  pid_t pid;
  int offset;
  int size;
};

struct info_msgbuf {
  long mtype;   
  struct info_struct info; 
};

