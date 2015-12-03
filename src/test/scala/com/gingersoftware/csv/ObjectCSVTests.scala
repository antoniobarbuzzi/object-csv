package com.gingersoftware.csv

import java.io.{File, StringWriter}

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}
/**
 * Created by dorony on 01/05/14.
 *
 * modified by Antonio on 03/12/2015
 */

@RunWith(classOf[JUnitRunner])
class ObjectCSVTests extends FlatSpec with Matchers {
  "ObjectCSV" should "write and read a csv file" in {
    val file = File.createTempFile("test", ".csv")
    try {
      val person1 = new Person("Doron,y\",\"", 10, 5.5, 1l, false, BigDecimal(10))
      val person2 = new Person("David", 20, 7.5, 2l, true, BigDecimal("13.3"), optString = Some(",ëçûæ,ëçû\"æëçûæëçûæëçûæëçûæɧɧɧɧɧࢠ"), optInt = Some(3))

      //Write
      val objectCsv = ObjectCSV()
      objectCsv.writeCSV(IndexedSeq(person1, person2), file)

      //Read
      val peopleFromCSV = objectCsv.readCSV[Person](file)
      peopleFromCSV should be(IndexedSeq(person1, person2))

      val source = scala.io.Source.fromFile(file)
      val lines = source.getLines().toList
      println(lines)
      lines.head should be(objectCsv.config.header + ObjectConverterTest.personHeader.mkString(","))
      lines.length should be(3)
    } finally {
      file.delete()
    }
  }

  it should "customize header, delimiter, lineTerminator" in {
    val person = new Person("Marty", 10, 5.5, 1l, false, BigDecimal(10))
    val stringWriter = new StringWriter
    ObjectCSV(CSVConfig(header = "", delimiter = ':', lineTerminator = "$")).writeCSV(IndexedSeq(person), stringWriter)
    val lines = stringWriter.toString.split("\\$")
    lines.length should be(2)
    lines(0).startsWith("name") should be(true)
    lines(0).count(c => c == ':') should be(11)
    lines(1).count(c => c == ':') should be(11)
  }
}
