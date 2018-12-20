package netfondsrepos.repos;

import com.gargoylesoftware.htmlunit.Page;
import netfondsrepos.downloader.MockDownloader;
import oahu.dto.Tuple;
import oahu.financial.Derivative;
import oahu.financial.DerivativePrice;
import oahu.financial.Stock;
import oahu.financial.StockPrice;
import oahu.financial.html.EtradeDownloader;
import oahu.financial.repository.StockMarketRepository;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestEtradeRepository {
    private static String storePath = "/home/rcs/opt/java/netfonds-repos/src/test/resources";
    private static final EtradeDownloader<Page, Serializable> downloader = new MockDownloader(storePath);
    private static final StockMarketRepository stockMarketRepos = new MockStockMarketRepos();

    @DisplayName("Test EtradeRepository2 fetching calls and puts")
    @Test
    public void testCallsPuts() throws IOException {
        String ticker = "NHY";
        EtradeRepository2 repos = new EtradeRepository2();
        repos.setDownloader(downloader);
        repos.setStockMarketRepository(stockMarketRepos);

        Stock stock = stockMarketRepos.findStock(ticker);

        Document doc = repos.getDocument(ticker);
        assertNotNull(doc, "Doc is null");

        Optional<StockPrice> stockPrice = repos.createStockPrice(doc, stock);
        assertEquals(true, stockPrice.isPresent(),"stockPrice.isPresent() == false");
        validateStockPrice(stockPrice.get());

        Elements rawCalls = repos.findRawOptions(doc, true);
        validateRawOptions(rawCalls, 103, true);

        Elements rawPuts = repos.findRawOptions(doc, false);
        validateRawOptions(rawPuts, 103, false);

        List<DerivativePrice> calls = repos.createDerivativePrices(rawCalls,Derivative.OptionType.CALL, stock);
        assertEquals(91, calls.size(), "Calls size not 91");

        Collection<DerivativePrice> calls2 = repos.calls(ticker);
        assertNotNull(calls2, "Calls2 not null");
        assertEquals(91, calls2.size(), "Calls2 size not 91");

        Collection<DerivativePrice> puts2 = repos.puts(ticker);
        assertNotNull(puts2, "Puts2 not null");
        assertEquals(100, puts2.size(), "Puts2 size not 100");

        Optional<StockPrice> stockPrice2 = repos.stockPrice(ticker);
        assertNotEquals(Optional.empty(), stockPrice2, "StockPrice2 is Optional.empty()");
        /*
        StockPrice s = stockPrice2.get();
        assertEquals(41.83, s.getOpn(),0.05);
        assertEquals(41.85, s.getHi(),0.05);
        assertEquals(40.87, s.getLo(),0.05);
        assertEquals(41.05, s.getCls(),0.05);
        assertEquals(4777330, s.getVolume());
        */

        Tuple<String> optionInfo = new Tuple<>("NHY","NHY9I30");
        Optional<DerivativePrice> opx = repos.findDerivativePrice(optionInfo);
        assertNotEquals(Optional.empty(), opx, "Opx is Optional.empty()");

    }
    private void validateRawOptions(Elements options, int expected, boolean isCalls) {
        String opTypw = isCalls ? "Calls" : "Puts";
        String msg = String.format("%s not %d", opTypw, expected);
        assertEquals(expected, options.size(), msg);
    }
    private void validateStockPrice(StockPrice stockPrice) {
        double expectedOpn = 41.83;
        assertEquals(expectedOpn, stockPrice.getOpn(), 0.01, String.format("Open not %.2f",expectedOpn));

        double expectedHi = 41.85;
        assertEquals(expectedHi, stockPrice.getHi(), 0.01, String.format("Hi not %.2f",expectedHi));

        double expectedLo = 40.87;
        assertEquals(expectedLo,  stockPrice.getLo(), 0.01, String.format("Lo not %.2f", expectedLo));

        double expectedCls = 41.05;
        assertEquals(expectedCls, stockPrice.getCls(), 0.01, String.format("Close not %.2f", expectedCls));

        long expectedVol = 4777330;
        assertEquals(expectedVol, stockPrice.getVolume(), String.format("Volume not %d", expectedVol));
    }
}
