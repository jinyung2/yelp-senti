package yelp

import org.apache.spark.SparkContext._
import scala.io._
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.rdd._
import org.apache.log4j.Logger
import org.apache.log4j.Level
import scala.collection._

object App {

  val INPUT_FILE = "script/yelp_review_trimmed.json"
  val TEST_INPUT = "test_output.txt"

  case class Review(stars: Int, document: String) {
    private val wordVec = document
      .replaceAll("[^a-zA-Z ]", "")
      .toLowerCase()
      .split(" ")
      .filter(x => x != "")

    override def toString: String = wordVec.mkString(", ");
  }

  def main(args: Array[String]) = {
    Logger.getLogger("org").setLevel(Level.OFF)
    Logger.getLogger("akka").setLevel(Level.OFF)

    val conf = new SparkConf().setAppName("NameOfApp")
      .setMaster("local[4]")
    val sc = new SparkContext(conf)

    val reviews = sc.textFile(TEST_INPUT)
      .map(_.split(", ", 2))
      .map(x => (x(0).trim.toInt, x(1)))
      .map{case (star, text) => Review(star, text)}
      .collect()
      .foreach(println)

  }
}
