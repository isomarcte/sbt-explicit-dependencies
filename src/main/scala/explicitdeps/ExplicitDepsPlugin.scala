package explicitdeps

import sbt.Keys._
import sbt._

object ExplicitDepsPlugin extends AutoPlugin {

  trait Implicits {
    implicit val moduleFilterRemoveValue: Remove.Value[ModuleFilter, ModuleFilter] =
      new Remove.Value[ModuleFilter, ModuleFilter] {
        override def removeValue(a: ModuleFilter, b: ModuleFilter): ModuleFilter = a - b
      }
  }

  object autoImport extends Implicits {
    val undeclaredCompileDependencies = taskKey[Set[Dependency]]("find all libraries that this project's code directly depends on for compilation, but which are not declared in libraryDependencies")
    val undeclaredCompileDependenciesTest = taskKey[Unit]("fail the build if there are any libraries that have not been explicitly declared as compile-time dependencies")
    val undeclaredCompileDependenciesFilter = settingKey[ModuleFilter]("Filter to specify the undeclared dependencies that you care about")

    val unusedCompileDependencies = taskKey[Set[Dependency]]("find all libraries declared in libraryDependencies that this project's code does not actually depend on for compilation")
    val unusedCompileDependenciesTest = taskKey[Unit]("fail the build if there are any libraries declared in libraryDependencies that this project's code does not actually depend on for compilation")
    val unusedCompileDependenciesFilter = settingKey[ModuleFilter]("Filter to specify the undeclared dependencies that you care about")
  }
  import autoImport._

  override def trigger = allRequirements
  override def requires = empty
  override lazy val projectSettings = Seq(
    undeclaredCompileDependencies := undeclaredCompileDependenciesTask.value,
    undeclaredCompileDependenciesTest := undeclaredCompileDependenciesTestTask.value,
    undeclaredCompileDependenciesFilter := defaultModuleFilter,

    unusedCompileDependencies := unusedCompileDependenciesTask.value,
    unusedCompileDependenciesTest := unusedCompileDependenciesTestTask.value,
    unusedCompileDependenciesFilter := defaultModuleFilter
  )

  lazy val undeclaredCompileDependenciesTask = Def.task {
    val log = streams.value.log
    val projectName = name.value
    val allLibraryDeps = getAllLibraryDeps(compile.in(Compile).value.asInstanceOf[Analysis], log)
    val libraryDeps = libraryDependencies.value
    val scalaBinaryVer = scalaBinaryVersion.value
    val filter = undeclaredCompileDependenciesFilter.value

    Logic.getUndeclaredCompileDependencies(
      projectName,
      allLibraryDeps,
      libraryDeps,
      scalaBinaryVer,
      filter,
      log
    )
  }

  lazy val undeclaredCompileDependenciesTestTask = Def.task {
    val undeclaredCompileDeps = undeclaredCompileDependencies.value
    if (undeclaredCompileDeps.nonEmpty)
      throw UndeclaredCompileDependenciesException
  }

  lazy val unusedCompileDependenciesTask = Def.task {
    val log = streams.value.log
    val projectName = name.value
    val allLibraryDeps = getAllLibraryDeps(compile.in(Compile).value.asInstanceOf[Analysis], log)
    val libraryDeps = libraryDependencies.value
    val scalaBinaryVer = scalaBinaryVersion.value
    val filter = unusedCompileDependenciesFilter.value

    Logic.getUnusedCompileDependencies(
      projectName,
      allLibraryDeps,
      libraryDeps,
      scalaBinaryVer,
      filter,
      log
    )
  }

  lazy val unusedCompileDependenciesTestTask = Def.task {
    val unusedCompileDeps = unusedCompileDependencies.value
    if (unusedCompileDeps.nonEmpty)
      throw UnusedCompileDependenciesException
  }

}
