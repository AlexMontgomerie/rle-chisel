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
  val bit_width = gen.width

  // check the rle_zero parameter
  assert(rle_zero >= 0, "The rle zero value must be greater than zero")
  assert(rle_zero < 256, "The rle zero value must be greater than zero")

  // intialise io definition
  val io = IO(new EncoderIO(gen))

  // create the states for rle
  val state = RegInit(EncoderState.EMPTY)

  // create registers to store incoming values and the rle counter
  val rle_cntr = RegInit(0.U)

  // create the finite state machine
  switch (state) {
    is(EncoderState.EMPTY) {
      when (io.in.bits =/= rle_zero.U && io.in.valid) {
        // pass value straight through if it's not an rle zero
        io.out.bits   := io.in.bits
        io.out.valid  := io.in.valid
        state := EncoderState.EMPTY
      } .elsewhen ( io.in.bits === rle_zero.U && io.in.valid ) {
        // move to the FILL state if the incoming value is an rle
        // zero, and update the rle counter
        io.out.bits   := DontCare
        io.out.valid  := false.B
        state := EncoderState.Fill
        rle_cntr := rle_cntr + 1.U
      } .otherwise {
        // otherwise, stay in the EMPTY state
        io.out.bits   := DontCare
        io.out.valid  := false.B
        state := EncoderState.EMPTY
      }
      // connect the ready signal straight through
      io.in.ready := io.out.ready
    }
    is(EncoderState.FILL) {
      when (io.in.bits =/= rle_zero.U && io.in.valid) {
        // go the the DUMP state, and cache the last value
        io.out.bits   := DontCare
        io.out.valid  := false.B
        io.in.ready   := false.B
        state := DUMP
      } .elsewhen ( io.in.bits === rle_zero.U && io.in.valid ) {
        // keep in current state, and increment the rle counter
        io.out.bits   := DontCare
        io.out.valid  := false.B
        io.in.ready   := true.B
        state := EncoderState.Fill
        rle_cntr := rle_cntr + 1.U
      } .otherwise {
        // stay in the FILL state
      }
    }
    is(EncoderState.DUMP) {

    }
    is(EncoderState.OVERFLOW) {

    }
  }

}
