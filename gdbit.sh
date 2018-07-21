#!/bin/sh
# note: gdb-multiarch refuses to set breakpoints in kernel addresses.
# so only the devkitA64 and the Linaro versions are currently supported.
GDB=aarch64-none-elf-gdb
if [ -e /opt/devkitpro/devkitA64/bin/aarch64-none-elf-gdb ]
then
	GDB=/opt/devkitpro/devkitA64/bin/aarch64-none-elf-gdb
elif [ -e gcc-linaro-7.3.1-2018.05-x86_64_aarch64-elf/bin/aarch64-elf-gdb ]
then
	GDB=gcc-linaro-7.3.1-2018.05-x86_64_aarch64-elf/bin/aarch64-elf-gdb
elif [ -e gcc-linaro-7.3.1-2018.05-i686_aarch64-elf/bin/aarch64-elf-gdb ]
then
	GDB=gcc-linaro-7.3.1-2018.05-i686_aarch64-elf/bin/aarch64-elf-gdb
fi
exec $GDB -ex "target remote :1234" -x gdbscript.gdb
