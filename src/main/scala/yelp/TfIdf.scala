package yelp

case class TfIdf() {
  case class Review(stars: Int, document: String) {
    private val wordVec = document
      .replaceAll("[^a-zA-Z ]", "")
      .toLowerCase()
      .split(" ")
      .filter(x => x != "")

    override def toString: String = wordVec.mkString(", ");
  }
}
