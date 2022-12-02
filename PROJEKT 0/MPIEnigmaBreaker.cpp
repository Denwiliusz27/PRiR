
//
// Created by Daniel Wielgosz on 02.11.2022.
//

#include "MPIEnigmaBreaker.h"
#include "mpi.h"
#include <algorithm>
#include <sys/wait.h>

using namespace std;

const uint MOVEPOSITIONS = 100;

MPIEnigmaBreaker::MPIEnigmaBreaker( Enigma *enigma, MessageComparator *comparator ) : EnigmaBreaker(enigma, comparator){}

MPIEnigmaBreaker::~MPIEnigmaBreaker() {
    delete[] rotorPositions;
}

void MPIEnigmaBreaker::crackMessage() {
    int rank;
    int size;
    uint *rotorsPositions = new uint[MAX_ROTORS];

    MPI_Comm_rank(MPI_COMM_WORLD, &rank);
    MPI_Comm_size(MPI_COMM_WORLD, &size);

    // pobieram max wartości dla kazdego z rotorów
    setMaxRotors();

    uint *messagesLengths = new uint[2];
    messagesLengths[0] = messageLength;
    messagesLengths[1] = expectedMessageLength;

    // do wszystkich wysyłane są długości wiadomości
    MPI_Bcast( messagesLengths, 2, MPI_UNSIGNED, 0 , MPI_COMM_WORLD );

    // ustawienie zmiennych dla każdego procesu
    messageLength = messagesLengths[0];
    expectedMessageLength = messagesLengths[1];

    // przechowuje wiadomości
    uint *buffor = new uint[messageLength + expectedMessageLength];

    if (rank == 0) {
        copy( messageToDecode, messageToDecode + messageLength, buffor );
        copy( expectedMessage, expectedMessage + expectedMessageLength, buffor + messageLength );
    }

    // bufor wysyłany do wszytskich procesów
    MPI_Bcast( buffor, messageLength + expectedMessageLength, MPI_UNSIGNED, 0, MPI_COMM_WORLD);

    // wiadomości i ich długości ustawiane dla wszystkich procesów
    setSampleToFind( buffor + messageLength, expectedMessageLength);
    setMessageToDecode( buffor, messageLength);

    if (rank == 0){
        MPI_Status status;
        bool ifRotorsMoved = true;
        int childProcess = 1;
        uint *testRotors = new uint[enigma->getNumberOfRotors()];
        uint *receivedRotors = new uint[enigma->getNumberOfRotors()];
        int nrOfProcesses = size;

        for (int i=0; i < enigma->getNumberOfRotors(); i++){
            testRotors[i] = 0;
        }

        int flag = 0;
        uint *dataToSend = new uint[enigma->getNumberOfRotors() + 1];

        while(true){
            // sprawdza czy nikt nic nie przysłał
            MPI_Iprobe(MPI_ANY_SOURCE, 100, MPI_COMM_WORLD, &flag , &status );

            // jeśli jestem ostatnim procesem to wyjdź
            if ( nrOfProcesses == 1 and !ifRotorsMoved ){
                break;
            }

            // dopóki ktoś prosi o dane to:
            while(flag) {
                // odebranie danych
                MPI_Recv( receivedRotors, enigma->getNumberOfRotors(), MPI_UNSIGNED, status.MPI_SOURCE, 100, MPI_COMM_WORLD, &status );

                // spradzenie czy odebrane ustawienie rotorow nie jest fałszywe - tzn. nie znaleziono rozwiązania
                if (receivedRotors[0] == enigma->getLargestRotorSetting()+1){
                    // rotory sie nie zmieniły to wyjdź
                    if (!ifRotorsMoved){
                        nrOfProcesses -= 1;
                    }

                    // wyślij ustawienie rotorów mówiące, że
                    MPI_Send(testRotors, enigma->getNumberOfRotors(), MPI_UNSIGNED, status.MPI_SOURCE, 100, MPI_COMM_WORLD);

                    // przesuń rotory o 100 pozycji
                    if (ifRotorsMoved){
//                        cout << " PROCES 0: wyslalem ustawienie: ";
//                        printRotors(testRotors);
//                        sleep( 3 );
                        calculateNewRotorsPosition(testRotors, enigma->getNumberOfRotors(), MOVEPOSITIONS+1);
//                        cout << " PROCES 0: nowe ustawienie: ";
//                        printRotors(testRotors);

//                        ifRotorsMoved = moveRotorsOnePosition(testRotors, enigma->getNumberOfRotors());
                    }

                    // ustaw rotory na wartość max + 1
                    if (!ifRotorsMoved){
                        deleteRotors(testRotors, enigma->getNumberOfRotors());
                    }

                // przesłane ustawienie rotorów nie jest fałszywe - znaleziono rozwiązanie
                } else {
                    ifRotorsMoved = false;
                    // znalezione rozwiązanie jest ustawiane jako result
                    setResult(receivedRotors, enigma->getNumberOfRotors());

                    // do potomków wysyłane fałszywe ustawienie == komunikat że to koniec pracy
                    deleteRotors(testRotors, enigma->getNumberOfRotors());
                    MPI_Send(testRotors, enigma->getNumberOfRotors(), MPI_UNSIGNED, status.MPI_SOURCE, 100, MPI_COMM_WORLD);
                    nrOfProcesses -= 1;
                }

                MPI_Iprobe(MPI_ANY_SOURCE, 100, MPI_COMM_WORLD, &flag , &status );

            }

            // skoro nikt nie prosi o dane to samemu szukaj rozwiązania
            if (ifRotorsMoved) {
//                cout << "sam sobie licze: ";
//                printRotors(testRotors);

                bool success = solutionFound(testRotors);
                if (success){
                    setResult(testRotors, enigma->getNumberOfRotors());
                    deleteRotors(testRotors, enigma->getNumberOfRotors());
                    ifRotorsMoved = false;
                } else {
                    ifRotorsMoved = moveRotorsOnePosition(testRotors, enigma->getNumberOfRotors());
                }
            }
        }
    } else {
        uint *taskRotors = new uint[enigma->getNumberOfRotors()];
        MPI_Status status;

        deleteRotors(taskRotors, enigma->getNumberOfRotors() );

        while(true){
            // wyślij prośbe o dane - fałszywe ustawienie rotorów, lub właściwe ustawienie = rozwiązanie
            MPI_Send(taskRotors, enigma->getNumberOfRotors(), MPI_UNSIGNED, 0, 100, MPI_COMM_WORLD);

            // odbierz rotory do sprawdzenia
            MPI_Recv(taskRotors, enigma->getNumberOfRotors(), MPI_UNSIGNED, 0, 100, MPI_COMM_WORLD, &status);

//            cout << rank << ": dostalem ustawienie: ";
//            printRotors(taskRotors);

            // odebrano fałszywe ustawienie == koniec pracy
            if (taskRotors[0] == enigma->getLargestRotorSetting()+1) {
                return;
            } else {
//                sleep(3);
                // sprawdź czy ustawienie jest rozwiązaniem
                for(int i=0; i<MOVEPOSITIONS; i++){
                    bool success = solutionFound(taskRotors);

                    if (success){
                        break;
                    } else {
                        moveRotorsOnePosition(taskRotors, enigma->getNumberOfRotors());
                    }
                }

//                cout << rank << ": koncze prace z ustawieniem: ";
//                printRotors(taskRotors);

                // nie znaleziono rozwiązania - ustaw rotory na fałszywe ustawienie
                deleteRotors(taskRotors, enigma->getNumberOfRotors());


//                bool success = solutionFound(taskRotors);
//
//                if (success){
//                } else {
//                    // nie znaleziono rozwiązania - ustaw rotory na fałszywe ustawienie
//                    deleteRotors(taskRotors, enigma->getNumberOfRotors());
//                }
            }
        }
    }
}

