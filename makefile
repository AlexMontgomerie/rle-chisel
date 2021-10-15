
all: encoder decoder

encoder: Encoder.v
	vivado -mode batch -source scripts/generate_ip_package.tcl -tclargs encoder ./impl/Encoder.v
	mv component.xml ip/encoder/

decoder: Decoder.v
	vivado -mode batch -source scripts/generate_ip_package.tcl -tclargs decoder ./impl/Decoder.v
	mv component.xml ip/decoder/

Encoder.v:
	./scripts/build_verilog.sh encoder

Decoder.v:
	./scripts/build_verilog.sh decoder

clean:
	rm -rf ip_*  vivado*.* *.xml xgui/ .Xil* webtalk*
