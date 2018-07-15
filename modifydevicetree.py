import sys
# pexpert/pexpert/device_tree.h
def u32(a, i):
	return a[i] | a[i+1] << 8 | a[i+2] << 16 | a[i+3] << 24
def w32(a, i, v):
	a[i] = v & 0xff
	a[i+1] = (v >> 8) & 0xff
	a[i+2] = (v >> 16) & 0xff
	a[i+3] = (v >> 24) & 0xff
def writenode(nodebytes, nodeoffset, nodedepth):
	nProperties = u32(nodebytes, nodeoffset)
	nChildren = u32(nodebytes, nodeoffset + 4)
	ptr = 8
	for p in range(nProperties):
		ptr += writeproperty(nodebytes, nodeoffset + ptr, nodedepth)
	for c in range(nChildren):
		ptr += writenode(nodebytes, nodeoffset + ptr, nodedepth + 1)
	return ptr

def writeproperty(nodebytes, nodeoffset, nodedepth):
	kPropNameLength = 32
	propname = nodebytes[nodeoffset:nodeoffset + kPropNameLength].rstrip(b"\x00").decode("utf-8")
	ptr = kPropNameLength
	proplen = u32(nodebytes, nodeoffset + ptr) & 0x7fffffff
	if u32(nodebytes, nodeoffset + ptr) != proplen:
		w32(nodebytes, nodeoffset + ptr, proplen)
	ptr += 4
	if propname == "timebase-frequency" and u32(nodebytes, nodeoffset + ptr) == 0:
		print("setting timebase")
		w32(nodebytes, nodeoffset + ptr, (1000 * 1000 * 1000)//16)
	if propname == "random-seed":
		print("setting random seed")
		w32(nodebytes, nodeoffset + ptr, 0xdeadf00d)
	ptr += proplen
	ptr = (ptr + 0x3) & ~0x3 #round up to nearest 4
	return ptr

def printone(filename, outname):
	with open(filename, "rb") as infile:
		indata = infile.read()
	indata = bytearray(indata)
	size = writenode(indata, 0x3c, 0)
	with open(outname, "wb") as outfile:
		outfile.write(indata[0x3c:0x3c+size])

if __name__ == "__main__":
	printone(sys.argv[1], sys.argv[2])
