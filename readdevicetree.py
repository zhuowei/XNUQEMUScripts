import sys
from devicetreefromim4p import *
# pexpert/pexpert/device_tree.h
def u32(a, i):
	return a[i] | a[i+1] << 8 | a[i+2] << 16 | a[i+3] << 24
def writenode(nodebytes, nodeoffset, nodedepth):
	print(" "*(4*nodedepth) + "{")
	nProperties = u32(nodebytes, nodeoffset)
	nChildren = u32(nodebytes, nodeoffset + 4)
	ptr = 8
	for p in range(nProperties):
		ptr += writeproperty(nodebytes, nodeoffset + ptr, nodedepth)
	for c in range(nChildren):
		ptr += writenode(nodebytes, nodeoffset + ptr, nodedepth + 1)
	print(" "*(4*nodedepth) + "}")
	return ptr

def printable(s):
	for c in s:
		if c < 0x20 or c >= 0x7f:
			return False
	return True

def decodepropvalue(propname, propvalueraw):
	if len(propvalueraw) == 0:
		return "(null)"
	if propname == "reg":
		return propvalueraw.hex()
	if propvalueraw[-1] == 0 and printable(propvalueraw[:-1]):
		return repr(propvalueraw[:-1].decode("utf-8"))
	if len(propvalueraw) == 4:
		return hex(u32(propvalueraw, 0))
	if len(propvalueraw) == 8:
		return hex(u32(propvalueraw, 0) | u32(propvalueraw, 4) << 32) + "L"
	return str(propvalueraw)

def writeproperty(nodebytes, nodeoffset, nodedepth):
	kPropNameLength = 32
	propname = nodebytes[nodeoffset:nodeoffset + kPropNameLength].rstrip(b"\x00").decode("utf-8")
	ptr = kPropNameLength
	proplen = u32(nodebytes, nodeoffset + ptr) & 0x7fffffff
	ptr += 4
	propvalueraw = nodebytes[nodeoffset + ptr:nodeoffset + ptr + proplen]
	propvalue = decodepropvalue(propname, propvalueraw)
	ptr += proplen
	print(" "*(4*nodedepth) + propname + ": " + propvalue)
	ptr = (ptr + 0x3) & ~0x3 #round up to nearest 4
	return ptr

def printone(filename):
	with open(filename, "rb") as infile:
		indata = infile.read()
	devicetreebytes = devicetreefromim4p(indata) if indata[0x7:0x7+4] == b"IM4P" else indata
	writenode(devicetreebytes, 0, 0)

if __name__ == "__main__":
	printone(sys.argv[1])
