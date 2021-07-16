package rle

import rle.Stream

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum

class DecoderIO[T <: UInt](gen: T) extends Bundle {
  val in = Flipped(Stream(gen))
  val out = Stream(gen)
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

  // cache the last signal
  val last_cache = RegInit(false.B)

  // create registers to store incoming values and the rle counter
  val rle_cntr = RegInit(0.U(bit_width.W))

  // set defaults
  io.out.bits   := DontCare
  io.out.valid  := false.B
  io.out.last   := false.B
  io.in.ready   := false.B

  // create the finite state machine
  switch (state) {
    is(DecoderState.EMPTY) {
      when (io.in.bits =/= rle_zero.U && io.in.valid) {
        // pass value straight through if it's not an rle zero
        io.out.bits   := io.in.bits
        io.out.valid  := io.in.valid
        state := DecoderState.EMPTY
      } .elsewhen ( io.in.bits === rle_zero.U && io.in.valid ) {
        // move to the FILL state if the incoming value is an rle
        // zero, and update the rle counter
        io.out.bits   := DontCare
        io.out.valid  := false.B
        state := DecoderState.ZERO
      }
      // connect the ready signal straight through
      io.out.last   := io.in.last
      io.in.ready   := io.out.ready
      // cache the last signal
      last_cache := io.in.last
    }
    is(DecoderState.ZERO) {
      when (io.in.bits > 1.U && io.in.valid) {
        // go the the DUMP state, and cache the last value
        rle_cntr := io.in.bits - 1.U
        state := DecoderState.REPEAT
        io.out.last   := false.B
        io.in.ready   := false.B
      } .elsewhen (io.in.bits === 1.U && io.in.valid) {
        state := DecoderState.EMPTY
        io.out.last   := io.in.last
        io.in.ready   := true.B
      }
      // no output during the FILL stage
      io.out.bits   := rle_zero.U
      io.out.valid  := true.B
      // cache the last signal
      last_cache := io.in.last
    }
    is(DecoderState.REPEAT) {
      when (rle_cntr === 1.U) {
        // for the last value, go back to the EMPTY state
        state := DecoderState.EMPTY
        io.in.ready := true.B
        io.out.last := last_cache
      } .otherwise {
        // decrement over the rle counter
        rle_cntr := rle_cntr - 1.U
        io.in.ready := false.B
        io.out.last   := false.B
      }
      // send a zero to the output
      io.out.bits := rle_zero.U
      io.out.valid := true.B
    }
  }

}
