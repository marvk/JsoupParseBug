package net.marvk;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    public static void main(final String[] args) throws IOException {
        final String url = URL + findBadPage();

        System.out.println("Malformed output via Jsoup.connect(url).get():");
        final Document d1 = Jsoup.connect(url).get();
        print(d1);

        System.out.println("Malformed output via Jsoup.connect(url).execute().parse():");
        final Connection.Response execute = Jsoup.connect(url).execute();
        final Document d2 = execute.parse();
        print(d2);

        System.out.println("Well-formed output via Jsoup.parse(Jsoup.connect(url).execute().body()):");
        final Document d3 = Jsoup.parse(Jsoup.connect(url).execute().body());
        print(d3);
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

    private static int findBadPage() {
        final ExecutorService executorService = Executors.newFixedThreadPool(25);

        final List<Future<Integer>> futures = IntStream.range(1, 4000)
                                                       .mapToObj(page -> download(executorService, page))
                                                       .collect(Collectors.toList());

        for (final Future<Integer> future : futures) {
            try {
                final int page = future.get();

                if (page != -1) {
                    executorService.shutdownNow();
                    return page;
                }
            } catch (final InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        throw new IllegalStateException("No bad page found");
    }

    private static Future<Integer> download(final ExecutorService executor, final int page) {
        return executor.submit(() -> {
            try {
                final List<String> collect = Jsoup.connect(URL + page)
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
            } catch (final Throwable t) {
                return page;
            }

            return -1;
        });
    }
}