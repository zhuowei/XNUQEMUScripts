b *0xFFFFFFF007433BE8
commands 1
print $x1=1000000000
c
end
b *0xFFFFFFF005FA5D84
commands 2
print ((unsigned int*)0xFFFFFFF0058083A8)[0] = 1
c
end
b *0xfffffff00743e434
commands 3
print $pc=0xfffffff00743e438
print $x0=1
c
end
