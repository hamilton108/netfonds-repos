package netfondsrepos.integrationtests;

import netfondsrepos.repos.EtradeRepository2;
import oahu.financial.OptionCalculator;
import oahu.financial.Stock;
import oahu.financial.html.EtradeDownloader;
import oahu.financial.repository.StockMarketRepository;
import org.assertj.core.api.Assertions;
import org.jsoup.nodes.Document;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@RunWith(SpringRunner.class)
public class TestEtradeRepository {

    private static String storePath = "/home/rcs/opt/java/netfonds-repos/src/test/resources";
    private static EtradeRepository2 repos;
    private static StockMarketRepository stockMarketRepos;
    private String ticker = "NHY";

    /*
    @Mock
    StockMarketRepository stockMarketRepository;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();
    */

    @BeforeClass
    public static void setup() {
        repos = new EtradeRepository2();

        stockMarketRepos = new StockMarketReposStub();
        repos.setStockMarketRepository(stockMarketRepos);

        EtradeDownloader downloader = new DownloaderStub(storePath);
        repos.setDownloader(downloader);

        OptionCalculator calculator = new BlackScholesStub();
        repos.setOptionCalculator(calculator);
    }

    @AfterClass
    public static void tearDown() {

    }

    @Test
    public void testCallsPuts() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = EtradeRepository2.class.getDeclaredMethod("getDocument",String.class);
        method.setAccessible(true);
        Document doc = (Document) method.invoke(repos, ticker); //repos.getDocument(ticker);
        Assertions.assertThat(doc).isNotNull();
    }
}
/*
import com.gargoylesoftware.htmlunit.Page;
import netfondsrepos.integrationtests.BlackScholesStub;
import oahu.dto.Tuple;
import oahu.financial.*;
import oahu.financial.html.EtradeDownloader;
import oahu.financial.repository.StockMarketRepository;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDate;
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
    private static final OptionCalculator calculator = new BlackScholesStub();

    @DisplayName("Test EtradeRepository2 fetching calls and puts")
    @Test
    public void testCallsPuts() throws IOException {
        String ticker = "NHY";
        EtradeRepository2 repos = new EtradeRepository2();
        repos.setDownloader(downloader);
        repos.setStockMarketRepository(stockMarketRepos);
        repos.setOptionCalculator(calculator);

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

        List<DerivativePrice> calls = repos.createDerivativePrices(rawCalls,Derivative.OptionType.CALL, stockPrice.get());
        assertEquals(91, calls.size(), "Calls size not 91");

        Collection<DerivativePrice> calls2 = repos.calls(ticker);
        assertNotNull(calls2, "Calls2 not null");
        assertEquals(91, calls2.size(), "Calls2 size not 91");

        Collection<DerivativePrice> puts2 = repos.puts(ticker);
        assertNotNull(puts2, "Puts2 not null");
        assertEquals(100, puts2.size(), "Puts2 size not 100");

        Optional<StockPrice> stockPrice2 = repos.stockPrice(ticker);
        assertNotEquals(Optional.empty(), stockPrice2, "StockPrice2 is Optional.empty()");

        //StockPrice s = stockPrice2.get();
        //assertEquals(41.83, s.getOpn(),0.05);
        //assertEquals(41.85, s.getHi(),0.05);
        //assertEquals(40.87, s.getLo(),0.05);
        //assertEquals(41.05, s.getCls(),0.05);
        //assertEquals(4777330, s.getVolume());


        Tuple<String> optionInfo = new Tuple<>("NHY","NHY9I30");
        Optional<DerivativePrice> opx = repos.findDerivativePrice(optionInfo);
        assertNotEquals(Optional.empty(), opx, String.format("Opx is Optional.empty() for %s", optionInfo.second()));
        validateDerivativePrice(opx.get());
    }
    private void validateDerivativePrice(DerivativePrice p) {
        Derivative d = p.getDerivative();
        assertNotNull(d);
        double x = 30.0;
        assertEquals(x, d.getX(), 0.01, String.format("X not %.2f", x));
        LocalDate date = LocalDate.of(2019,9,20);
        assertEquals(date, d.getExpiry(), String.format("Expiry not %s", date));
        Derivative.OptionType optionType = Derivative.OptionType.CALL;
        assertEquals(optionType, d.getOpType(), String.format("Options type not %s", optionType));
        double buy = 11.25;
        assertEquals(buy, p.getBuy(), 0.01, String.format("Buy not %.2f", buy));
        double sell = 13.25;
        assertEquals(sell, p.getSell(), 0.01, String.format("Sell not %.2f", sell));

        double ivBuy = 4.25;
        Optional<Double> maybeIvBuy = p.getIvBuy();
        assertNotEquals(Optional.empty(), maybeIvBuy, "maybeIvBuy was empty");
        assertEquals(ivBuy, maybeIvBuy.get(), 0.01, String.format("IvBuy not %.2f", ivBuy));
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
*/