/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.spark.sql.execution

import java.io._
import java.nio.file.{Path, StandardOpenOption, Files}
import java.util.{ArrayList => JavaArrayList}

import org.apache.spark.SparkException
import org.apache.spark.sql.catalyst.expressions.{Projection, Row}
import org.apache.spark.sql.execution.CS143Utils._

import scala.collection.JavaConverters._

/**
  * This trait represents a regular relation that is hash partitioned and spilled to
  * disk.
  */
private[sql] sealed trait DiskHashedRelation {
  /**
    *
    * @return an iterator of the [[DiskPartition]]s that make up this relation.
    */
  def getIterator(): Iterator[DiskPartition]

  /**
    * Close all the partitions for this relation. This should involve deleting the files hashed into.
    */
  def closeAllPartitions()
}

/**
  * A general implementation of [[DiskHashedRelation]].
  *
  * @param partitions the disk partitions that we are going to spill to
  */
protected [sql] final class GeneralDiskHashedRelation(partitions: Array[DiskPartition])
  extends DiskHashedRelation with Serializable {

  override def getIterator() = {
    /* IMPLEMENT THIS METHOD */
    //weijia task2
    if (partitions.length > 0)
    {
      partitions.iterator
    }
    else
    {
      null
    }
  // null
  }

  override def closeAllPartitions() = {
    /* IMPLEMENT THIS METHOD */
    //weijia task2
    for (diskPartition <- partitions)
    {
      diskPartition.closePartition()
    }
    // null
  }
}

private[sql] class DiskPartition (
                                   filename: String,
                                   blockSize: Int) {
  private val path: Path = Files.createTempFile("", filename)
  private val data: JavaArrayList[Row] = new JavaArrayList[Row]
  private val outStream: OutputStream = Files.newOutputStream(path)
  private val inStream: InputStream = Files.newInputStream(path)
  private val chunkSizes: JavaArrayList[Int] = new JavaArrayList[Int]()
  private var writtenToDisk: Boolean = false
  private var inputClosed: Boolean = false

  /**
    * This method inserts a new row into this particular partition. If the size of the partition
    * exceeds the blockSize, the partition is spilled to disk.
    *
    * @param row the [[Row]] we are adding
    */
  def insert(row: Row) = {
    /* IMPLEMENT THIS METHOD */
    //weijia task1
    val ifinputclose: Boolean = inputClosed
    if (!ifinputclose){
      if (this.measurePartitionSize() > this.blockSize)
      {
        this.spillPartitionToDisk()
        //clear the data
        this.data.clear()
      }
      this.data.add(row)
      this.writtenToDisk = false
    }

    if (ifinputclose)
    {
      throw new SparkException("Error: Input closed")
    }
  }

  /**
    * This method converts the data to a byte array and returns the size of the byte array
    * as an estimation of the size of the partition.
    *
    * @return the estimated size of the data
    */
  private[this] def measurePartitionSize(): Int = {
    CS143Utils.getBytesFromList(data).size
  }

  /**
    * Uses the [[Files]] API to write a byte array representing data to a file.
    */
  private[this] def spillPartitionToDisk() = {
    val bytes: Array[Byte] = getBytesFromList(data)

    // This array list stores the sizes of chunks written in order to read them back correctly.
    chunkSizes.add(bytes.size)

    Files.write(path, bytes, StandardOpenOption.APPEND)
    writtenToDisk = true
  }

  /**
    * If this partition has been closed, this method returns an Iterator of all the
    * data that was written to disk by this partition.
    *
    * @return the [[Iterator]] of the data
    */
  def getData(): Iterator[Row] = {
    if (!inputClosed) {
      throw new SparkException("Should not be reading from file before closing input. Bad things will happen!")
    }

    new Iterator[Row] {
      var currentIterator: Iterator[Row] = data.iterator.asScala
      val chunkSizeIterator: Iterator[Int] = chunkSizes.iterator().asScala
      var byteArray: Array[Byte] = null

      override def next() = {
        /* IMPLEMENT THIS METHOD */
        //weijia task1
        if (currentIterator.hasNext)
        {
          currentIterator.next()
        }
        else
        {
          null
        }
      }

      override def hasNext() = {
        /* IMPLEMENT THIS METHOD */
        //weijia task1
        if (currentIterator.hasNext){
          true
        }
        else{
          fetchNextChunk()
        }
      }

      /**
        * Fetches the next chunk of the file and updates the iterator. Should return true
        * unless the iterator is empty.
        *
        * @return true unless the iterator is empty.
        */
      private[this] def fetchNextChunk(): Boolean = {
        /* IMPLEMENT THIS METHOD */
        //weijia task1
        if (!chunkSizeIterator.hasNext){
          false
        }
        else
        {
          val fetchSize = chunkSizeIterator.next()
          byteArray = CS143Utils.getNextChunkBytes(inStream, fetchSize, byteArray)
          currentIterator = CS143Utils.getListFromBytes(byteArray).iterator.asScala
          true
        }

      }
    }
  }

  /**
    * Closes this partition, implying that no more data will be written to this partition. If getData()
    * is called without closing the partition, an error will be thrown.
    *
    * If any data has not been written to disk yet, it should be written. The output stream should
    * also be closed.
    */
  def closeInput() = {
    /* IMPLEMENT THIS METHOD */
    //weijia task1
    var bA: Array[Byte] = null
    if (!writtenToDisk)
    {
      
      if (data.size() <= 0)
      {
        writtenToDisk = true
        outStream.close()
      }  
      else{
        //write data
        this.spillPartitionToDisk()
        this.data.clear()
        outStream.close()
      }
    }
    inputClosed = true
  }


  /**
    * Closes this partition. This closes the input stream and deletes the file backing the partition.
    */
  private[sql] def closePartition() = {
    inStream.close()
    Files.deleteIfExists(path)
  }
}

private[sql] object DiskHashedRelation {

  /**
    * Given an input iterator, partitions each row into one of a number of [[DiskPartition]]s
    * and constructors a [[DiskHashedRelation]].
    *
    * This executes the first phase of external hashing -- using a course-grained hash function
    * to partition the tuples to disk.
    *
    * The block size is approximately set to 64k because that is a good estimate of the average
    * buffer page.
    *
    * @param input the input [[Iterator]] of [[Row]]s
    * @param keyGenerator a [[Projection]] that generates the keys for the input
    * @param size the number of [[DiskPartition]]s
    * @param blockSize the threshold at which each partition will spill
    * @return the constructed [[DiskHashedRelation]]
    */
    var diskArray: Array[DiskPartition] = new Array[DiskPartition](64)

  def apply (
              input: Iterator[Row],
              keyGenerator: Projection,
              size: Int = 64,
              blockSize: Int = 64000) = {
    /* IMPLEMENT THIS METHOD */
    //weijia task2
    var i = 0
    while (i < diskArray.length){
      val filename: String = Integer.toString(i)
      diskArray(i) = new DiskPartition(filename, blockSize)
      i += 1
    }
    
    if (input.hasNext){
    	do{
      val row = input.next()
      // keyGenerator a [[Projection]] that generates the keys for the input
      val generater = keyGenerator.apply(row)
      val hashcode = generater.hashCode() 
      // size the number of [[DiskPartition]]s
      val index = hashcode % size
      diskArray(index).insert(row)
    }while (input.hasNext)
    }

    var j = 0
    while (j < diskArray.length){
      diskArray(j).closeInput()
      j += 1
    }
    //the constructed [[DiskHashedRelation]]
    val DiskHashRelation: GeneralDiskHashedRelation = new GeneralDiskHashedRelation(diskArray)
    DiskHashRelation
  }
}
