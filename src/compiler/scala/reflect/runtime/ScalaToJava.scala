package scala.reflect
package runtime

import java.lang.{Class => jClass, Package => jPackage}
import java.lang.reflect.{
  Method => jMethod, Constructor => jConstructor, Modifier => jModifier, Field => jField,
  Member => jMember, Type => jType, Array => jArray, GenericDeclaration}

trait ScalaToJava extends ConversionUtil { self: SymbolTable =>

  import definitions._

  /** Optionally, the Java package corresponding to a given Scala package, or None if no such Java package exists.
   *  @param   pkg The Scala package
   */
  def packageToJava(pkg: Symbol): Option[jPackage] = packageCache.toJavaOption(pkg) {
    Option(java.lang.Package.getPackage(pkg.fullName.ToString))
  }

  /** The Java class corresponding to given Scala class.
   *  Note: This only works for
   *   - top-level classes
   *   - Scala classes that were generated via jclassToScala
   *   - classes that have a class owner that has a corresponding Java class
   *  @throws A `ClassNotFoundException` for all Scala classes not in one of these categories.
   */
  //@throws(classOf[ClassNotFoundException])
  def classToJava(clazz: Symbol): jClass/*[_]*/ = classCache.toJava(clazz) {
    def noClass = throw new java.lang.ClassNotFoundException("no Java class corresponding to "+clazz+" found")
    //println("classToJava "+clazz+" "+clazz.owner+" "+clazz.owner.isPackageClass)//debug
    if (clazz.isValueClass)
      valueClassToJavaType(clazz)
    else if (clazz == ArrayClass)
      noClass
    else if (clazz.owner.isPackageClass)
      javaClass(clazz.javaClassName)
    else if (clazz.owner.isClass)
      classToJava(clazz.owner)
        .getDeclaredClasses
        .find(_.getSimpleName == clazz.name.ToString)
        .getOrElse(noClass)
    else
      noClass
  }

  private def expandedName(sym: Symbol): String =
    if (sym.isPrivate) nme.expandedName(sym.name.toTermName, sym.owner).ToString
    else sym.name.ToString

  def fieldToJava(fld: Symbol): jField = fieldCache.toJava(fld) {
    val jclazz = classToJava(fld.owner)
    try jclazz getDeclaredField fld.name.ToString
    catch {
      case ex: /* MANUALLY */ java.lang.NoSuchFieldException => jclazz getDeclaredField expandedName(fld)
    }
  }

  def methodToJava(meth: Symbol): jMethod = methodCache.toJava(meth) {
    val jclazz = classToJava(meth.owner)
    val paramClasses = transformedType(meth).paramTypes map typeToJavaClass
    try jclazz getDeclaredMethod (meth.name.ToString, scala.Array( paramClasses: _* ) )
    catch {
      case ex: java.lang.NoSuchMethodException =>
        jclazz getDeclaredMethod (expandedName(meth), scala.Array( paramClasses: _* ) )
    }
  }

  def constrToJava(constr: Symbol): jConstructor/*[_]*/ = constructorCache.toJava(constr) {
    val jclazz = classToJava(constr.owner)
    val paramClasses = transformedType(constr).paramTypes map typeToJavaClass
    jclazz getConstructor (scala.Array( paramClasses: _* ) )
  }

  private def jArrayClass(elemClazz: jClass/*[_]*/): jClass/*[_]*/ = {
    _root_.java.lang.Object.instancehelper_getClass(jArray.newInstance(elemClazz, 0))
  }

  /** The Java class that corresponds to given Scala type.
   *  Pre: Scala type is already transformed to Java level.
   */
  def typeToJavaClass(tpe: Type): jClass/*[_]*/ = tpe match {
    case ExistentialType(_, rtpe) => typeToJavaClass(rtpe)
    case TypeRef(_, ArrayClass, List(elemtpe)) => jArrayClass(typeToJavaClass(elemtpe))
    case TypeRef(_, sym, _) => classToJava(sym)
    case _ => throw new java.lang.NoClassDefFoundError("no Java class corresponding to "+tpe+" found")
  }
}