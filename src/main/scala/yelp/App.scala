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
  val TEST = Array("cant", "i")
  val STOP_WORDS = List("a", "about", "above", "after", "again", "against", "ain",
    "all", "am", "an", "and", "any", "are", "aren", "aren't", "as", "at", "be", "because",
    "been", "before", "being", "below", "between", "both", "but", "by", "can", "couldn",
    "couldn't", "d", "did", "didn", "didn't", "do", "does", "doesn", "doesn't", "doing",
    "don", "don't", "down", "during", "each", "few", "for", "from", "further", "had",
    "hadn", "hadn't", "has", "hasn", "hasn't", "have", "haven", "haven't", "having",
    "he", "her", "here", "hers", "herself", "him", "himself", "his", "how", "i", "if",
    "in", "into", "is", "isn", "isn't", "it", "it's", "its", "itself", "just", "ll", "m",
    "ma", "me", "mightn", "mightn't", "more", "most", "mustn", "mustn't", "my", "myself",
    "needn", "needn't", "no", "nor", "not", "now", "o", "of", "off", "on", "once", "only",
    "or", "other", "our", "ours", "ourselves", "out", "over", "own", "re", "s", "same",
    "shan", "shan't", "she", "she's", "should", "should've", "shouldn", "shouldn't", "so",
    "some", "such", "t", "than", "that", "that'll", "the", "their", "theirs", "them",
    "themselves", "then", "there", "these", "they", "this", "those", "through", "to",
    "too", "under", "until", "up", "ve", "very", "was", "wasn", "wasn't", "we", "were",
    "weren", "weren't", "what", "when", "where", "which", "while", "who", "whom", "why",
    "will", "with", "won", "won't", "wouldn", "wouldn't", "y", "you", "you'd", "you'll",
    "you're", "you've", "your", "yours", "yourself", "yourselves", "could", "he'd", "he'll",
    "he's", "here's", "how's", "i'd", "i'll", "i'm", "i've", "let's", "ought", "she'd",
    "she'll", "that's", "there's", "they'd", "they'll", "they're", "they've", "we'd",
    "we'll", "we're", "we've", "what's", "when's", "where's", "who's", "why's", "would")

  case class Review(stars: Int, document: String) {
    private var wordVec = document
      .replaceAll("[^a-zA-Z ]", "")
      .toLowerCase()
      .split(" ")
      .filter(x => x != "")

    override def toString: String = wordVec.mkString(", ");
    def setWordVec(newVec : Array[String]) : Unit = {
      wordVec = newVec
    }
    def getWordVec() : Array[String] = {
      wordVec
    }
  }

  def main(args: Array[String]) = {
    Logger.getLogger("org").setLevel(Level.OFF)
    Logger.getLogger("akka").setLevel(Level.OFF)

    val conf = new SparkConf().setAppName("NameOfApp")
      .setMaster("local[4]")
    val sc = new SparkContext(conf)
    val reviewsAndTFs = mutable.Map[Int,Array[(String,Int)]]() // review IDs + term frequencies
    val documentFreq = mutable.Map[String,Int]() // df of unique words across reviews

    val reviews = sc.textFile(TEST_INPUT)
      .map(_.split(", ", 2))
      .map(x => (x(0).trim.toInt, x(1)))
      .map{case (star, text) => Review(star, text)}
      .map(x => { //get rid of stop words
        x.setWordVec(x.getWordVec().filterNot(STOP_WORDS.contains(_)))
        x
      })

    var id : Int = 0 // get term frequencies
    reviews.collect().foreach(x => {
      val temp = sc.parallelize(x.getWordVec()).map(y => (y, 1)).reduceByKey((x, y) => x + y).collect();
      reviewsAndTFs += (id -> temp)
      id+=1
    })

    // get document frequencies
    reviews.collect().foreach(x => {
      x.getWordVec().foreach(y =>
        documentFreq.get(y) match {
        case None => documentFreq += (y -> 1);
        case Some(xs) => documentFreq.update(y, xs + 1);
      })
    })

    //reviewsAndTFs.foreach(x => println(x._1 + " " + x._2.foreach(print)))
    documentFreq.foreach(println)
  }
}
