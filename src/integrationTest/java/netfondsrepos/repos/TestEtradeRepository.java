package netfondsrepos.repos;

import netfondsrepos.repos.EtradeRepository2;
import oahu.dto.Tuple;
import oahu.financial.*;
import oahu.financial.html.EtradeDownloader;
import oahu.financial.repository.StockMarketRepository;
import oahu.testing.TestUtil;
import static  org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

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
    public void testDocIsNotNull() {
        Document doc = getDocument();
        assertThat(doc).isNotNull();
    }


    @Test
    public void testStockPrice() {
        Stock stock = stockMarketRepos.findStock(ticker);

        Document doc = getDocument();

        Optional<StockPrice> stockPrice = createStockPrice(doc, stock);
        assertThat(stockPrice.isPresent()).isEqualTo(true);
        validateStockPrice(stockPrice.get());
    }


    @Test
    public void testCallsPutsSizes() {
        int expected = 103;

        Document doc = getDocument();

        Elements rawCalls = rawOptions(doc, true);
        assertThat(rawCalls.size()).isEqualTo(expected);

        Elements rawPuts = rawOptions(doc, false);
        assertThat(rawPuts.size()).isEqualTo(expected);
    }


    @Test
    public void testCreateDerivativePrices() {
        Document doc = getDocument();
        Elements rawCalls = rawOptions(doc,true);
        Stock stock = stockMarketRepos.findStock(ticker);
        Optional<StockPrice> stockPrice = createStockPrice(doc, stock);
        List<DerivativePrice> calls = createDerivativePrices(rawCalls, Derivative.OptionType.CALL, stockPrice.get());
        assertThat(calls.size()).isEqualTo(91);

        Collection<DerivativePrice> calls2 = repos.calls(ticker);
        assertThat(calls2).isNotNull();
        assertThat(calls2.size()).isEqualTo(91);
    }

    @Test
    public void testCreateAndValidateDerivativePrice() {
        Tuple<String> optionInfo = new Tuple<>("NHY","NHY9I30");
        Optional<DerivativePrice> opx = repos.findDerivativePrice(optionInfo);
        assertThat(opx).isNotEmpty();
        validateDerivativePrice(opx.get());
    }

    private List<DerivativePrice> createDerivativePrices(Elements rawOptions, Derivative.OptionType optionType, StockPrice stockPrice) {
        Class[] paramsTypes = {Elements.class, Derivative.OptionType.class, StockPrice.class};
        Object[] params = {rawOptions,optionType,stockPrice};
        return TestUtil.callMethodFor(EtradeRepository2.class, repos, "createDerivativePrices", paramsTypes, params);
    }
    private Optional<StockPrice> createStockPrice(Document doc, Stock stock) {
        Class[] paramsTypes = {Document.class, Stock.class};
        Object[] params = {doc,stock};
        return TestUtil.callMethodFor(EtradeRepository2.class, repos, "createStockPrice", paramsTypes, params);
    }

    private void validateDerivativePrice(DerivativePrice p) {
        Derivative d = p.getDerivative();
        assertThat(d).isNotNull();

        assertThat(d.getX()).isCloseTo(30.0, offset(0.01));

        LocalDate date = LocalDate.of(2019,9,20);
        assertThat(d.getExpiry()).isEqualTo(date);

        assertThat(d.getOpType()).isEqualTo(Derivative.OptionType.CALL);

        assertThat(p.getBuy()).isCloseTo(11.25, offset(0.01));

        assertThat(p.getSell()).isCloseTo(13.25, offset(0.01));


        Optional<Double> maybeIvBuy = p.getIvBuy();
        assertThat(maybeIvBuy).isNotEmpty();
        assertThat(maybeIvBuy.get()).isCloseTo(4.25, offset(0.01));
    }
    private void validateStockPrice(StockPrice stockPrice) {
        double expectedOpn = 41.83;
        assertThat(stockPrice.getOpn()).isCloseTo(expectedOpn, offset(0.01));

        double expectedHi = 41.85;
        assertThat(stockPrice.getHi()).isCloseTo(expectedHi, offset(0.01));

        double expectedLo = 40.87;
        assertThat(stockPrice.getLo()).isCloseTo(expectedLo, offset(0.01));

        double expectedCls = 41.05;
        assertThat(stockPrice.getCls()).isCloseTo(expectedCls, offset(0.01));

        long expectedVol = 4777330;
        assertThat(stockPrice.getVolume()).isEqualTo(expectedVol);
    }

    private Elements rawOptions(Document doc, boolean isCalls) {
        Class[] paramsTypes = {Document.class, boolean.class};
        Object[] params = {doc,isCalls};
        return TestUtil.callMethodFor(EtradeRepository2.class, repos, "findRawOptions", paramsTypes, params);
    }
    private Document getDocument() {
        Class[] paramsTypes = {String.class};
        Object[] params = {ticker};
        return TestUtil.callMethodFor(EtradeRepository2.class, repos, "getDocument", paramsTypes, params);
    }
}
