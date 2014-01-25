package geotrellis.spark.rdd
import geotrellis.spark.formats.TileIdWritable
import geotrellis.spark.utils._

import org.apache.commons.codec.binary.Base64
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FSDataInputStream
import org.apache.hadoop.fs.LocalFileSystem
import org.apache.hadoop.fs.Path

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.InputStreamReader
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.PrintWriter
import java.nio.ByteBuffer
import java.util.Scanner

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer

class TileIdPartitioner extends org.apache.spark.Partitioner {

  @transient
  private var splitPoints = new Array[TileIdWritable](0)

  override def getPartition(key: Any) = findPartition(key)
  override def numPartitions = splitPoints.length + 1
  override def toString = "TileIdPartitioner split points: " + {
    if (splitPoints.isEmpty) "Empty" else splitPoints.zipWithIndex.mkString
  }

  // TODO override equals and hashCode

  private def findPartition(key: Any) = {
    val index = java.util.Arrays.binarySearch(splitPoints.asInstanceOf[Array[Object]], key)
    if (index < 0)
      (index + 1) * -1
    else
      index
  }

  private def writeObject(out: ObjectOutputStream) {
    out.defaultWriteObject()
    out.writeInt(splitPoints.length)
    splitPoints.foreach(split => out.writeLong(split.get))
  }

  private def readObject(in: ObjectInputStream) {
    in.defaultReadObject()
    val buf = new ArrayBuffer[TileIdWritable]
    val len = in.readInt
    for (i <- 0 until len)
      buf += TileIdWritable(in.readLong())
    splitPoints = buf.toArray
  }
}

object TileIdPartitioner {
  val SplitFile = "splits"

  /* construct a partitioner from the splits file, if one exists */
  def apply(rasterPath: Path, conf: Configuration): TileIdPartitioner = {
    val tp = new TileIdPartitioner
    tp.splitPoints = readSplits(rasterPath, conf)
    tp
  }

  /* construct a partitioner from the splits file, if one exists */
  def apply(rasterPath: String, conf: Configuration): TileIdPartitioner = {
    apply(new Path(rasterPath), conf)
  }

  /* construct a partitioner from a split generator */
  def apply(splitGenerator: SplitGenerator, rasterPath: Path, conf: Configuration): TileIdPartitioner = {
    writeSplits(splitGenerator, rasterPath, conf)
    apply(rasterPath, conf)
  }

  private def readSplits(rasterPath: Path, conf: Configuration): Array[TileIdWritable] = {
    val splitFile = new Path(rasterPath, SplitFile)
    HdfsUtils.getLineScanner(splitFile, conf) match {
      case Some(in) =>
        try {
          val splitPoints = new ListBuffer[TileIdWritable]
          for (line <- in) {
            splitPoints +=
              TileIdWritable(ByteBuffer.wrap(Base64.decodeBase64(line.getBytes)).getLong)
          }
          splitPoints.toArray
        } finally {
          in.close
        }
      case None =>
        Array[TileIdWritable]()
    }
  }

  private def writeSplits(splitGenerator: SplitGenerator, rasterPath: Path, conf: Configuration): Int = {
    val splits = splitGenerator.getSplits
    val splitFile = new Path(rasterPath, SplitFile)
    println("writing splits to " + splitFile)
    val fs = splitFile.getFileSystem(conf)
    val fdos = fs.create(splitFile)
    val out = new PrintWriter(fdos)
    splits.foreach {
      split => out.println(new String(Base64.encodeBase64(ByteBuffer.allocate(8).putLong(split).array())))
    }
    out.close()
    fdos.close()

    splits.length
  }

  def printSplits(rasterPath: Path, conf: Configuration) {
    val splits = readSplits(rasterPath, conf)
    splits.zipWithIndex.foreach(t => println("Split #%d: %d".format(t._2, t._1.get)))
  }
}

