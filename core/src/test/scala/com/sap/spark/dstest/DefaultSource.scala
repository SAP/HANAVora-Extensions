package com.sap.spark.dstest

import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.sources._
import org.apache.spark.sql.sources.sql.{View, DimensionView}
import org.apache.spark.sql.types._

/**
 * Test default source that is capable of creating dummy temporary and persistent relations
 */
class DefaultSource extends TemporaryAndPersistentSchemaRelationProvider
with TemporaryAndPersistentRelationProvider
with PartitionedRelationProvider
with RegisterAllTableRelations
with ViewProvider
with DimensionViewProvider
with DatasourceCatalog {


  // (YH) shouldn't we also add the created table name to the tables sequence in DefaultSource?
  override def createRelation(sqlContext: SQLContext,
                              parameters: Map[String, String]): BaseRelation =
    createRelation(sqlContext, parameters, None, None, isTemporary = false, allowExisting = false)

  override def createRelation(sqlContext: SQLContext,
                              parameters: Map[String, String],
                              schema: StructType): BaseRelation =
    new DummyRelationWithoutTempFlag(sqlContext, schema)

  override def createRelation(sqlContext: SQLContext, parameters: Map[String, String],
                              isTemporary: Boolean,
                              allowExisting: Boolean): BaseRelation =
    new DummyRelationWithTempFlag(sqlContext,
      DefaultSource.standardSchema,
      isTemporary)

  override def createRelation(sqlContext: SQLContext, parameters: Map[String, String],
                              sparkSchema: StructType, isTemporary: Boolean,
                              allowExisting: Boolean): BaseRelation =
    new DummyRelationWithTempFlag(sqlContext, sparkSchema, isTemporary)

  override def createRelation(sqlContext: SQLContext, parameters: Map[String, String],
                              partitioningFunction: Option[String],
                              partitioningColumns: Option[Seq[String]], isTemporary: Boolean,
                              allowExisting: Boolean): BaseRelation =
    new DummyRelationWithTempFlag(sqlContext,
      DefaultSource.standardSchema,
      isTemporary)

  override def createRelation(sqlContext: SQLContext, parameters: Map[String, String],
                              sparkSchema: StructType, partitioningFunction: Option[String],
                              partitioningColumns: Option[Seq[String]], isTemporary: Boolean,
                              allowExisting: Boolean): BaseRelation =
    new DummyRelationWithTempFlag(sqlContext, sparkSchema, isTemporary)

  override def getAllTableRelations(sqlContext: SQLContext,
                                    options: Map[String, String])
    : Map[String, LogicalPlanSource] = {
    DefaultSource.tables.map(name =>
      (name, BaseRelationSource(
        new DummyRelationWithTempFlag(sqlContext, DefaultSource.standardSchema, false)))
    ).toMap ++
    DefaultSource.views.map {
      case (name, (_, query)) =>
        name -> CreatePersistentViewSource(query)
    }
  }


  override def getTableRelation(tableName: String, sqlContext: SQLContext,
                                options: Map[String, String]): Option[LogicalPlanSource] = {
    if (DefaultSource.tables.contains(tableName)) {
      Some(BaseRelationSource(
        new DummyRelationWithTempFlag(sqlContext, DefaultSource.standardSchema, false)))
    } else {
      if (DefaultSource.views.contains(tableName)) {
        Some(CreatePersistentViewSource(DefaultSource.views(tableName)._2))
      } else {
        None
      }
    }
  }

  override def getRelation(sqlContext: SQLContext, name: Seq[String], options: Map[String, String])
    : Option[RelationInfo] = {
    DefaultSource.tables.find(r => r.equals(name.last))
      .map(r => RelationInfo(r, isTemporary = false, "TABLE", Some("<DDL statement>")))
  }

  override def getRelations(sqlContext: SQLContext, options: Map[String, String])
    : Seq[RelationInfo] = {
    DefaultSource.tables.map(r =>
      RelationInfo(r, isTemporary = false, "TABLE", Some("<DDL statement>")))
  }

  override def getTableNames(sqlContext: SQLContext, parameters: Map[String, String])
    : Seq[String] = {
    DefaultSource.tables
  }

  /**
   * @inheritdoc
   */
  override def createView(sqlContext: SQLContext, view: View, options: Map[String, String],
                          allowExisting: Boolean): Unit = {
    DefaultSource.addView(view.name.table, "view", options("VIEW_SQL"))
  }

  /**
   * @inheritdoc
   */
  override def dropView(sqlContext: SQLContext, view: Seq[String], options: Map[String, String],
                        allowNotExisting: Boolean): Unit = {
    // no-op
  }

  /**
   * @inheritdoc
   */
  override def createDimensionView(sqlContext: SQLContext, view: DimensionView,
                                   options: Map[String, String],
                                   allowExisting: Boolean): Unit = {
    DefaultSource.addView(view.name.table, "dimension", options("VIEW_SQL"))
  }

  /**
   * @inheritdoc
   */
  override def dropDimensionView(sqlContext: SQLContext, view: Seq[String],
                                 options: Map[String, String],
                                 allowNotExisting: Boolean): Unit = {
    // no-op
  }
}

/**
 * Companion Object handling already existing relations
  *
  * Contains a relation with the name "StandardTestTable" you might add additional ones. This one
  * is for the thriftserver test.
  *
 */
object DefaultSource {

  private val standardSchema = StructType(Seq(StructField("field", IntegerType, nullable = true)))

  val standardRelation = "StandardTestTable"

  private var tables = Seq(standardRelation)
  private var views = Map.empty[String, (String, String)]

  def addRelation(name: String): Unit = {
    tables = tables ++ Seq(name)
  }

  def addView(name: String, kind: String, query: String): Unit = {
    views = views + (name -> (kind, query))
  }

  def reset(keepStandardRelations: Boolean = true): Unit = {
    if (keepStandardRelations) {
      tables = Seq(standardRelation)
    } else {
      tables = Seq.empty[String]
    }
    views = Map.empty[String, (String, String)]
  }

}
