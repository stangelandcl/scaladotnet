/* NSC -- new Scala compiler
 * Copyright 2005-2011 LAMP/EPFL
 * @author Paul Phillips
 */

package scala.tools.nsc
package interpreter

/** A class which the repl utilizes to expose predefined objects.
 *  The base implementation is empty; the standard repl implementation
 *  is StdReplVals.
 */
abstract class ReplVals { }

class StdReplVals(final val r: ILoop) extends ReplVals {
  lazy val repl                     = r
  lazy val intp                     = r.intp
  lazy val power                    = r.power
  lazy val reader                   = r.in
  lazy val vals                     = this
  lazy val global: intp.global.type = intp.global
  lazy val isettings                = intp.isettings
  lazy val completion               = reader.completion
  lazy val history                  = reader.history
  lazy val phased                   = power.phased
  lazy val analyzer                 = global.analyzer

  lazy val treedsl = new { val global: intp.global.type = intp.global } with ast.TreeDSL { }
  lazy val typer = analyzer.newTyper(
    analyzer.rootContext(
      power.unit("").asInstanceOf[analyzer.global.CompilationUnit]
    )
  )

  lazy val replImplicits = new power.Implicits2 {
    import intp.global._

    private val manifestFn = ReplVals.mkManifestToType[intp.global.type](global)
    implicit def mkManifestToType(sym: Symbol) = manifestFn(sym)
  }

  def typed[T <: analyzer.global.Tree](tree: T): T = typer.typed(tree).asInstanceOf[T]
}

object ReplVals {
  /** Latest attempt to work around the challenge of foo.global.Type
   *  not being seen as the same type as bar.global.Type even though
   *  the globals are the same.  Dependent method types to the rescue.
   */
  def mkManifestToType[T <: Global](global: T) = {
    import global._
    import definitions._
    
    /** We can't use definitions.manifestToType directly because we're passing
     *  it to map and the compiler refuses to perform eta expansion on a method
     *  with a dependent return type.  (Can this be relaxed?) To get around this
     *  I have this forwarder which widens the type and then cast the result back
     *  to the dependent type.
     */
    def manifestToType(m: OptManifest[_]): Global#Type =
      definitions.manifestToType(m)
    
    class AppliedTypeFromManifests(sym: Symbol) {
      def apply[M](implicit m1: Manifest[M]): Type =
        appliedType(sym.typeConstructor, List(m1) map (x => manifestToType(x).asInstanceOf[Type]))

      def apply[M1, M2](implicit m1: Manifest[M1], m2: Manifest[M2]): Type =
        appliedType(sym.typeConstructor, List(m1, m2) map (x => manifestToType(x).asInstanceOf[Type]))
    }
    
    (sym: Symbol) => new AppliedTypeFromManifests(sym)
  }
}
