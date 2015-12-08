package com.gingersoftware.csv

import java.time.{LocalDate, LocalDateTime}

import scala.reflect.ClassTag
import scala.reflect.runtime.{universe => ru}

/**
 * Created by dorony on 30/04/14.
 *
 * modified by Antonio on 03/12/2015
 */

import java.util.concurrent.ConcurrentHashMap

import scala.reflect.runtime.{universe => ru}


private[csv] class ObjectConverter {
  private val cache = new ConcurrentHashMap[ru.TypeTag[_], (ru.MethodMirror, List[(String, ru.Type, Option[Any])])]()

  def toObject[T: ru.TypeTag](data: Seq[String], header: Seq[String]): T = {
    val propertyToValue = header.view.map(h => underscoreToCamel(h)).zip(data).toMap
    val (ctor: ru.MethodMirror, parametersAndType) = extractCtorAndParameters[T](ru.typeTag[T])
    val args = parametersAndType.map { case (paramName, paramType, default) => parseValue(paramName, paramType, propertyToValue, default) }

    ctor(args: _*).asInstanceOf[T]
  }

  private def extractCtorAndParameters[T: ru.TypeTag](t: ru.TypeTag[T]): (ru.MethodMirror, List[(String, ru.Type, Option[Any])]) = {
    if (!cache.containsKey(t)) {
      cache.put(t, doExtractCtorAndParameters[T]())
    }
    cache.get(t)
  }

  private def doExtractCtorAndParameters[T: ru.TypeTag](): (ru.MethodMirror, List[(String, ru.Type, Option[Any])]) = {
    val runtimeMirror = ru.runtimeMirror(getClass.getClassLoader)
    val typeObj = ru.typeOf[T]
    val outputClass = typeObj.typeSymbol.asClass
    val cm = runtimeMirror.reflectClass(outputClass)
    val ctorSymbol = typeObj.decl(ru.termNames.CONSTRUCTOR)
    if (ctorSymbol == ru.NoSymbol)
      throw new IllegalArgumentException(s"type $typeObj should have a primary constructor but doesn't")
    val ctor = ctorSymbol.asMethod

    val defaults: Map[String, Any] = ExtractDefaultValues[T](typeToClassTag[T])
    val parametersAndType = ctor.paramLists.head.map { p =>
      val pname = p.name.decodedName.toString
      (pname, p.typeSignature, defaults.get(pname))
    }

    val ctorm: ru.MethodMirror = cm.reflectConstructor(ctor)
    (ctorm, parametersAndType)
  }

  private def parseValue(paramName: String, paramType: ru.Type, propertyToValue: Map[String, String], default: Option[Any]): Any = {
    propertyToValue.get(paramName) match {
      case Some(v) => convert(v, paramType)
      case None if default.isDefined => default.get
      case None if paramType.erasure =:= ru.typeOf[Option[Any]] => None
      case None => throw new IllegalArgumentException(s"Missing non optional value $paramName")
    }
  }

  private def convert(v: String, t: ru.Type): Any = {
    t match {
      case _ if t =:= ru.typeOf[String] => v
      case _ if t =:= ru.typeOf[Int] => v.toInt
      case _ if t =:= ru.typeOf[Double] => v.toDouble
      case _ if t =:= ru.typeOf[Boolean] => v.toBoolean
      case _ if t =:= ru.typeOf[Long] => v.toLong
      case _ if t =:= ru.typeOf[BigDecimal] => BigDecimal(v)
      case _ if t =:= ru.typeOf[LocalDateTime] => LocalDateTime.parse(v)
      case _ if t =:= ru.typeOf[LocalDate] => LocalDate.parse(v)

      //Converting empty string to None instead of Some(emptyString)
      case _ if t =:= ru.typeOf[Option[String]] && v.isEmpty => None

      case _ if t =:= ru.typeOf[Option[String]] => Option(v)
      case _ if t =:= ru.typeOf[Option[Int]] => if (isNullOrEmpty(v)) None else Some(v.toInt)
      case _ if t =:= ru.typeOf[Option[Double]] => if (isNullOrEmpty(v)) None else Some(v.toDouble)
      case _ if t =:= ru.typeOf[Option[Boolean]] => if (isNullOrEmpty(v)) None else Some(v.toBoolean)
      case _ if t =:= ru.typeOf[Option[Long]] => if (isNullOrEmpty(v)) None else Some(v.toLong)
      case _ if t =:= ru.typeOf[Option[BigDecimal]] => if (isNullOrEmpty(v)) None else Some(BigDecimal(v))
      case _ if t =:= ru.typeOf[Option[LocalDateTime]] => if (isNullOrEmpty(v)) None else Some(LocalDateTime.parse(v))
      case _ if t =:= ru.typeOf[Option[LocalDate]] => if (isNullOrEmpty(v)) None else Some(LocalDate.parse(v))

      case _ => throw new IllegalStateException(s"Cannot convert $v to type $t")
    }
  }

  private def getCaseClassParams(cc: Product) = {
    val values = cc.productIterator
    cc.getClass.getDeclaredFields.map(_.getName -> values.next).toMap
  }

  private def isNullOrEmpty(s: String): Boolean = {
    s == null || s.isEmpty
  }

  private def underscoreToCamel(name: String) = "_([a-z\\d])".r.replaceAllIn(name, { m => m.group(1).toUpperCase })

  def fromObject(obj: Product, header: IndexedSeq[String]): IndexedSeq[String] = {
    val propToValue = getCaseClassParams(obj)
    header.map((h: String) => {
      val value: Option[Any] = propToValue.get(h)
      if (value.isEmpty || value.get == null) {
        ""
      } else {
        value.get match {
          case Some(v) => v.toString
          case None => ""
          case v => v.toString
        }
      }
    })
  }

  def extractHeader[T: ru.TypeTag](): IndexedSeq[String] = {
    val (_, parametersAndType) = extractCtorAndParameters[T](ru.typeTag[T])
    parametersAndType.view.map { case (name, pType, _) => name }.toVector
  }

  private def typeToClassTag[T: ru.TypeTag]: ClassTag[T] = {
    ClassTag[T](ru.typeTag[T].mirror.runtimeClass(ru.typeTag[T].tpe))
  }
}
