package corp.sap.spark

import java.util.Locale

import org.apache.spark.{SparkConf, SparkContext}
import org.scalatest.{BeforeAndAfterAll, Suite, BeforeAndAfterEach}

trait WithSparkContext extends BeforeAndAfterAll {
  self: Suite =>

  override def beforeAll(): Unit = {
    try {
      super.beforeAll()
      setUpSparkContext()
    } catch {
      case ex: Throwable =>
        tearDownSparkContext()
        throw ex
    }
  }

  override def afterAll(): Unit = {
    try {
      super.afterAll()
    } finally {
      tearDownSparkContext()
    }
  }

  /**
   * Number of workers to use (default: 3).
   */
  protected lazy val numberOfSparkWorkers: Int =
    getSetting("spark.workers", "3").toInt

  def sparkConf: SparkConf = {
    val conf = new SparkConf(false)
    /* XXX: Prevent 200 partitions on shuffle */
    conf.set("spark.sql.shuffle.partitions", "4")
    /* XXX: Disable join broadcast */
    conf.set("spark.sql.autoBroadcastJoinThreshold", "-1")
    conf.set("spark.broadcast.factory", "org.apache.spark.broadcast.HttpBroadcastFactory")
    conf.set("spark.shuffle.spill", "false")
    conf.set("spark.shuffle.compress", "false")
    conf.set("spark.ui.enabled", "false")
  }

  def sc: SparkContext

  protected def setUpSparkContext(): Unit

  protected def tearDownSparkContext(): Unit

  /**
   * Gets a setting from a system property, environment variable or default value. In that order.
   *
   * @param key Lowercase, dot-separated system property key.
   * @param default Optional default value.
   * @return Setting value.
   */
  protected def getSetting(key: String, default: String): String = {
    Seq(
      Option(System.getProperty(key)),
      Option(System.getenv(key.toUpperCase(Locale.ENGLISH).replaceAll("\\.", "_"))),
      Some(default)
    )
      .flatten
      .head
  }

}
