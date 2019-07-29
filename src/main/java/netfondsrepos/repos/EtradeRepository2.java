package netfondsrepos.repos;

import com.gargoylesoftware.htmlunit.Page;
import critterrepos.beans.StockPriceBean;
import critterrepos.beans.options.DerivativeBean;
import critterrepos.beans.options.DerivativePriceBean;
import oahu.dto.Tuple;
import oahu.dto.Tuple2;
import oahu.dto.Tuple3;
import oahu.financial.*;
import oahu.financial.html.EtradeDownloader;
import oahu.financial.repository.EtradeRepository;
import oahu.financial.repository.StockMarketRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class EtradeRepository2 implements EtradeRepository<Tuple<String>> {

    private EtradeDownloader<Page, String, Serializable> downloader;

    private Map<String,Tuple3<Optional<StockPrice>,Collection<DerivativePrice>,Collection<DerivativePrice>>>
    stoxPutsCalls = null;
    private StockMarketRepository stockMarketRepository;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM-yyyy");
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    // fredag 23/11-2018 18:30:05
    private OptionCalculator optionCalculator;

    //region interface EtradeRepository
    @Override
    public Optional<DerivativePrice> findDerivativePrice(Tuple<String> optionInfo) {
        Tuple3<Optional<StockPrice>,Collection<DerivativePrice>,Collection<DerivativePrice>>
                parsed = parseHtmlFor(optionInfo.first(),null);
        Optional<DerivativePrice> price =
                parsed.second().stream().filter(x -> x.getDerivative().getTicker().equals(optionInfo.second())).findAny();
        if (price.isPresent()) {
            return price;
        }
        else {
            return parsed.third().stream().filter(x -> x.getDerivative().getTicker().equals(optionInfo.second())).findAny();
        }
    }

    @Override
    public Optional<StockPrice> stockPrice(int oid) {
        return stockPrice(getTickerFor(oid));
    }
    @Override
    public Optional<StockPrice> stockPrice(String ticker) {
        Tuple3<Optional<StockPrice>, Collection<DerivativePrice>, Collection<DerivativePrice>>
                parsed = parseHtmlFor(ticker, null);
        return parsed.first();
    }


    @Override
    public Collection<DerivativePrice> puts(String ticker) {
        Tuple3<Optional<StockPrice>, Collection<DerivativePrice>, Collection<DerivativePrice>>
                parsed = parseHtmlFor(ticker, null);
        return parsed.third();
    }

    @Override
    public Collection<DerivativePrice> puts(int oid) {
        return puts(getTickerFor(oid));
    }

    @Override
    public Collection<DerivativePrice> calls(String ticker) {
        Tuple3<Optional<StockPrice>, Collection<DerivativePrice>, Collection<DerivativePrice>>
                parsed = parseHtmlFor(ticker, null);
        return parsed.second();
    }

    @Override
    public Collection<DerivativePrice> calls(int oid) {
        return calls(getTickerFor(oid));
    }

    @Override
    public Collection<Derivative> callPutDefs(String ticker) {
        return null;
    }

    @Override
    public void setDownloadDate(LocalDate localDate) {

    }

    Tuple3<Optional<StockPrice>, Collection<DerivativePrice>, Collection<DerivativePrice>>
    parseHtmlFor(String ticker, File suppliedFile) {
        try {
            if (stoxPutsCalls == null) {
                stoxPutsCalls = new HashMap<>();
            }
            Tuple3<Optional<StockPrice>, Collection<DerivativePrice>, Collection<DerivativePrice>>
                    result = stoxPutsCalls.get(ticker);
            if (result == null) {
                Stock stock = stockMarketRepository.findStock(ticker);
                Document doc = getDocument(ticker);

                Optional<StockPrice> stockPrice = createStockPrice(doc, stock);
                StockPrice spParam = stockPrice.isPresent() ? stockPrice.get() : null;

                Elements rawCalls = findRawOptions(doc, true);
                List<DerivativePrice> calls = createDerivativePrices(rawCalls, Derivative.OptionType.CALL, spParam);

                Elements rawPuts = findRawOptions(doc, false);
                List<DerivativePrice> puts = createDerivativePrices(rawPuts, Derivative.OptionType.PUT, spParam);

                result = new Tuple3<>(stockPrice, calls, puts);
                stoxPutsCalls.put(ticker, result);
            }
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            //throw new RuntimeException(e);
            Tuple3<Optional<StockPrice>, Collection<DerivativePrice>, Collection<DerivativePrice>>
                    tmp = new Tuple3<>(Optional.empty(), new ArrayList<>(), new ArrayList<>());
            return tmp;
        }

    }

    @Override
    public void invalidateCache() {
        stoxPutsCalls = null;
    }
    //endregion

    //region Properties
    @Autowired
    public void setDownloader(EtradeDownloader<Page, String, Serializable> downloader) {
        this.downloader = downloader;
    }
    @Autowired
    public void setStockMarketRepository(StockMarketRepository stockMarketRepository) {
        this.stockMarketRepository = stockMarketRepository;
    }
    @Autowired
    public void setOptionCalculator(OptionCalculator optionCalculator) {
        this.optionCalculator = optionCalculator;
    }
    //endregion Properties

    //region Private/Package Accessible methods
    Map<Integer,String> oid2ticker = new HashMap<>();

    String getTickerFor(int oid) {
        String ticker = oid2ticker.get(oid);
        if (ticker == null) {
            ticker = stockMarketRepository.getTickerFor(oid);
            oid2ticker.put(oid,ticker);
        }
        return ticker;
    }
    Document getDocument(String ticker) throws IOException {
        Page page = downloader.downloadDerivatives(ticker);
        return Jsoup.parse(page.getWebResponse().getContentAsString());
    }

    Optional<StockPrice> createStockPrice(Document doc, Stock stock) {
        Element top = doc.getElementById("updatetable1");
        if (top == null) {
            return Optional.empty();
        }
        Element closeEl = top.getElementById("ju.l");
        Elements openEl = top.getElementsByAttributeValue("name","ju.op");
        Elements hiEl = top.getElementsByAttributeValue("name","ju.h");
        Elements loEl = top.getElementsByAttributeValue("name","ju.lo");
        Elements volEl = top.getElementsByAttributeValue("name","ju.vo");
        Element timeEl = doc.getElementById("toptime");
        if ((closeEl == null)
            || openEl.isEmpty()
            || hiEl.isEmpty()
            || loEl.isEmpty()
            || volEl.isEmpty()
            || timeEl == null) {
            return Optional.empty();
        }
        try {
            double close = Double.parseDouble(closeEl.text());
            double open = Double.parseDouble(openEl.text());
            double hi = Double.parseDouble(hiEl.text());
            double lo = Double.parseDouble(loEl.text());
            long vol = Long.parseLong(volEl.text().replaceAll("\\s",""));
            Tuple2<LocalDate, LocalTime> timeInfo = getTimeInfo(timeEl);
            StockPriceBean result = new StockPriceBean(timeInfo.first(), timeInfo.second(), open, hi, lo, close, vol);
            result.setStock(stock);
            return Optional.of(result);
        }
        catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    Tuple2<LocalDate, LocalTime> getTimeInfo(Element el) {
        // fredag 23/11-2018 18:30:05
        String txt = el.text();
        String[] txts = txt.split("\\s");
        LocalDate ld = LocalDate.parse(txts[1], dateFormatter);
        LocalTime tm = LocalTime.parse(txts[2], timeFormatter);
        return new Tuple2<>(ld,tm);
    }

    Elements findRawOptions(Document doc, boolean isCalls) {
        String search = isCalls ? "American call" : "American put";
        Elements result = doc.getElementsContainingOwnText(search);
        return result;
    }


    List<DerivativePrice> createDerivativePrices(Elements rawOptions, Derivative.OptionType optionType, StockPrice stockPrice) {
        List<DerivativePrice> result = new ArrayList<>();
        for (Element el : rawOptions) {
            DerivativePrice price = createDerivativePrice(el.parent(), optionType, stockPrice);
            if (price != null) {
                result.add(price);
            }
        }
        return result;
    }

    private DerivativePrice createDerivativePrice(Element el, Derivative.OptionType optionType, StockPrice stockPrice) {
        int sz = el.childNodeSize();
        if (sz < 15) {
            return null;
        }
        try {
            Element buyEl = el.child(4);
            Element sellEl = el.child(5);
            double buy = Double.parseDouble(buyEl.text());
            double sell = Double.parseDouble(sellEl.text());
            Stock stock = stockPrice == null ? null : stockPrice.getStock();
            Optional<Derivative> derivative = fetchOrCreateDerivative(el, optionType, stock);
            if (!derivative.isPresent()) {
                return null;
            }
            DerivativePrice bean = new DerivativePriceBean(stockPrice, derivative.get(), buy, sell, optionCalculator);
            return bean;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
    /*
    private Optional<Derivative> fetchOrCreateDerivative(Element el) {
        Element tickerEl = el.child(0);
        String ticker = tickerEl.text();
        Optional<Derivative> found = stockMarketRepository.findDerivative(ticker);
        if (!found.isPresent()) {
            Derivative.OptionType ot = optionTypeFor(el);
            if (ot == Derivative.OptionType.UNDEF) {
                return null;
            }
            DerivativeBean derivative2 = new DerivativeBean();
            derivative2.setLifeCycle(Derivative.LifeCycle.FROM_HTML);

            derivative2.setOpType(ot);
            derivative2.setTicker(ticker);
            derivative2.setX(exercisePriceFor(el));
            derivative2.setExpiry(expiryFor(el));
            stockMarketRepository.insertDerivative(derivative2, null);
            return Optional.of(derivative2);
        }
        else {
            return found;
        }
    }
    */
    private Optional<Derivative> fetchOrCreateDerivative(Element el, Derivative.OptionType optionType, Stock stock) {
        try {
            String optionName = el.child(0).text();

            Optional<Derivative> found = stockMarketRepository.findDerivative(optionName);

            if (!found.isPresent()) {
                DerivativeBean derivative2 = new DerivativeBean();
                derivative2.setLifeCycle(Derivative.LifeCycle.FROM_HTML);
                derivative2.setOpType(optionType);

                double x = Double.parseDouble(el.child(2).text());
                String child3txt = el.child(3).text();
                LocalDate exp = LocalDate.parse(child3txt, dateFormatter);

                derivative2.setTicker(optionName);
                derivative2.setX(x);
                derivative2.setExpiry(exp);
                derivative2.setStock(stock);

                return Optional.of(derivative2);
            }
            else {
                return found;
            }
        }
        catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    /*
    private double exercisePriceFor(Element el)  {
        Element xEl = el.child(2);
        return Double.parseDouble(xEl.text());
    }
    private LocalDate expiryFor(Element el) {
        String expiry = el.child(3).text();
        LocalDate exp = LocalDate.parse(expiry, dateFormatter);
        return LocalDate.now();
    }
    private Derivative.OptionType optionTypeFor(Element el) {
        String ot = el.child(1).text();
        Derivative.OptionType result = ot.equals("American call") ? Derivative.OptionType.CALL :
                ot.equals("American put") ? Derivative.OptionType.PUT :
                        ot.equals("European call") ? Derivative.OptionType.CALL :
                                ot.equals("European put") ? Derivative.OptionType.PUT :
                                        Derivative.OptionType.UNDEF;
        return result;
    }
    */
    //endregion
 }
