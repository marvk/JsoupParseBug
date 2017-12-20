This project is made to highlight a probable bug I found in `org.jsoup.helper.HttpConnection::get` and `org.jsoup.helper.HttpConnection.Response::parse`.

While working on an application that downloads and parses RSS files from [packetstormsecurity.com](https://packetstormsecurity.com/) I've found a strange behaviour: Sometimes when selecting a `pubDate` entity from an `item` entity, Jsoup would continue parsing past the closing `pubDate` tag and into the next entity.

This seems to be caused by the above mentioned methods introducing the html character codes for `<` and `>` (`&lt;` and `&gt;` respectively) while parsing the string `</pubDate>`, resulting in a pubDate entity looking like this `<pubDate>Tue, 06 Jun 2017 14:06:27 GMT&lt;/pubDate&gt;`, causing the parser to overshoot.

The bug only happens on very rare occasion for the above mentioned website, I'd guess about one to five in one thousand pages.

Since the above mentioned website is posting new items multiple times per day and thus the affected pages change, the code contains a method to find bad pages. When a bad page is found, the code will download and parse it thrice, once for each of the affected methods and once with a workaround, which uses `Jsoup::parse` and passes the body of the `HttpConnection.Response` object obtained from `Connection::execute`.

Here is some example output from the program:

    Malformed output via Jsoup.connect(url).get():
    Wed, 07 Jun 2017 14:22:24 GMT
    Wed, 07 Jun 2017 14:21:31 GMT
    Wed, 07 Jun 2017 14:19:11 GMT
    Wed, 07 Jun 2017 14:19:03 GMT
    Wed, 07 Jun 2017 14:18:54 GMT
    Wed, 07 Jun 2017 14:18:49 GMT
    Wed, 07 Jun 2017 14:18:44 GMT
    Wed, 07 Jun 2017 14:18:38 GMT
    Wed, 07 Jun 2017 14:18:30 GMT
    Wed, 07 Jun 2017 14:18:23 GMT
    Wed, 07 Jun 2017 14:18:17 GMT
    Wed, 07 Jun 2017 14:18:12 GMT
    Wed, 07 Jun 2017 14:15:55 GMT
    Wed, 07 Jun 2017 13:47:58 GMT
    Wed, 07 Jun 2017 10:11:11 GMT
    Tue, 06 Jun 2017 23:26:00 GMT
    Tue, 06 Jun 2017 23:25:00 GMT
    Tue, 06 Jun 2017 23:24:00 GMT
    Tue, 06 Jun 2017 23:23:00 GMT
    Tue, 06 Jun 2017 14:06:27 GMT</pubDate> Gentoo Linux Security Advisory 201706-5 - Multiple vulnerabilities in D-Bus might allow an attacker to overwrite files with a fixed filename in arbitrary directories or conduct a symlink attack. Versions less than 1.10.18 are affected.
    Tue, 06 Jun 2017 14:06:16 GMT
    Tue, 06 Jun 2017 14:06:02 GMT
    Tue, 06 Jun 2017 14:05:53 GMT
    Tue, 06 Jun 2017 14:05:45 GMT
    Tue, 06 Jun 2017 14:05:34 GMT


    Malformed output via Jsoup.connect(url).execute().parse():
    Wed, 07 Jun 2017 14:22:24 GMT
    Wed, 07 Jun 2017 14:21:31 GMT
    Wed, 07 Jun 2017 14:19:11 GMT
    Wed, 07 Jun 2017 14:19:03 GMT
    Wed, 07 Jun 2017 14:18:54 GMT
    Wed, 07 Jun 2017 14:18:49 GMT
    Wed, 07 Jun 2017 14:18:44 GMT
    Wed, 07 Jun 2017 14:18:38 GMT
    Wed, 07 Jun 2017 14:18:30 GMT
    Wed, 07 Jun 2017 14:18:23 GMT
    Wed, 07 Jun 2017 14:18:17 GMT
    Wed, 07 Jun 2017 14:18:12 GMT
    Wed, 07 Jun 2017 14:15:55 GMT
    Wed, 07 Jun 2017 13:47:58 GMT
    Wed, 07 Jun 2017 10:11:11 GMT
    Tue, 06 Jun 2017 23:26:00 GMT
    Tue, 06 Jun 2017 23:25:00 GMT
    Tue, 06 Jun 2017 23:24:00 GMT
    Tue, 06 Jun 2017 23:23:00 GMT
    Tue, 06 Jun 2017 14:06:27 GMT</pubDate> Gentoo Linux Security Advisory 201706-5 - Multiple vulnerabilities in D-Bus might allow an attacker to overwrite files with a fixed filename in arbitrary directories or conduct a symlink attack. Versions less than 1.10.18 are affected.
    Tue, 06 Jun 2017 14:06:16 GMT
    Tue, 06 Jun 2017 14:06:02 GMT
    Tue, 06 Jun 2017 14:05:53 GMT
    Tue, 06 Jun 2017 14:05:45 GMT
    Tue, 06 Jun 2017 14:05:34 GMT


    Well-formed output via Jsoup.parse(Jsoup.connect(url).execute().body()):
    Wed, 07 Jun 2017 14:22:24 GMT
    Wed, 07 Jun 2017 14:21:31 GMT
    Wed, 07 Jun 2017 14:19:11 GMT
    Wed, 07 Jun 2017 14:19:03 GMT
    Wed, 07 Jun 2017 14:18:54 GMT
    Wed, 07 Jun 2017 14:18:49 GMT
    Wed, 07 Jun 2017 14:18:44 GMT
    Wed, 07 Jun 2017 14:18:38 GMT
    Wed, 07 Jun 2017 14:18:30 GMT
    Wed, 07 Jun 2017 14:18:23 GMT
    Wed, 07 Jun 2017 14:18:17 GMT
    Wed, 07 Jun 2017 14:18:12 GMT
    Wed, 07 Jun 2017 14:15:55 GMT
    Wed, 07 Jun 2017 13:47:58 GMT
    Wed, 07 Jun 2017 10:11:11 GMT
    Tue, 06 Jun 2017 23:26:00 GMT
    Tue, 06 Jun 2017 23:25:00 GMT
    Tue, 06 Jun 2017 23:24:00 GMT
    Tue, 06 Jun 2017 23:23:00 GMT
    Tue, 06 Jun 2017 14:06:27 GMT
    Tue, 06 Jun 2017 14:06:16 GMT
    Tue, 06 Jun 2017 14:06:02 GMT
    Tue, 06 Jun 2017 14:05:53 GMT
    Tue, 06 Jun 2017 14:05:45 GMT
    Tue, 06 Jun 2017 14:05:34 GMT
    
Tested on Jsoup 1.11.2