import sys
# Modifies a boot kext cache for Ghidra 9.1.4
# Usage: python3 patch_boot_kext_collection_for_ghidra.py <input.kc> <output.kc>
with open(sys.argv[1], "rb") as infile:
	indata = bytearray(infile.read())
# fix
# java.io.IOException:
# Error on line 2: DOCTYPE is disallowed when the feature "http://apache.org/xml/features/disallow-doctype-decl" set to true.
# by wiping out the DOCTYPE in the prelinked info plist
nextindex = 0
while True:
	index = indata.find(b"<!DOCTYPE", nextindex)
	if index == -1:
		break
	closing = indata.find(b">", index)
	if closing == -1:
		break

	nextkmbuildversion = indata.find(b"<key>KMBuildVersion", index)
	shouldpatch = nextkmbuildversion != -1 and (nextkmbuildversion - index) < 0x100

	if shouldpatch:
		indata[index:closing + 1] = (closing + 1 - index)*b" "
	nextindex = closing + 1
# fix
# java.io.IOException
# at ghidra.app.util.opinion.MachoLoader.load(MachoLoader.java:98)
# by wiping out the LC_BUILD_VERSION in the header
# https://github.com/NationalSecurityAgency/ghidra/issues/2192
lc_build_version_offset = indata.find(b"\x32\x00\x00\x00\x18\x00\x00\x00\x00\x00\x00\x00")
indata[lc_build_version_offset] = 0xff # invalid load command

with open(sys.argv[2], "wb") as outfile:
	outfile.write(indata)