// ustawia rotory na fałszywe ustawienie - max + 1
void MPIEnigmaBreaker::deleteRotors(uint *testRotors, int size){
    for (int i=0; i < size; i++){
        testRotors[i] = enigma->getLargestRotorSetting() + 1;
    }
}

// ustawia dane ustawienie rotorów jako rozwiązanie
void MPIEnigmaBreaker::setResult(uint *successfulRotors, int size){
    for (int i=0; i< size; i++ ) {
        rotorPositions[i] = successfulRotors[i];
    }
}

// przesuwa rotory o 1 pozycje
bool MPIEnigmaBreaker::moveRotorsOnePosition(uint *testRotors, int size){
    int index = size-1;

    while (testRotors[index] == enigma->getLargestRotorSetting()){
        testRotors[index] = 0;
        index -= 1;
    }

    if (index == -1){
        return false;
    } else {
        testRotors[index] += 1;
    }
    return true;
}

// ustaw rotory na nowa pozycje
void MPIEnigmaBreaker::calculateNewRotorsPosition(uint *testRotors, int size, int loopCounter){
    int index = size-1;
    int newValue = 0;

//    cout << "~~zaczynam obliczanie nowej pozycji od: ";
//    printRotors(testRotors);

    for (int i=index; i>=0; i--){
        newValue = (loopCounter + testRotors[i]) % (enigma->getLargestRotorSetting() + 1);
        loopCounter = (loopCounter + testRotors[i])  / (enigma->getLargestRotorSetting() + 1);
        testRotors[i] = newValue;
//        cout << "new Value: " << newValue << endl;
//        cout << "loop c: " << newValue << endl;
    }

//    cout << "~~koncze obliczanie nowej pozycji na: ";
//    printRotors(testRotors);
}


void MPIEnigmaBreaker::setMaxRotors() {
    this->rMax = new uint[ MAX_ROTORS ];

    for ( uint rotor = 0; rotor < MAX_ROTORS; rotor++ ) {
        if ( rotor < enigma->getNumberOfRotors() )
            rMax[ rotor ] = enigma->getLargestRotorSetting();
        else
            rMax[ rotor ] = 0;
    }
}


void MPIEnigmaBreaker::setSampleToFind(uint *expected, uint expectedLength ) {
    comparator->setExpectedFragment(expected, expectedLength);
    this->expectedMessage = expected;
    this->expectedMessageLength = expectedLength;
}


bool MPIEnigmaBreaker::solutionFound(uint *rotorSettingsProposal ) {
    for ( uint rotor = 0; rotor < rotors; rotor++ )
        rotorPositions[rotor] = rotorSettingsProposal[rotor];

    enigma->setRotorPositions(rotorPositions);
    uint *decodedMessage = new uint[ messageLength ];

    for (uint messagePosition = 0; messagePosition < messageLength; messagePosition++ ) {
        decodedMessage[ messagePosition ] = enigma->code(messageToDecode[ messagePosition ] );
    }

    bool result = comparator->messageDecoded(decodedMessage);

    delete[] decodedMessage;

    return result;
}


void MPIEnigmaBreaker::getResult( uint *rotorPositions ) {
    for ( uint rotor = 0; rotor < rotors; rotor++ ) {
        rotorPositions[ rotor ] = this->rotorPositions[ rotor ];
    }
}


void MPIEnigmaBreaker::printRotors(uint *rotorsPositions){
    cout << "[";
    for (uint rotor = 0; rotor<rotors; rotor++){
        cout << rotorsPositions[rotor] << ", " ;
    }
    cout << "]" << endl;
}
