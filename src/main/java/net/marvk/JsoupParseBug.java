package net.marvk;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class JsoupParseBug {
    private static final String URL = "https://rss.packetstormsecurity.com/files/page";

    /**
     * Print some parsed text from the two affected methods and the same output from the workaround
     */
    public static void printBadExampleOutput() throws IOException {
        final String url = findFirstBadPage();

        System.out.println("Malformed output via Jsoup.connect(url).get():");
        final Document d1 = Jsoup.connect(url).get();
        print(d1);

        System.out.println("Malformed output via Jsoup.connect(url).execute().parse():");
        final Document d2 = Jsoup.connect(url).execute().parse();
        print(d2);

        System.out.println("Well-formed output via Jsoup.parse(Jsoup.connect(url).execute().body()):");
        final Document d3 = Jsoup.parse(Jsoup.connect(url).execute().body());
        print(d3);
    }

    /**
     * Use to find and return the first bad url.
     * @return the url with the smallest page index either with or without trailing slash
     */
    public static String findFirstBadPage() {
        final ExecutorService executorService = Executors.newFixedThreadPool(25);
        final List<Future<String>> futures = getDownloadFutures(executorService);

        for (final Future<String> future : futures) {
            try {
                final String url = future.get();

                if (url != null) {
                    executorService.shutdownNow();
                    return url;
                }
            } catch (final InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        throw new IllegalStateException("No bad page found");
    }

    /**
     * Use to print all bad URLs, both with and without trailing slash.
     */
    public static void printAllBadUrls() {
        final ExecutorService executorService = Executors.newFixedThreadPool(25);
        getDownloadFutures(executorService);
    }

    private static void print(final Document document) {
        document.select("item")
                .stream()
                .map(e -> e.select("pubDate"))
                .flatMap(Collection::stream)
                .map(Element::text)
                .forEach(System.out::println);
        System.out.println(System.lineSeparator());
    }

    private static List<Future<String>> getDownloadFutures(final ExecutorService executorService) {
        return IntStream.range(1, 4000)
                        .mapToObj(page -> Arrays.asList(download(executorService, URL + page), download(executorService, URL + page + "/")))
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList());
    }

    private static Future<String> download(final ExecutorService executor, final String url) {
        return executor.submit(() -> {
            try {
                final List<String> collect = Jsoup.connect(url)
                                                  .get()
                                                  .select("item")
                                                  .stream()
                                                  .map(e -> e.select("pubDate"))
                                                  .flatMap(Collection::stream)
                                                  .map(Element::text)
                                                  .collect(Collectors.toList());

                for (final String s : collect) {
                    LocalDateTime.parse(s, DateTimeFormatter.RFC_1123_DATE_TIME);
                }
            } catch (final DateTimeParseException t) {
                System.out.println(url);
                return url;
            }

            return null;
        });
    }

    public static void main(final String[] args) throws IOException {
        printBadExampleOutput();
    }
}