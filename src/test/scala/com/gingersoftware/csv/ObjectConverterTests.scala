package com.gingersoftware.csv

/**
 * Created by dorony on 01/05/14.
 */

import java.time.{LocalDateTime, LocalDate}

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

object ObjectConverterTest {
  val personHeader = Array("name", "age", "salary", "userID", "isNice", "money", "dateOfBirth", "alarmTime", "optString", "optInt", "optDouble", "optLong", "optBoolean", "optBigDecimal", "optDate", "optTime")
}

@RunWith(classOf[JUnitRunner])
class ObjectConverterTest extends FlatSpec with Matchers {
  val converter = new ObjectConverter()
  private val defaultDate = LocalDate.of(1999, 12, 31)
  private val defaultTime = LocalDateTime.of(2015, 12, 7, 11, 41, 21, 818000000)

  "ObjectConverter.toObject" should "instantiates the object correctly" in {
    val person = converter.toObject[Person](Array("Doron","30","25.5","123", "true", "2.718", "1999-12-31", "2015-12-07T11:41:21.818"), ObjectConverterTest.personHeader)
    val expected = Person("Doron", 30, 25.5, 123l, true, money = BigDecimal("2.718"), defaultDate, defaultTime)
    person should be(expected)
  }

  it should "handle optional values" in {
    val person = converter.toObject[Person](Array("Doron","30","25.5","123", "true", "2.718", "1999-12-31", "2015-12-07T11:41:21.818", "optString", "3", "2.2", "9", "true", "5555.12", "1999-12-31", "2015-12-07T11:41:21.818"), ObjectConverterTest.personHeader)
    val expected = Person("Doron", 30, 25.5, 123l, true, money = BigDecimal("2.718"), defaultDate, defaultTime, Some("optString"), Some(3), Some(2.2), Some(9l), Some(true), Some(BigDecimal("5555.12")), Some(defaultDate), Some(defaultTime))
    person should be(expected)
  }

  it should "replace missing values with default values" in {
    val person = converter.toObject[PersonWithDefaults](Array("Doron"), Array("name"))
    person should be(PersonWithDefaults("Doron"))
  }

  it should "set missing optional value to None" in {
    val person = converter.toObject[PersonWithNonDefaultOption](Array("Doron"), Array("name"))
    person should be(PersonWithNonDefaultOption("Doron", None))
  }

  it should "fail if a non optional and without default parameter is missing" in {
    intercept[IllegalArgumentException] {
      converter.toObject[Person](Array("Doron"), Array("name"))
    }
  }

  it should "convert snake_case header into camelCase" in {
    val snakeCaseHeader = Array("name", "age", "salary", "userID", "is_nice", "money", "date_of_birth", "alarm_time", "opt_string", "opt_int", "opt_double", "opt_long", "opt_boolean", "opt_big_decimal", "opt_date", "opt_time")
    val person = converter.toObject[Person](Array("Doron","30","25.5","123", "true", "2.718", "1999-12-31", "2015-12-07T11:41:21.818", "optString", "3", "2.2", "9", "true", "5555.12", "1999-12-31", "2015-12-07T11:41:21.818"), snakeCaseHeader)
    val expected = Person("Doron", 30, 25.5, 123l, true, money = BigDecimal("2.718"), defaultDate, defaultTime, Some("optString"), Some(3), Some(2.2), Some(9l), Some(true), Some(BigDecimal("5555.12")),  Some(defaultDate), Some(defaultTime))
    person should be(expected)
  }

  it should "correctly convert strings with commas and quotes" in {
    def convertShouldMatch(name: String) {
      val person = converter.toObject[PersonWithNonDefaultOption](Array(name), Array("name"))
      person should be(PersonWithNonDefaultOption(name, None))
    }
    val tests = Seq("com, ma", "two,comma,s", ",", ",\"", ",\"\"", ",\",\"")
    tests.foreach(name => convertShouldMatch(name))
  }

  it should "throw if type has no constructor" in {
    intercept[IllegalArgumentException] {
      converter.toObject[Product](Array("Doron","30","25.5","true"), Array("name","age","salary","isNice"))
    }
  }

  "ObjectConverter.fromObject" should "follow the order of the header when converting" in {
    val person = Person("Doron", 30, 25.5, 123l, true, money = BigDecimal("2.718"), defaultDate, defaultTime)
    val result = converter.fromObject(person, Array("salary", "name", "age", "money", "userID"))
    result should be(IndexedSeq("25.5", "Doron", "30", "2.718", "123"))
  }

  it should "convert empty options to empty string" in {
    val person = Person("Doron", 30, 25.5, 123l, true, money = BigDecimal("2.718"), defaultDate, defaultTime)
    val result = converter.fromObject(person, ObjectConverterTest.personHeader)
    result should be(IndexedSeq("Doron", "30", "25.5", "123", "true", "2.718", "1999-12-31", "2015-12-07T11:41:21.818", "", "", "", "", "", "", "", ""))
  }

  it should "convert not empty options to the values of the objects" in {
    val person = Person("Doron", 30, 25.5, 123l, true, money = BigDecimal("2.718"), defaultDate, defaultTime, optString = Some("not-empty"), optInt = Some(2), optBigDecimal = Some(BigDecimal("3.12")), optDate = Some(defaultDate), optTime = Some(defaultTime))
    val result = converter.fromObject(person, ObjectConverterTest.personHeader)
    result should be(IndexedSeq("Doron", "30", "25.5", "123", "true", "2.718", "1999-12-31", "2015-12-07T11:41:21.818", "not-empty", "2", "", "", "", "3.12", "1999-12-31", "2015-12-07T11:41:21.818"))
  }

  "ObjectConverter.getHeader" should "returns the ctor parameter names" in {
    assert(converter.extractHeader[Person] === IndexedSeq(ObjectConverterTest.personHeader: _*))
    assert(converter.extractHeader[PersonWithDefaults] === IndexedSeq(ObjectConverterTest.personHeader: _*))
    assert(converter.extractHeader[PersonWithNonDefaultOption] === IndexedSeq("name", "other"))
  }
}

case class Person(name: String,
                  age: Int,
                  salary: Double,
                  userID: Long,
                  isNice: Boolean,
                  money: BigDecimal,
                  dateOfBirth: LocalDate,
                  alarmTime: LocalDateTime,
                  optString: Option[String] = None,
                  optInt: Option[Int] = None,
                  optDouble: Option[Double] = None,
                  optLong: Option[Long] = None,
                  optBoolean: Option[Boolean] = None,
                  optBigDecimal: Option[BigDecimal] = None,
                  optDate: Option[LocalDate] = None,
                  optTime: Option[LocalDateTime] = None)


case class PersonWithDefaults(name: String,
                              age: Int = 10,
                              salary: Double = 10000,
                              userID: Long = 0l,
                              isNice: Boolean = false,
                              money: BigDecimal = BigDecimal("3.3"),
                              dateOfBirth: LocalDate = LocalDate.of(2000, 1, 2),
                              alarmTime: LocalDateTime = LocalDateTime.of(2000, 1, 2, 13, 12, 23, 3213),
                              optString: Option[String] = Some("a default value"),
                              optInt: Option[Int] = None,
                              optDouble: Option[Double] = None,
                              optLong: Option[Long] = None,
                              optBoolean: Option[Boolean] = None,
                              optBigDecimal: Option[BigDecimal] = None,
                              optDate: Option[LocalDate] = None,
                              optTime: Option[LocalDateTime] = None)

case class PersonWithNonDefaultOption(name: String, other: Option[String])
