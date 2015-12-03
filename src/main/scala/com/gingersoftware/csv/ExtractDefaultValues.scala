package com.gingersoftware.csv

import java.util.concurrent.ConcurrentHashMap

private[csv] object ExtractDefaultValues {
  private val cache = new ConcurrentHashMap[reflect.ClassTag[_], Map[String, Any]]()

  /*
   * Scala does not offer an api to easily retrieve default values, but, according to the Scala spec at 4.6, 6.6.1.
   *
   * "For every parameter pi, j with a default argument a method named f$default$n is generated which computes the default argument expression"
   *
   * Therefore, we access the companion object of the case class, retrieve the parameters list from the apply method, then
   * for each parameter at index i, we call the method "apply$default$i" to get the default value.
   *
   * The approach is documented here:
   * http://stackoverflow.com/questions/14034142/how-do-i-access-default-parameter-values-via-scala-reflection
   */
  private def get[A](implicit t: reflect.ClassTag[A]): Map[String, Any] = {
    import reflect.runtime.{currentMirror => cm, universe => ru}
    val clazz = cm.classSymbol(t.runtimeClass)
    val mod = clazz.companion.asModule
    val im = cm.reflect(cm.reflectModule(mod).instance)
    val ts = im.symbol.typeSignature
    val mApply = ts.member(ru.TermName("apply")).asMethod
    val syms = mApply.paramLists.flatten

    val defaultValues = syms.zipWithIndex.map { case (p, i) =>
      val mDef = ts.member(ru.TermName(s"apply$$default$$${i + 1}"))
      if (mDef != ru.NoSymbol) {
        p.name.decodedName.toString -> im.reflectMethod(mDef.asMethod)()
      } else {
        p.name.decodedName.toString -> null
      }
    }.filter(_._2 != null).toMap
    defaultValues
  }

  def apply[A](implicit t: reflect.ClassTag[A]): Map[String, Any] = {
    if (!cache.containsKey(t)) {
      val defaults = get[A]
      cache.put(t, defaults)
    }
    cache.get(t)
  }
}
