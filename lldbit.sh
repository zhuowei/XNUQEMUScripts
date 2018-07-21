#!/bin/sh
# from xnu's README.md
exec lldb lz_comp2/kcache_out.bin -o "process connect --plugin gdb-remote connect://127.0.0.1:1234" -s lldbscript.lldb
