package de.tototec.sbuild

import java.io.File
import java.util.Date

/**
 * While a target is executed, this trait can be used to get relevant information
 * about the current target execution and interact with the executor.
 *
 */
trait TargetContext {
  /** The name of the currently executed target */
  def name: String

  /**
   * The file this targets produces, or <code>None</code> if this target is phony.
   */
  def targetFile: Option[File]

  /** The prerequisites (direct dependencies) of this target. */
  def prerequisites: Seq[TargetRef]

  /**
   * Those files, that belongs to dependencies that resolve to files.
   * Dependencies with phony scheme or which resolve to phony will not be included.
   */
  def fileDependencies: Seq[File]

  /**
   * The time in milliseconds this target took to execute.
   * In case this target is still running, the time since it started.
   */
  def execDurationMSec: Long

  def targetLastModified: Option[Long]
  def targetLastModified_=(targetLastModified: Long)

  def project: Project
}

class TargetContextImpl(target: Target) extends TargetContext {

  override def name = target.name

  /**
   * The file this targets produces, or <code>None</code> if this target is phony.
   */
  override def targetFile: Option[File] = target.targetFile

  def start = startTime match {
    case null => startTime = new Date()
    case _ =>
  }
  private var startTime: Date = _

  def end = endTime match {
    case null => endTime = new Date()
    case _ =>
  }
  private var endTime: Date = _

  /**
   * The time in milliseconds this target took to execute.
   * In case this target is still running, the time since it started.
   */
  override def execDurationMSec: Long = startTime match {
    case null => 0
    case _ =>
      (endTime match {
        case null => new Date()
        case x => x
      }).getTime - startTime.getTime
  }

  /**
   * The prerequisites (direct dependencies) of this target.
   */
  override def prerequisites: Seq[TargetRef] = target.dependants

  override def fileDependencies: Seq[File] = target.dependants map { t =>
    target.project.findTarget(t, true) match {
      case None => throw new ProjectConfigurationException("Missing dependency: " + t.name)
      case Some(found) => found.targetFile match {
        case None => null
        case Some(f) => f
      }
    }
  } filter { f => f != null }

  private var _targetLastModified: Option[Long] = None
  override def targetLastModified: Option[Long] = _targetLastModified
  override def targetLastModified_=(targetLastModified: Long) = _targetLastModified =
    _targetLastModified match {
      case Some(previous) => Some(math.max(previous, targetLastModified))
      case None if targetLastModified > 0 => Some(targetLastModified)
      case _ => None
    }

  override def project: Project = target.project
}