package hu.sztaki.spark.youtube

import com.google.api.services.youtube.model.SearchResult
import hu.sztaki.spark
import hu.sztaki.spark.{Language, Source}

object Thread {

  def apply(searchResult: SearchResult): spark.Thread =
    spark.Thread(
      Source.Youtube,
      searchResult.getSnippet.getChannelId,
      searchResult.getId.getVideoId,
      Language.Unknown,
      Option(searchResult.getSnippet.getDescription),
      Option(searchResult.getSnippet.getTitle),
      Option(searchResult.getSnippet.getPublishedAt.getValue)
    )

}
