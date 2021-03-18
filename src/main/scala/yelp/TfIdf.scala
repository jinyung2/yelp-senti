package yelp

import org.apache.spark.rdd.RDD
import yelp.App.{STOP_WORDS, TEST_INPUT, sc}
import scala.collection.mutable

class TfIdf {

  private var trainingSetReviews: Array[Review] = Array() // review IDs + term frequencies
  private val documentFreq = mutable.Map[String, Int]() // df of unique words across reviews
  private val inverseDocumentFreq = mutable.Map[String, Double]() // idf of unique words across reviews

  case class Review(star: Int, document: String) extends Serializable {
    val temp = document
      .replaceAll("[^a-zA-Z ]", "")
      .toLowerCase()
      .split(" ")
      .filter(x => x != "")
      .filterNot(STOP_WORDS.contains(_))
      .map(word => (word, 1))

    private val wordVecTF = sc.parallelize(temp)
      .reduceByKey((x, y) => x + 1)
      .map { case (word, tf) => (word, tf.toDouble / temp.length) } // tf is now weighted based on total # of words
      .collect() // performs the TF

    override def toString: String = wordVecTF.mkString(", ");

    def getWordVec() = wordVecTF

    // returns the tfidf weighted RDD
  }

  // for creating model from training set of 1000 reviews
  def train(reviews: RDD[String]) = {
    trainingSetReviews = reviews
      .map(_.split(", ", 2))
      .map(x => (x(0).trim.toInt, x(1)))
      .map { case (star, text) => Review(star, text)}
      .collect()

    // populate the document freq
    trainingSetReviews.foreach(x => {
      x.getWordVec().foreach(y =>
        documentFreq.get(y._1) match {
          case None => documentFreq += (y._1 -> 1);
          case Some(xs) => documentFreq.update(y._1, xs + 1);
        })
    })

    //calculate idf for each unique term using df
    // idf(t) = log(N/(df + 1))
    documentFreq.foreach(x => {
      val idf = Math.log10(trainingSetReviews.length / (x._2 + 1).toDouble) / Math.log10(2.0)
      inverseDocumentFreq += (x._1 -> idf)
    })
  }

  def getReviews = trainingSetReviews
  def getDF = documentFreq
  def getIDF = inverseDocumentFreq

}
