package service

import actor.{Order, OrderStatus}
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.bson.codecs.configuration.CodecRegistry
import org.mongodb.scala._
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.model.Filters._
import util.Helpers._

object DatabaseService {

  val mongoClient: MongoClient = MongoClient()
  val codecRegistry: CodecRegistry = fromRegistries(fromProviders(classOf[Order]), DEFAULT_CODEC_REGISTRY)
  val database: MongoDatabase = mongoClient.getDatabase("orders-db").withCodecRegistry(codecRegistry)
  val collection: MongoCollection[Order] = database.getCollection("orders")

  //For orderStatus codec
  val codecRegistryOrderStatus: CodecRegistry = fromRegistries(fromProviders(classOf[OrderStatus]), DEFAULT_CODEC_REGISTRY)
  val db: MongoDatabase = mongoClient.getDatabase("orders-db").withCodecRegistry(codecRegistryOrderStatus)
  val collectionOrderStatus: MongoCollection[OrderStatus] = db.getCollection("orderStatus")

  /**
   * Get all orders
   * @return all orders
   */
  def getAllTransfers: Seq[Order] = {
    collection.find().results()
  }

  /**
   * Get a free id
   * @return id founded
   */
  def getFreeId: Int = {
    val insertAndCount = for {
      countResult <- collection.countDocuments()
    } yield countResult
    insertAndCount.headResult().toInt
  }

  /**
   * Return one order through his id
   * @param id Order wished
   * @return
   */
  def getOrderByID(id: Integer): Order = {
    collection.find(equal("id", id)).headResult()
  }

  /**
   * Return all orders with a specific status
   * @param status Status wished
   * @return All orders with the specific status entered
   */
  def getTransfersByStatus(status: String): Seq[Order] = {
    collection.find(equal("status", status)).results()
  }

  /**
   * Return status id from description
   * @param statusDescription
   * @return Id of status
   */
  def getStatusIdFromDescription(statusDescription: String): Int = {
    collectionOrderStatus.find(equal("description", statusDescription)).headResult().id
  }

  /**
   * Return orderStatus
   * @param status Status wished
   * @return orderStatus object
   */
  def getOrderStatus(status: Int): OrderStatus = {
    collectionOrderStatus.find(equal("id", status)).headResult()
  }
}
