# RLE Chisel

![build](https://github.com/AlexMontgomerie/rle-chisel/actions/workflows/test.yml/badge.svg)

A Chisel implementation of single-value Run Length Encoding (RLE). This module operates on a 
modified `Decoupled` interface, which includes an additional `last` signal. 
