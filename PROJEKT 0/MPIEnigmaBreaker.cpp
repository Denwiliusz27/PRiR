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

const uint ROTORSSEND = 100;
const uint POSITIONSEND = 200;


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
        int childProcess = 1;
        uint *testRotors = new uint[enigma->getNumberOfRotors()];
        uint *receivedRotors = new uint[enigma->getNumberOfRotors()];
        int nrOfProcesses = size;
        uint64_t possibleRotorsPositions = pow((enigma->getLargestRotorSetting()+1), enigma->getNumberOfRotors());

        uint64_t startRotorsPosition = 0;
        uint64_t endRotorsPosition = 0;

        uint64_t *dataToSend = new uint64_t[2];

        for (int i=0; i < enigma->getNumberOfRotors(); i++){
            testRotors[i] = 0;
        }

        int flag = 0;

        while(true){
            // sprawdza czy nikt nic nie przysłał
            MPI_Iprobe(MPI_ANY_SOURCE, ROTORSSEND, MPI_COMM_WORLD, &flag , &status );

            // jeśli jestem ostatnim procesem to wyjdź
            if ( nrOfProcesses == 1 and !ifRotorsMoved ){
                break;
            }

            // dopóki ktoś prosi o dane to:
            while(flag) {
                // odebranie danych
                MPI_Recv( receivedRotors, enigma->getNumberOfRotors(), MPI_UNSIGNED, status.MPI_SOURCE, ROTORSSEND, MPI_COMM_WORLD, &status );

                // spradzenie czy odebrane ustawienie rotorow nie jest fałszywe (równe max+1) - tzn. nie znaleziono rozwiązania
                if (receivedRotors[0] == enigma->getLargestRotorSetting()+1){
                    Debug("+++++++++++++++++jeszcze dziala " << nrOfProcesses << " procesow" );


                    // rotory sie nie zmieniły to wyjdź
                    if (!ifRotorsMoved){
                        dataToSend[0] = 0;
                        dataToSend[1] = 0;

                        cout << "informuje ze trzeba skonczyc" << endl;
                        MPI_Send(dataToSend, 2, MPI_UNSIGNED_LONG_LONG, status.MPI_SOURCE, POSITIONSEND, MPI_COMM_WORLD);
                        cout << "~~~poinformowane" << endl;

                        nrOfProcesses -= 1;
                        Debug("jeszcze dziala " << nrOfProcesses << " procesow" );
                        break;
                    }

                    if (startRotorsPosition + 10 <= possibleRotorsPositions){
                        endRotorsPosition = startRotorsPosition + 10;
                    } else {
                        endRotorsPosition = possibleRotorsPositions;
                    }

                    dataToSend[0] = startRotorsPosition;
                    dataToSend[1] = endRotorsPosition;


                    // wyślij ustawienie rotorów mówiące, że
                    MPI_Send(dataToSend, 2, MPI_UNSIGNED_LONG_LONG, status.MPI_SOURCE, POSITIONSEND, MPI_COMM_WORLD);


                    // przesuń rotory na pozycje endRotorsPosition
                    if (ifRotorsMoved){
//                        cout << "Proces 0: wyslalem rotory:";
//                        printRotors(testRotors);
                        calculateNewRotorsPosition(testRotors, enigma->getNumberOfRotors(), endRotorsPosition);
//                        sleep( 3 );
//                        cout << "PROCES 0: przesunalem rotory na pozycje " << endRotorsPosition  << endl;
//                        printRotors(testRotors);
                        startRotorsPosition = endRotorsPosition;


                        // ifRotorsMoved = moveRotorsOnePosition(testRotors, enigma->getNumberOfRotors());
                    }

                    // ustaw rotory na wartość max + 1
                    if (!ifRotorsMoved || (endRotorsPosition == possibleRotorsPositions)){
                        deleteRotors(testRotors, enigma->getNumberOfRotors());
                    }

                    // przesłane ustawienie rotorów nie jest fałszywe - znaleziono rozwiązanie
                } else {
                    ifRotorsMoved = false;
                    // znalezione rozwiązanie jest ustawiane jako result
                    setResult(receivedRotors, enigma->getNumberOfRotors());

                    // do potomków wysyłane fałszywe ustawienie == komunikat że to koniec pracy
                    deleteRotors(testRotors, enigma->getNumberOfRotors());

                    dataToSend[0] = 0;
                    dataToSend[1] = 0;

                    cout << "################################## koniec? " << endl;
                    MPI_Send(dataToSend,2, MPI_UNSIGNED_LONG_LONG, status.MPI_SOURCE, POSITIONSEND, MPI_COMM_WORLD);
                    nrOfProcesses -= 1;
                    Debug("~~~~jeszcze dziala " << nrOfProcesses << " procesow" );
                }
                MPI_Iprobe(MPI_ANY_SOURCE, ROTORSSEND, MPI_COMM_WORLD, &flag , &status );
            }

            // skoro nikt nie prosi o dane to samemu szukaj rozwiązania
            if (ifRotorsMoved) {
                bool success = solutionFound(testRotors);
                if (success){
                    setResult(testRotors, enigma->getNumberOfRotors());
                    deleteRotors(testRotors, enigma->getNumberOfRotors());
                    ifRotorsMoved = false;
                } else {
//                    cout << "PROCES 0: sam sobie popracuje:";
//                    printRotors(testRotors);
                    ifRotorsMoved = moveRotorsOnePosition(testRotors, enigma->getNumberOfRotors());
                    startRotorsPosition += 1;
                }
            }
        }
    } else {
        uint *taskRotors = new uint[enigma->getNumberOfRotors()];
        MPI_Status status;

        uint64_t *dataReceived = new uint64_t[2];

        deleteRotors(taskRotors, enigma->getNumberOfRotors() );

        while(true){
            // wyślij prośbe o dane - fałszywe ustawienie rotorów, lub właściwe ustawienie = rozwiązanie
            MPI_Send(taskRotors, enigma->getNumberOfRotors(), MPI_UNSIGNED, 0, ROTORSSEND, MPI_COMM_WORLD);

            // odbierz rotory do sprawdzenia
            MPI_Recv(dataReceived, 2, MPI_UNSIGNED_LONG_LONG, 0, POSITIONSEND, MPI_COMM_WORLD, &status);

            calculateNewRotorsPosition(taskRotors, enigma->getNumberOfRotors(), dataReceived[0]);
//            cout << "~~" << rank << ": Odebralem ustawienie:";
//            printRotors(taskRotors);


//            for (int i=0; i<enigma->getNumberOfRotors(); i++){
//                cout << dataReceived[i] << ", ";
//                taskRotors[i] = dataReceived[i];
//            }
//            cout << "]" << endl;


            // odebrano fałszywe ustawienie == koniec pracy
            if (dataReceived[0] == 0) {
                Debug(rank << " zakonczyl prace");
                return;
            } else {
                bool success;
                for (int i=dataReceived[0]; i<=dataReceived[1]; i++){

//                    cout << "~~PROCES" << rank << ": obliczam ustawienie: ";
//                    printRotors(taskRotors);

                    success = solutionFound(taskRotors);

                    // sprawdź czy ustawienie jest rozwiązaniem
                    if (success){
                        break;
                    } else {
                        moveRotorsOnePosition(taskRotors, enigma->getNumberOfRotors());
                    }
                }

                if (!success){
                    // nie znaleziono rozwiązania - ustaw rotory na fałszywe ustawienie
                    deleteRotors(taskRotors, enigma->getNumberOfRotors());
                }
            }
        }
    }
}


void MPIEnigmaBreaker::calculateNewRotorsPosition(uint *testRotors, int size, uint64_t positionNr){
    int rotorState;
    int x_power_n;

    for (int n=size-1; n>=0; n--){
//        cout << "Mam rotor: " << n << " o wartosci: " << testRotors[n] << endl;
        x_power_n = pow(enigma->getLargestRotorSetting(), n);
        testRotors[size-n-1] = positionNr / x_power_n;
        positionNr -= testRotors[size-n-1] * x_power_n;
//        cout << "n:" << n << " x^n:" << x_power_n << " stan:" << testRotors[size-n-1] << endl;
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