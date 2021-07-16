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

  // function to run tests
  def run_test (seq_in: Seq[UInt], seq_out: Seq[UInt], description: String) {

    // testing annotations
    val annotations = Seq(VerilatorBackendAnnotation,WriteVcdAnnotation)

    behavior of "Encoder"
    it should s"be correct for $description (Encoder)" in {
      // create the DUT
      test(new Encoder(UInt(8.W), 0)).withAnnotations(annotations) { c =>

        // convert to stream interfaces to StreamDriver
        val in  = new StreamDriver(c.io.in)
        val out = new StreamDriver(c.io.out)

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

  // create test sequences
  var description = "a stream with no zeros"
  var seq_in = Seq(1.U, 2.U, 3.U, 4.U, 5.U, 6.U, 7.U, 8.U, 9.U)
  var seq_out = Seq(1.U, 2.U, 3.U, 4.U, 5.U, 6.U, 7.U, 8.U, 9.U)
  run_test(seq_in, seq_out, description)

  description = "a stream with both zeros and non-zeros"
  seq_in = Seq(1.U, 2.U, 0.U, 0.U, 0.U, 0.U, 7.U, 8.U, 9.U)
  seq_out = Seq(1.U, 2.U, 0.U, 4.U, 7.U, 8.U, 9.U)
  run_test(seq_in, seq_out, description)

  description = "a stream with a single zero"
  seq_in = Seq(1.U, 2.U, 0.U, 7.U, 8.U, 9.U)
  seq_out = Seq(1.U, 2.U, 0.U, 1.U, 7.U, 8.U, 9.U)
  run_test(seq_in, seq_out, description)

  description = "a stream with a single zero at the end"
  seq_in = Seq(1.U, 2.U, 7.U, 8.U, 9.U, 0.U)
  seq_out = Seq(1.U, 2.U, 7.U, 8.U, 9.U, 0.U, 1.U)
  run_test(seq_in, seq_out, description)

}

