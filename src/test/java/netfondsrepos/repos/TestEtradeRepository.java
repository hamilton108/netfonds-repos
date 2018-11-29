package netfondsrepos.repos;

import com.gargoylesoftware.htmlunit.Page;
import netfondsrepos.downloader.MockDownloader;
import oahu.financial.StockPrice;
import oahu.financial.html.EtradeDownloader;
import oahu.financial.repository.StockMarketRepository;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Serializable;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestEtradeRepository {
    private static String storePath = "/home/rcs/opt/java/netfonds-repos/src/test/resources";
    private static final EtradeDownloader<Page, Serializable> downloader = new MockDownloader(storePath);
    private static final StockMarketRepository stockMarketRepos = new MockStockMarketRepos();

    @DisplayName("Test EtradeRepository2")
    @Test
    public void testOne() throws IOException {
        String ticker = "NHY";
        EtradeRepository2 repos = new EtradeRepository2();
        repos.setDownloader(downloader);
        repos.setStockMarketRepository(stockMarketRepos);

        Document doc = repos.getDocument(ticker);
        assertNotNull(doc, "Doc is null");

        Optional<StockPrice> stockPrice = repos.getStockPrice(doc);
        assertEquals(stockPrice.isPresent(),true, "stockPrice.isPresent() == false");
        assertEquals(1,1, "1 is not 1???");
    }
}
