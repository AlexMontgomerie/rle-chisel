package rle.encoder_test

import rle.Encoder
import rle.test.StreamDriver

import org.scalatest._

import chisel3._
import chiseltest._
import chisel3.util._

import chiseltest.experimental.TestOptionBuilder._
import chiseltest.internal.VerilatorBackendAnnotation
import chiseltest.internal.WriteVcdAnnotation

class EncoderTest extends FlatSpec with ChiselScalatestTester with Matchers {

  // testing annotations
  val annotations = Seq(VerilatorBackendAnnotation,WriteVcdAnnotation)

  behavior of "Encoder"
  it should "be correct for a stream with no zeros" in {
    // create the DUT
    test(new Encoder(UInt(8.W), 0)).withAnnotations(annotations) { c =>

      // convert to stream interfaces to StreamDriver
      val in  = new StreamDriver(c.io.in)
      val out = new StreamDriver(c.io.out)

      // create test sequences
      val seq_in = Seq(1.U, 2.U, 3.U, 4.U, 5.U, 6.U, 7.U, 8.U, 9.U)
      val seq_out = Seq(1.U, 2.U, 3.U, 4.U, 5.U, 6.U, 7.U, 8.U, 9.U)

      // module setup
      in.initSource().setSourceClock(c.clock)
      out.initSink().setSinkClock(c.clock)

      // run the sequences
      parallel(
        in.enqueueSeq(seq_in),
        out.expectDequeueSeq(seq_out)
      )

    }
  }

  behavior of "Encoder"
  it should "be correct for a stream with both zeros and non-zeros" in {
    // create the DUT
    test(new Encoder(UInt(8.W), 0)).withAnnotations(annotations) { c =>

      // convert to stream interfaces to StreamDriver
      val in  = new StreamDriver(c.io.in)
      val out = new StreamDriver(c.io.out)

      // create test sequences
      val seq_in = Seq(1.U, 2.U, 0.U, 0.U, 0.U, 0.U, 7.U, 8.U, 9.U)
      val seq_out = Seq(1.U, 2.U, 0.U, 4.U, 7.U, 8.U, 9.U)

      // module setup
      in.initSource().setSourceClock(c.clock)
      out.initSink().setSinkClock(c.clock)

      // run the sequences
      parallel(
        in.enqueueSeq(seq_in),
        out.expectDequeueSeq(seq_out)
      )

    }
  }


}

