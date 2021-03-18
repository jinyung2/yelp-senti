package yelp

object IndexFactory {
  private var index = -1
  def create(): Int = {
    index += 1
    index
  }
}
