#start
#watchreg r12
#watchreg r13
#watchreg r14
#watchreg r15
#nocap
resurrectionThreshold 3.5
logcalls
checkpt 0xf900 0xfb00 __mementos_checkpoint checkpoints/logfile
#break 0xe7bc
