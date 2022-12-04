//
// Created by Daniel Wielgosz on 02.11.2022.
//

#include "MPIEnigmaBreaker.h"
#include "mpi.h"
#include <algorithm>
#include <cmath>
#include <sys/wait.h>

#define MPI_DEBUG true

#if MPI_DEBUG
#define Debug(x) std::cout<< x << '\n';
#else
#define Debug(x)
#endif


using namespace std;


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

    // bufor wysyłany do wszystkich procesów
    MPI_Bcast( buffor, messageLength + expectedMessageLength, MPI_UNSIGNED, 0, MPI_COMM_WORLD);

    // wiadomości i ich długości ustawiane dla wszystkich procesów
    setSampleToFind( buffor + messageLength, expectedMessageLength);
    setMessageToDecode( buffor, messageLength);

    if (rank == 0){
        MPI_Status status;
        bool ifRotorsMoved = true;
        uint *testRotors = new uint[enigma->getNumberOfRotors()];
        int nrOfProcesses = size;
        uint64_t possibleRotorsPositions = pow((enigma->getLargestRotorSetting()+1), enigma->getNumberOfRotors());
        uint *test = new uint[enigma->getNumberOfRotors()];

        uint64_t startRotorsPosition = 0;
        uint64_t endRotorsPosition = 0;

        uint64_t *dataToSend = new uint64_t[2];
        uint64_t *dataReceived = new uint64_t[2];

        for (int i=0; i < enigma->getNumberOfRotors(); i++){
            testRotors[i] = 0;
        }

        int flag = 0;

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
                MPI_Recv( dataReceived, 2, MPI_UNSIGNED_LONG_LONG, status.MPI_SOURCE, 100, MPI_COMM_WORLD, &status );

                // spradzenie czy odebrana pozycja rotorow nie jest fałszywe (== -2) - tzn. nie znaleziono rozwiązania
                if (dataReceived[0] == -2){

                    // rotory sie nie zmieniły to wyjdź
                    if (!ifRotorsMoved){
                        dataToSend[0] = -1;
                        dataToSend[1] = -1;

                        MPI_Send(dataToSend, 2, MPI_UNSIGNED_LONG_LONG, status.MPI_SOURCE, 100, MPI_COMM_WORLD);

                        nrOfProcesses -= 1;
                        break;
                    }

                    if (startRotorsPosition + 100 <= possibleRotorsPositions){
                        endRotorsPosition = startRotorsPosition + 100;
                    } else {
                        endRotorsPosition = possibleRotorsPositions;
                    }

                    dataToSend[0] = startRotorsPosition;
                    dataToSend[1] = endRotorsPosition;

                    // wyślij zakres pozycji rotorów do sprawdzenia
                    MPI_Send(dataToSend, 2, MPI_UNSIGNED_LONG_LONG, status.MPI_SOURCE, 100, MPI_COMM_WORLD);

                    // przesuń rotory na pozycje endRotorsPosition
                    if (ifRotorsMoved){
                        startRotorsPosition = endRotorsPosition+1;
                        calculateNewRotorsPosition(testRotors, enigma->getNumberOfRotors(), startRotorsPosition);
                    }

                    // ustaw zakresy do wysłania jako -1 - aby procesy zakonczyły prace
                    if (!ifRotorsMoved || endRotorsPosition == possibleRotorsPositions){
                        dataToSend[0] = -1;
                        dataToSend[1] = -1;
                    }

                    // otrzymano pozycje rotorów która nie jest fałszywa - znaleziono rozwiązanie
                } else {
                    ifRotorsMoved = false;

                    // otrzymana pozycja zamieniana na układ rotorów i ustawiana jako result
                    calculateNewRotorsPosition(test, enigma->getNumberOfRotors(), dataReceived[0]);
                    setResult(test, enigma->getNumberOfRotors());

                    // do potomków wysyłane fałszywy zakres = -1 - komunikat że to koniec pracy
                    dataToSend[0] = -1;
                    dataToSend[1] = -1;

                    MPI_Send(dataToSend,2, MPI_UNSIGNED_LONG_LONG, status.MPI_SOURCE, 100, MPI_COMM_WORLD);
                    nrOfProcesses -= 1;
                }

                MPI_Iprobe(MPI_ANY_SOURCE, 100, MPI_COMM_WORLD, &flag , &status );
            }

            // skoro nikt nie prosi o dane to samemu szukaj rozwiązania
            if (ifRotorsMoved) {
                bool success = solutionFound(testRotors);
                if (success){
                    setResult(testRotors, enigma->getNumberOfRotors());
                    ifRotorsMoved = false;
                } else {
                    ifRotorsMoved = moveRotorsOnePosition(testRotors, enigma->getNumberOfRotors());
                    startRotorsPosition += 1;
                }
            }
        }
    } else {
        uint *taskRotors = new uint[enigma->getNumberOfRotors()];
        MPI_Status status;
        uint64_t position;

        uint64_t *dataReceived = new uint64_t[2];
        dataReceived[0] = -2;
        dataReceived[1] = -2;

        while(true){
            // wyślij prośbe o dane - fałszywy zakres pozycji = 0, lub właściwą pozycje = rozwiązanie
            MPI_Send(dataReceived, 2, MPI_UNSIGNED_LONG_LONG, 0, 100, MPI_COMM_WORLD);

            // odbierz zakres do sprawdzenia
            MPI_Recv(dataReceived, 2, MPI_UNSIGNED_LONG_LONG, 0, 100, MPI_COMM_WORLD, &status);

            // odebrano fałszywy zakres == koniec pracy
            if (dataReceived[0] == -1) {
                return;
            } else {
                bool success;
                calculateNewRotorsPosition(taskRotors, enigma->getNumberOfRotors(), dataReceived[0]);

                for (int i=dataReceived[0]; i<=dataReceived[1]; i++){
                    success = solutionFound(taskRotors);

                    // sprawdź czy ustawienie jest rozwiązaniem
                    if (success){
                        position = calculatePositionFromRotors(taskRotors, enigma->getNumberOfRotors());
                        dataReceived[0] = position;
                        dataReceived[1] = position;

                        uint *test = new uint[enigma->getNumberOfRotors()];
                        calculateNewRotorsPosition(test, enigma->getNumberOfRotors(), position);
                        break;
                    } else {
                        moveRotorsOnePosition(taskRotors, enigma->getNumberOfRotors());
                    }
                }

                if (!success){
                    // nie znaleziono rozwiązania - ustaw zakres na fałszywe ustawienie
                    dataReceived[0] = -2;
                    dataReceived[1] = -2;
                }
            }
        }
    }
}

// konwertuje nr pozycji na odpowiadajace ustawienie rotorow
void MPIEnigmaBreaker::calculateNewRotorsPosition(uint *testRotors, int size, uint64_t positionNr){
    uint x_power_n;

    for (int n=size-1; n>=0; n--){
        x_power_n = calculatePow(enigma->getLargestRotorSetting()+1, n);
        testRotors[size-n-1] = positionNr / x_power_n;
        positionNr -= testRotors[size-n-1] * x_power_n;
    }
}

// konwertuje ustawienie rotorow na nr pozycji
uint64_t MPIEnigmaBreaker::calculatePositionFromRotors(uint *testRotors, int size){
    uint64_t value = 0;

    for(int n=size-1; n>=0; n--){
        value += testRotors[size-n-1] * calculatePow(enigma->getLargestRotorSetting()+1, n);
    }

    return value;
}

// oblicza wartosc x^n
uint64_t MPIEnigmaBreaker::calculatePow(uint64_t x, int n){
    uint64_t result = 1;

    for(int i=0; i<n; i++){
        result *= x;
    }

    return result;
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