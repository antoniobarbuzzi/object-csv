package com.gingersoftware.csv

import java.io._

import com.github.tototoshi.csv._

import scala.reflect.ClassTag
import scala.reflect.runtime.universe.TypeTag

case class CSVConfig(header: String = "#",
                     delimiter: Char = defaultCSVFormat.delimiter,
                     quoteChar: Char = defaultCSVFormat.quoteChar,
                     treatEmptyLineAsNil: Boolean = defaultCSVFormat.treatEmptyLineAsNil,
                     escapeChar: Char = defaultCSVFormat.escapeChar,
                     lineTerminator: String = defaultCSVFormat.lineTerminator,
                     quoting: Quoting = defaultCSVFormat.quoting) extends CSVFormat


/**
 * Created by dorony on 01/05/14.
 *
 * modified by Antonio on 03/12/2015
 */
object ObjectCSV {
  def apply(config: CSVConfig = new CSVConfig()) = new ObjectCSV(config)
}

class ObjectCSV(val config: CSVConfig) {
  def readCSV[T: TypeTag : ClassTag](is: InputStream): IndexedSeq[T] = {
    readCSV(new InputStreamReader(is))
  }

  def readCSV[T: TypeTag : ClassTag](reader: Reader): IndexedSeq[T] = {
    val csvReader = CSVReader.open(reader)(config)
    try {
      readCSV(csvReader)
    } finally {
      csvReader.close()
    }
  }

  def readCSV[T: TypeTag : ClassTag](file: File): IndexedSeq[T] = {
    val csvReader = CSVReader.open(file)(config)
    try {
      readCSV(csvReader)
    } finally {
      csvReader.close()
    }
  }

  private def readCSV[T: TypeTag : ClassTag](csvReader: CSVReader): IndexedSeq[T] = {
    val objectConverter = new ObjectConverter
    val data = csvReader.all()
    val header = data.head
    if (!header.head.startsWith(config.header)) {
      throw new Exception("Expected a commented out header. Found: " + header)
    }
    val headerWithoutComments = Array(header.head.substring(config.header.length)) ++ header.tail
    val objects = data.view.tail.map { row => objectConverter.toObject[T](row, headerWithoutComments) }
    objects.toVector
  }

  def writeCSVToString[T <: Product : TypeTag : ClassTag](objects: Seq[T]): String = {
    val stringWriter = new StringWriter()
    writeCSV(objects, stringWriter)
    stringWriter.toString
  }

  def writeCSV[T <: Product : TypeTag : ClassTag](objects: Seq[T], outputFile: File): Unit = {
    val fileWriter = new FileWriter(outputFile)
    try {
      writeCSV(objects, fileWriter)
    } finally {
      fileWriter.close()
    }
  }

  def writeCSV[T <: Product : TypeTag : ClassTag](objects: Seq[T], writer: Writer): Unit = {
    val converter = new ObjectConverter
    val header = converter.extractHeader[T]()
    val csvWriter = CSVWriter.open(writer)(config)
    csvWriter.writeRow(Seq(config.header + header.head) ++ header.tail)
    val rows = objects.view.map(o => converter.fromObject(o, header))
    rows.foreach(r => csvWriter.writeRow(r))
    csvWriter.flush()
    csvWriter.close()
  }
}
