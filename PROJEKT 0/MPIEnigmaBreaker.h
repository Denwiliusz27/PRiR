
//
// Created by Daniel Wielgosz on 02.11.2022.
//

#ifndef MAIN_CPP_MPIENIGMABREAKER_H
#define MAIN_CPP_MPIENIGMABREAKER_H

#include"EnigmaBreaker.h"

class MPIEnigmaBreaker : public EnigmaBreaker {
private:
    uint expectedMessageLength;
    uint *expectedMessage;
    uint *rMax;

    bool solutionFound( uint *rotorSettingsProposal );

public:
    MPIEnigmaBreaker( Enigma *enigma, MessageComparator *comparator );

    void crackMessage();
    void getResult( uint *rotorPositions );

    void setMaxRotors();
    void setSampleToFind(uint *expected, uint expectedLength ) override;
    int changeRotorsPositions(uint* rotorsPositions, int nr);
    void drukujRotory();
    bool moveRotorsOnePosition(uint *testRotors, int size);
    void deleteRotors(uint *testRotors, int size);
    void setResult(uint *successfulRotors, int size);

    virtual ~MPIEnigmaBreaker();
};


#endif //MAIN_CPP_MPIENIGMABREAKER_H
