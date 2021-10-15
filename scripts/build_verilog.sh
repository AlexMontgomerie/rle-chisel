#!/bin/bash
module=$1

inst=""

if [ "$module" == "encoder" ] ; then

    # create accum module instance
    inst="rle.Encoder(UInt(8.W), 0)"

elif [ "$module" == "decoder" ] ; then

    # create max pool module instance
    inst="rle.Decoder(UInt(8.W), 0)"

fi


# create file
cat <<EOF > _temphelper.scala
package _temphelper
import chisel3._
import chisel3.stage.ChiselStage

object Elaborate extends App {
    chisel3.Driver.execute(args, () => new ${inst})
}
EOF

# build verilog
sbt "runMain _temphelper.Elaborate --target-dir impl"
# sbt "runMain _temphelper.Elaborate --help"

# clean up file
rm _temphelper.scala


