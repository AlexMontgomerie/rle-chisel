package rle

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum

class EncoderIO[T <: UInt](gen: T) extends Bundle {
  val in = Flipped(Decoupled(gen))
  val out = Decoupled(gen)
}

object EncoderState extends ChiselEnum {
  val EMPTY, FILL, DUMP, OVERFLOW = Value
}

class Encoder[T <: UInt](gen: T, rle_zero: Int) extends Module {

  // get the width of the UInt type
  val bit_width = gen.getWidth

  // check the rle_zero parameter
  assert(rle_zero >= 0, "The rle zero value must be greater than zero")
  assert(rle_zero < scala.math.pow(2,bit_width), "The rle zero value must be greater than zero")

  // intialise io definition
  val io = IO(new EncoderIO(gen))

  // create the states for rle
  val state = RegInit(EncoderState.EMPTY)

  // create registers to store incoming values and the rle counter
  val rle_cntr = RegInit(0.U(bit_width.W))

  // set defaults
  io.out.bits := DontCare
  io.out.valid := false.B
  io.in.ready := false.B

  // create the finite state machine
  switch (state) {
    is(EncoderState.EMPTY) {
      when (io.in.bits =/= rle_zero.U && io.in.valid) {
        // pass value straight through if it's not an rle zero
        state := EncoderState.EMPTY
      } .elsewhen ( io.in.bits === rle_zero.U && io.in.valid ) {
        // move to the FILL state if the incoming value is an rle
        // zero, and update the rle counter
        state := EncoderState.FILL
        rle_cntr := rle_cntr + 1.U
      }
      // connect the ready signal straight through, and the
      // input directly to output
      io.out.bits   := io.in.bits
      io.out.valid  := io.in.valid
      io.in.ready   := io.out.ready
    }
    is(EncoderState.FILL) {
      when (io.in.bits =/= rle_zero.U && io.in.valid) {
        // go the the DUMP state, and cache the last value
        io.in.ready   := false.B
        state := EncoderState.DUMP
      } .elsewhen ( io.in.bits === rle_zero.U && io.in.valid ) {
        // keep in current state, and increment the rle counter
        io.in.ready   := true.B
        state := EncoderState.FILL
        rle_cntr := rle_cntr + 1.U
      }
      // no output during the FILL stage
      io.out.bits   := DontCare
      io.out.valid  := false.B
   }
    is(EncoderState.DUMP) {
      // pass the total number of zeros to the output, and
      // go to the output state
      io.out.bits := rle_cntr
      io.out.valid := true.B
      io.in.ready := false.B
      state := EncoderState.EMPTY
    }
    is(EncoderState.OVERFLOW) {
      // TODO: handle case
    }
  }

}