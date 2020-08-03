#!/bin/bash
# Iterates through all Kexts marked as required in the boot kext collection,
# and prints out which ones require ACPI (and thus won't work on arm64e)
# The output is formatted as elide-identifier arguments to kmutil, which can
# be copy-pasted into build_arm64e_kcache.sh.
set -e
IFS='
'
for i in $(find /System/Library/Extensions -name Info.plist)
do
	if grep -q "<string>Root</string>" "$i" && \
		grep -q "<key>com.apple.iokit.IOACPIFamily</key>" "$i"
	then
		identifier=$(grep -A 1 -m 2 "CFBundleIdentifier" "$i" | \
			head -n 2|tail -n 1|sed -e "s/.*<string>//g" -e "s@</string>@@g")
		echo "--elide-identifier $identifier \\"
	fi
done
