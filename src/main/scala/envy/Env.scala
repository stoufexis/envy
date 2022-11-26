package envy

import shapeless._
import shapeless.labelled._
import cats.implicits._

trait Env[A] {
  def load(prefix : String, source : Map[String, String]) : Either[Throwable, A]
}

class LowPriorityInstances {
  trait EnvField[A, D] {
    def load(prefix : String, source : Map[String, String], default : D) : Either[Throwable, A]
  }
  implicit def envFieldFromEnv[A](implicit e : Env[A]) : EnvField[A, Some[A]]   =
    (name, src, default) => Right(e.load(name, src).getOrElse(default.value))
  implicit def envFieldFromEnvN[A](implicit e : Env[A]): EnvField[A, None.type] =
    (name, src, _) => e.load(name, src)
}

object Env extends LowPriorityInstances {
  def load[A : Env: Manifest](source : Map[String, String] = sys.env): Either[Throwable, A] =
    implicitly[Env[A]].load(configPrefix[A], source)

  def format(name: String): String =
    "[A-Z\\d]".r
      .replaceAllIn(name, m => (if (m.start == 0) "" else "_") + m.group(0))
      .toUpperCase

  def configPrefix[A](implicit man: Manifest[A]): String =
    format(man.runtimeClass.getSimpleName.replaceAll("(?i)config", ""))

  implicit def hNilEnvF[D] : EnvField[HNil, D] = (_, _, _) => Right(HNil)

  implicit def envDFromReadS[A : Read]: EnvField[A, Some[A]]   = (name, src, d) => Right(Read.readFromSource(name, src).getOrElse(d.value))
  implicit def envDFromReadN[A : Read]: EnvField[A, None.type] = (name, src, _) => Read.readFromSource(name, src)

  implicit def hListBaseEnv[K <: Symbol, H, T <: HList, DH, DT <: HList]
  ( implicit
    witness : Witness.Aux[K]
  , hEnv    : Lazy[EnvField[H, DH]]
  , tEnv    : EnvField[T, DT]
  ) : EnvField[FieldType[K, H] :: T, DH :: DT] =
  (prefix, src, default) =>
    ( hEnv.value.load(s"${prefix}_${format(witness.value.name)}", src, default.head)
    , tEnv.load(prefix, src, default.tail)
    ).mapN(field[K](_) :: _)

  implicit def genericEnv[A, R, D <: HList]
  ( implicit
    gen  : LabelledGeneric.Aux[A, R]
  , defs : Default.Aux[A, D]
  , rEnv : Lazy[EnvField[R, D]]
  ) : Env[A] =
  (prefix, src) =>
    rEnv.value.load(prefix, src, defs()).map(gen.from)
}
