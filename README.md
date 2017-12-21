This project is made to highlight a probable bug I found in `org.jsoup.helper.HttpConnection::get` and `org.jsoup.helper.HttpConnection.Response::parse`.

While working on an application that downloads and parses RSS files from [packetstormsecurity.com](https://packetstormsecurity.com/) I've found a strange behaviour: Sometimes when selecting a `pubDate` entity from an `item` entity, Jsoup would continue parsing past the closing `pubDate` tag and into the next entity.

This seems to be caused by the above mentioned methods introducing the html character codes for `<` and `>` (`&lt;` and `&gt;` respectively) while parsing the string `</pubDate>`, resulting in a pubDate entity looking like this `<pubDate>Tue, 06 Jun 2017 14:06:27 GMT&lt;/pubDate&gt;`, causing the parser to overshoot.

The bug only happens on very rare occasion for the above mentioned website, I'd guess about one to five in one thousand pages.

Since the above mentioned website is posting new items multiple times per day and thus the affected pages change, the code contains a method to find bad pages. When a bad page is found, the code will download and parse it thrice, once for each of the affected methods and once with a workaround, which uses `Jsoup::parse` and passes the body of the `HttpConnection.Response` object obtained from `Connection::execute`.

Another curious thing is that if a url is affected or not changes based on whether the url has a trailing slash or not. 

Here is some example output from the program:

    https://rss.packetstormsecurity.com/files/page17/
    Malformed output via Jsoup.connect(url).get():
    Sun, 26 Nov 2017 23:23:00 GMT
    Sat, 25 Nov 2017 15:15:43 GMT
    Sat, 25 Nov 2017 15:14:07 GMT
    Sat, 25 Nov 2017 15:12:55 GMT
    Sat, 25 Nov 2017 15:11:14 GMT
    Sat, 25 Nov 2017 15:09:49 GMT
    Sat, 25 Nov 2017 15:07:50 GMT
    Sat, 25 Nov 2017 15:06:20 GMT
    Sat, 25 Nov 2017 15:05:18 GMT
    Sat, 25 Nov 2017 15:02:26 GMT
    Sat, 25 Nov 2017 14:47:39 GMT
    Fri, 24 Nov 2017 12:13:00 GMT
    Fri, 24 Nov 2017 12:12:00 GMT
    Thu, 23 Nov 2017 12:12:12 GMT
    Thu, 23 Nov 2017 12:11:11 GMT
    Thu, 23 Nov 2017 11:11:00 GMT
    Thu, 23 Nov 2017 10:11:11 GMT
    Thu, 23 Nov 2017 09:22:22 GMT
    Wed, 22 Nov 2017 16:11:40 GMT
    Wed, 22 Nov 2017 15:58:36 GMT
    Wed, 22 Nov 2017 15:56:50 GMT
    Wed, 22 Nov 2017 15:55:40 GMT
    Wed, 22 Nov 2017 15:53:13 GMT</pubDate> WebKit suffers from an out-of-bounds read in WebCore::RenderText::localCaretRect.
    Wed, 22 Nov 2017 15:50:02 GMT
    Wed, 22 Nov 2017 15:48:27 GMT
    
    
    Malformed output via Jsoup.connect(url).execute().parse():
    Sun, 26 Nov 2017 23:23:00 GMT
    Sat, 25 Nov 2017 15:15:43 GMT
    Sat, 25 Nov 2017 15:14:07 GMT
    Sat, 25 Nov 2017 15:12:55 GMT
    Sat, 25 Nov 2017 15:11:14 GMT
    Sat, 25 Nov 2017 15:09:49 GMT
    Sat, 25 Nov 2017 15:07:50 GMT
    Sat, 25 Nov 2017 15:06:20 GMT
    Sat, 25 Nov 2017 15:05:18 GMT
    Sat, 25 Nov 2017 15:02:26 GMT
    Sat, 25 Nov 2017 14:47:39 GMT
    Fri, 24 Nov 2017 12:13:00 GMT
    Fri, 24 Nov 2017 12:12:00 GMT
    Thu, 23 Nov 2017 12:12:12 GMT
    Thu, 23 Nov 2017 12:11:11 GMT
    Thu, 23 Nov 2017 11:11:00 GMT
    Thu, 23 Nov 2017 10:11:11 GMT
    Thu, 23 Nov 2017 09:22:22 GMT
    Wed, 22 Nov 2017 16:11:40 GMT
    Wed, 22 Nov 2017 15:58:36 GMT
    Wed, 22 Nov 2017 15:56:50 GMT
    Wed, 22 Nov 2017 15:55:40 GMT
    Wed, 22 Nov 2017 15:53:13 GMT</pubDate> WebKit suffers from an out-of-bounds read in WebCore::RenderText::localCaretRect.
    Wed, 22 Nov 2017 15:50:02 GMT
    Wed, 22 Nov 2017 15:48:27 GMT
    
    
    Well-formed output via Jsoup.parse(Jsoup.connect(url).execute().body()):
    Sun, 26 Nov 2017 23:23:00 GMT
    Sat, 25 Nov 2017 15:15:43 GMT
    Sat, 25 Nov 2017 15:14:07 GMT
    Sat, 25 Nov 2017 15:12:55 GMT
    Sat, 25 Nov 2017 15:11:14 GMT
    Sat, 25 Nov 2017 15:09:49 GMT
    Sat, 25 Nov 2017 15:07:50 GMT
    Sat, 25 Nov 2017 15:06:20 GMT
    Sat, 25 Nov 2017 15:05:18 GMT
    Sat, 25 Nov 2017 15:02:26 GMT
    Sat, 25 Nov 2017 14:47:39 GMT
    Fri, 24 Nov 2017 12:13:00 GMT
    Fri, 24 Nov 2017 12:12:00 GMT
    Thu, 23 Nov 2017 12:12:12 GMT
    Thu, 23 Nov 2017 12:11:11 GMT
    Thu, 23 Nov 2017 11:11:00 GMT
    Thu, 23 Nov 2017 10:11:11 GMT
    Thu, 23 Nov 2017 09:22:22 GMT
    Wed, 22 Nov 2017 16:11:40 GMT
    Wed, 22 Nov 2017 15:58:36 GMT
    Wed, 22 Nov 2017 15:56:50 GMT
    Wed, 22 Nov 2017 15:55:40 GMT
    Wed, 22 Nov 2017 15:53:13 GMT
    Wed, 22 Nov 2017 15:50:02 GMT
    Wed, 22 Nov 2017 15:48:27 GMT
    
Tested on Jsoup 1.11.2