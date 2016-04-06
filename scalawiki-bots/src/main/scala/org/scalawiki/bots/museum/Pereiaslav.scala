package org.scalawiki.bots.museum

import java.nio.file.{FileSystem, FileSystems}

import better.files.File.Order
import better.files.{File => SFile}
import com.typesafe.config.{Config, ConfigFactory, ConfigRenderOptions, ConfigValueFactory}
import net.ceedubs.ficus.Ficus._
import org.scalawiki.MwBot
import org.scalawiki.bots.FileUtils._
import org.scalawiki.dto.markup.Table
import org.scalawiki.wikitext.TableParser
import org.scalawiki.wlx.dto.Monument
import org.scalawiki.wlx.dto.lists.WlmUa

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Pereiaslav(conf: Config, fs: FileSystem = FileSystems.getDefault) {

  val home = conf.getString("home")

  val lang = conf.getString("lang")

  def ukWiki = MwBot.get(conf.getString("host"))

  val tablePage = conf.getString("table-page")

  val wlmPage = conf.getString("wlm-page")

  val convertDocs = conf.getOrElse[Boolean]("convert-docs", false)

  val sep = fs.getSeparator

  private val homeDir = SFile(fs.getPath(home))

  def getEntries: Future[Seq[Entry]] = {
    for (entries <- fromWikiTable(tablePage)) yield entries.map { entry =>

      val objDir = homeDir / entry.dir
      val files = list(objDir, isImage).map(_.pathAsString)

      entry.copy(images = files,
        descriptions = getImagesDescr(objDir, files),
        text = getArticleText(objDir)
      )
    }
  }

  def fromWikiTable(tablePage: String): Future[Seq[Entry]] = {
    for (text <- ukWiki.pageText(tablePage)) yield {
      val table = TableParser.parse(text)
      table.data.toSeq.map { row =>
        val seq = row.toSeq
        val (name, article, wlmId) = (seq(0), seq(1), if (seq.size > 2) seq(2) else "")

        Entry(name, Some(article), if (wlmId.trim.nonEmpty) Some(wlmId) else None, Seq.empty, Seq.empty, None)
      }
    }
  }

  def getImagesDescr(dir: SFile, files: Seq[String]): Seq[String] = {
    docsToHtml(dir)
    val descFile = list(dir, isHtml)(Order.bySize).headOption.toSeq
    descFile.flatMap { file =>
      val content = file.contentAsString
      val lines = HtmlParser.trimmedLines(content)
      val filenames = files.map(path => SFile(path).nameWithoutExtension.toLowerCase)
      lines.filter { line =>
        filenames.exists(line.toLowerCase.startsWith) || line.head.isDigit
      }
    }
  }

  def docsToHtml(dir: SFile): Unit = {
    if (convertDocs) {
      val docs = list(dir, isDoc).map(_.toJava)
      ImageListParser.docToHtml(docs)
    }
  }

  def getArticleText(dir: SFile): Option[String] = {
    list(dir, isHtml)(Order.bySize).lastOption.map { file =>
      HtmlParser.htmlText(file.contentAsString)
    }
  }

  def makeTable(dirs: Seq[String]) = {

    val headers = Seq("dir", "article", "wlmId")
    val data = dirs.map(d => Seq(d, d, ""))
    val string = new Table(headers, data).asWiki
    ukWiki.page("User:IlyaBot/test").edit("test", multi = false)
  }

  def makeUploadFiles(entries: Seq[Entry]): Unit = {
    entries.foreach {
      entry =>
        val file = homeDir / entry.dir / "upload.conf"
        val config = makeUploadFileConfig(entry)
        val renderOptions = ConfigRenderOptions.concise().setFormatted(true)
        val text = config.root().render(renderOptions)
        if (!file.exists) {
          println(s"Creating ${file.pathAsString}")
          file.overwrite(text)
        }
    }
  }

  def descriptionLang(description: String, lang: String) = {
    val screened = description.replace("|", "{{!}}").replace("=", "{{=}}")
    s"{{$lang|$description}}"
  }

  def interWikiLink(target: String, title: String = "", lang: String) = {
    val screened = title.replace("|", "{{!}}")
    s"[[:$lang:$target|$title]]"
  }

  def makeUploadFileConfig(entry: Entry): Config = {
    import scala.collection.JavaConverters._

    val maps = entry.images.zip(entry.descriptions).zipWithIndex.map {
      case ((image, description), index) =>
        val article = entry.article.get
        val wikiDescription = descriptionLang(
          description + ", " + interWikiLink(article, lang = lang),
          lang
        )

        Map(
          "title" -> s"${entry.dir} ${index + 1}",
          "file" -> image,
          "description" -> wikiDescription
        ) ++ entry.wlmId.map("wlm-id" -> _)
    }
    val value = ConfigValueFactory.fromIterable(maps.map(_.asJava).asJava)
    ConfigFactory.empty().withValue("images", value)
  }

  def makeGallery(entries: Seq[Entry]): Unit = {
    HtmlOutput.makeGalleries(entries).zipWithIndex.foreach { case (h, i) =>
      SFile(s"$home/e$i.html").overwrite(h)
    }
  }

  def wlm(): Seq[Monument] = {
    val ukWiki = MwBot.get(MwBot.ukWiki)
    val text = ukWiki.await(ukWiki.pageText(wlmPage))

    Monument.monumentsFromText(text, wlmPage, WlmUa.templateName, WlmUa).toBuffer
  }

  def run() = {
    getEntries.map { entries =>
      makeGallery(entries)
      makeUploadFiles(entries)
    } onFailure { case e => println(e) }
  }
}

object Pereiaslav {

  def main(args: Array[String]) {
    val conf = ConfigFactory.load("pereiaslav.conf")
    val pereiaslav = new Pereiaslav(conf)
    pereiaslav.run()
  }
}

