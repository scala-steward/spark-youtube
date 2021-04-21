package hu.sztaki.spark.youtube

import com.google.api.services.youtube.model.CommentThread
import hu.sztaki.spark.Comment.{Author, Metrics, Parent}
import hu.sztaki.spark.{Comment, Language, Source, Thread}

object Comment {

  import scala.collection.JavaConverters.asScalaBufferConverter

  def apply(thread: Thread, commentThread: CommentThread): List[Comment] =
    hu.sztaki.spark.Comment(
      Source.Youtube,
      thread.forum,
      thread.thread,
      commentThread.getSnippet.getTopLevelComment.getSnippet.getTextOriginal,
      Language.Unknown,
      Option(commentThread.getId),
      Option(commentThread.getSnippet.getTopLevelComment.getSnippet.getParentId).map(Parent(_)),
      Option(commentThread.getSnippet.getTopLevelComment.getSnippet.getUpdatedAt.getValue),
      Option(commentThread.getSnippet.getTopLevelComment.getSnippet.getLikeCount).map(
        likes => Metrics(positive = Some(likes.toInt), negative = None, reported = None)
      ),
      Option(commentThread.getSnippet.getTopLevelComment.getSnippet.getAuthorDisplayName).map(
        author =>
          Author(
            internalID = None,
            alias = None,
            name = Option(author),
            mail = None,
            resource = None,
            created = None
          )
      ),
      None
    ) ::
      Option(commentThread.getReplies).toList.flatMap(_.getComments.asScala.map {
        commentReply =>
          hu.sztaki.spark.Comment(
            Source.Youtube,
            thread.forum,
            thread.thread,
            commentReply.getSnippet.getTextOriginal,
            Language.Unknown,
            Option(commentReply.getId),
            Option(commentReply.getSnippet.getParentId).map(Parent(
              _
            )),
            Option(commentReply.getSnippet.getUpdatedAt.getValue),
            Option(commentReply.getSnippet.getLikeCount).map(
              likes => Metrics(positive = Some(likes.toInt), negative = None, reported = None)
            ),
            Option(commentReply.getSnippet.getAuthorDisplayName).map(
              author =>
                Author(
                  internalID = None,
                  alias = None,
                  name = Option(author),
                  mail = None,
                  resource = None,
                  created = None
                )
            ),
            None
          )
      })

}
