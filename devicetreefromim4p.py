from pyasn1.codec.der import decoder as der_decoder
def devicetreefromim4p(indata):
	outdata = der_decoder.decode(indata)
	return bytes(outdata[0][3])
