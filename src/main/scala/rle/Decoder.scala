package rle

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum

class DecoderIO[T <: UInt](gen: T) extends Bundle {
  val in = Flipped(Decoupled(gen))
  val out = Decoupled(gen)
}

object DecoderState extends ChiselEnum {
  val EMPTY, ZERO, REPEAT = Value
}

class Decoder[T <: UInt](gen: T, rle_zero: Int) extends Module {

  // get the width of the UInt type
  val bit_width = gen.getWidth

  // check the rle_zero parameter
  assert(rle_zero >= 0, "The rle zero value must be greater than zero")
  assert(rle_zero < scala.math.pow(2,bit_width), "The rle zero value must be greater than zero")

  // intialise io definition
  val io = IO(new DecoderIO(gen))

  // create the states for rle
  val state = RegInit(DecoderState.EMPTY)

  // create registers to store incoming values and the rle counter
  val rle_cntr = RegInit(0.U(bit_width.W))

  // set defaults
  io.out.bits   := DontCare
  io.out.valid  := false.B
  io.in.ready   := false.B

  // create the finite state machine
  switch (state) {
    is(DecoderState.EMPTY) {
      when (io.in.bits =/= rle_zero.U && io.in.valid) {
        // pass value straight through if it's not an rle zero
        state := DecoderState.EMPTY
      } .elsewhen ( io.in.bits === rle_zero.U && io.in.valid ) {
        // move to the FILL state if the incoming value is an rle
        // zero, and update the rle counter
        state := DecoderState.ZERO
      }
      // connect the ready signal straight through, and the
      // input directly to output
      io.out.bits   := io.in.bits
      io.out.valid  := io.in.valid
      io.in.ready   := io.out.ready
    }
    is(DecoderState.ZERO) {
      when (io.in.bits > 1.U && io.in.valid) {
        // go the the DUMP state, and cache the last value
        rle_cntr := io.in.bits - 1.U
        state := DecoderState.REPEAT
      } .elsewhen (io.in.bits === 1.U && io.in.valid) {
        state := DecoderState.EMPTY
      }
      // no output during the FILL stage
      io.out.bits   := DontCare
      io.out.valid  := false.B
      io.in.ready   := false.B
    }
    is(DecoderState.REPEAT) {
      when (rle_cntr === 1.U) {
        // for the last value, go back to the EMPTY state
        state := DecoderState.EMPTY
        io.in.ready := true.B
      } .otherwise {
        // decrement over the rle counter
        rle_cntr := rle_cntr - 1.U
        io.in.ready := false.B
      }
      // send a zero to the output
      io.out.bits := 0.U
      io.out.valid := true.B
    }
  }

}
