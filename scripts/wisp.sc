#start
#watchreg r12
#watchreg r13
#watchreg r14
#watchreg r15
#nocap
resurrectionThreshold 3.5
logcalls
checkpt 0xfa00 0xfc00 __mementos_checkpoint checkpoints/logfile
#break 0xe7bc
