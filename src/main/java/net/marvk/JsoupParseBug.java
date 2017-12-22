package net.marvk;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
     *
     * @return the url with the smallest page index either with or without trailing slash
     */
    public static String findFirstBadPage() {
        final ExecutorService executorService = Executors.newFixedThreadPool(25);
        final List<Future<String>> futures = getDownloadFutures(executorService, false);

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
     *
     * @param saveToDisk if parameter is set, saves all bad pages found to badPages/
     */
    public static void printAllBadUrls(final boolean saveToDisk) {
        final ExecutorService executorService = Executors.newFixedThreadPool(25);
        getDownloadFutures(executorService, saveToDisk);
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

    private static List<Future<String>> getDownloadFutures(final ExecutorService executorService, final boolean save) {
        return IntStream.range(1, 4000)
                        .mapToObj(page -> Arrays.asList(download(executorService, URL + page, save), download(executorService, URL + page + "/", save)))
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList());
    }

    private static Future<String> download(final ExecutorService executor, final String url, final boolean save) {
        return executor.submit(() -> {
            final Connection.Response response = Jsoup.connect(url)
                                                      .execute();


            final List<String> collect = response.parse()
                                                 .select("item")
                                                 .stream()
                                                 .map(e -> e.select("pubDate"))
                                                 .flatMap(Collection::stream)
                                                 .map(Element::text)
                                                 .collect(Collectors.toList());
            try {
                for (final String s : collect) {
                    LocalDateTime.parse(s, DateTimeFormatter.RFC_1123_DATE_TIME);
                }
            } catch (final DateTimeParseException e) {
                System.out.println(url);
                if (save) {
                    saveToFile(url, Jsoup.connect(url).execute().bodyAsBytes());
                }
                return url;
            }

            return null;
        });
    }

    private static void saveToFile(final String url, final byte[] bytes) {
        try {
            final Path slash = Paths.get("badPages" + File.separator + url.replaceFirst(URL, "")
                                                                          .replaceAll("/", "SLASH"));

            if (!slash.getParent().toFile().exists()) {
                Files.createDirectory(slash.getParent());
            }

            Files.write(
                    slash,
                    bytes
            );
        } catch (final Throwable t) {
            t.printStackTrace();
        }
    }

    public static void main(final String[] args) throws IOException {
        printAllBadUrls(true);
    }
}