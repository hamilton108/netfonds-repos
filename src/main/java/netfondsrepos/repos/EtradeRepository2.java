package netfondsrepos.repos;

import com.gargoylesoftware.htmlunit.Page;
import critterrepos.beans.StockPriceBean;
import critterrepos.beans.options.DerivativeBean;
import critterrepos.beans.options.DerivativePriceBean;
import oahu.dto.Tuple;
import oahu.dto.Tuple3;
import oahu.financial.Derivative;
import oahu.financial.DerivativePrice;
import oahu.financial.Stock;
import oahu.financial.StockPrice;
import oahu.financial.html.EtradeDownloader;
import oahu.financial.repository.EtradeRepository;
import oahu.financial.repository.StockMarketRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class EtradeRepository2 implements
        EtradeRepository<Tuple<String>,
                         Tuple3<Optional<StockPrice>,Collection<DerivativePrice>,Collection<DerivativePrice>>> {

    private EtradeDownloader<Page, Serializable> downloader;

    private Map<String,Tuple3<Optional<StockPrice>,Collection<DerivativePrice>,Collection<DerivativePrice>>>
    stoxPutsCalls = null;
    private StockMarketRepository stockMarketRepository;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM-yyyy");

    //region interface EtradeRepository
    @Override
    public Optional<DerivativePrice> findDerivativePrice(Tuple<String> optionInfo) {
        return Optional.empty();
    }

    @Override
    public Optional<StockPrice> stockPrice(String ticker) {
        return Optional.empty();
    }

    @Override
    public Collection<DerivativePrice> puts(String ticker) {
        return null;
    }

    @Override
    public Collection<DerivativePrice> calls(String ticker) {
        return null;
    }

    @Override
    public Collection<Derivative> callPutDefs(String ticker) {
        return null;
    }

    @Override
    public void setDownloadDate(LocalDate localDate) {

    }

    @Override
    public Tuple3<Optional<StockPrice>, Collection<DerivativePrice>, Collection<DerivativePrice>>
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
                Elements rawCalls = findRawOptions(doc, true);
                List<DerivativePrice> calls = createDerivativePrices(rawCalls);
                result = new Tuple3<>(stockPrice, calls, null);
                stoxPutsCalls.put(ticker, result);
            }
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            //throw new RuntimeException(e);
            return null;
        }

        /*
        try {
            if (stoxPutsCalls == null) {
                stoxPutsCalls = new HashMap<>();
            }
            Tuple3<Optional<StockPrice>, Collection<DerivativePrice>, Collection<DerivativePrice>>
                    result = stoxPutsCalls.get(ticker);
            if (result == null) {
                Stock stock = stockMarketRepository.findStock(ticker);
                Page page = downloader.downloadDerivatives(ticker);
                Document doc = Jsoup.parse(page.getWebResponse().getContentAsString());
                Element top = doc.getElementsByClass("com topmargin").first();
                //List<Element> trs = top.getElementsByTag("tr");
                List<Element> rawCalls = top.getElementsContainingOwnText("American call");
                List<Element> rawPuts = top.getElementsContainingOwnText("American put");

                Collection<DerivativePrice> calls = new ArrayList<>();
                for (Element el : rawCalls) {
                    DerivativePrice call = processElement(el.parent(), Derivative.OptionType.CALL, stock);
                    if (call != null) {
                        calls.add(call);
                    }
                }
                result = new Tuple3<>(Optional.empty(), calls, null);
            }
            return result;
        }
        catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        */
    }
    //endregion

    //region Properties
    public void setDownloader(EtradeDownloader<Page, Serializable> downloader) {
        this.downloader = downloader;
    }
    public void setStockMarketRepository(StockMarketRepository stockMarketRepository) {
        this.stockMarketRepository = stockMarketRepository;
    }
    //endregion Properties

    //region Private/Package Accessible methods
    Document getDocument(String ticker) throws IOException {
        Page page = downloader.downloadDerivatives(ticker);
        return Jsoup.parse(page.getWebResponse().getContentAsString());
    }

    /*
    DerivativePrice processElement(Element el, Derivative.OptionType optionType, Stock stock) {
        int sz = el.childNodeSize();
        if (sz < 15) {
            return null;
        }
        try {
            Element buyEl = el.child(4);
            Element sellEl = el.child(5);
            double buy = Double.parseDouble(buyEl.text());
            double sell = Double.parseDouble(sellEl.text());

            Element optionNameEl = el.child(0);
            Optional<Derivative> derivative = stockMarketRepository.findDerivative(optionNameEl.text());
            if (!derivative.isPresent()) {
                derivative = createNewDerivative(el,optionType,stock);
            }
            if (!derivative.isPresent()) {
                return null;
            }
            stockMarketRepository.insertDerivative(derivative.get(),null);

            DerivativePrice bean = new DerivativePriceBean(derivative.get(), buy, sell, null);
            return bean;
        }
        catch (NumberFormatException ex) {
            return null;
        }
    }
    //*/

    Optional<Derivative> createNewDerivative(Element el, Derivative.OptionType optionType, Stock stock) {
        try {
            DerivativeBean derivative2 = new DerivativeBean();
            derivative2.setLifeCycle(Derivative.LifeCycle.FROM_HTML);
            derivative2.setOpType(optionType);

            String optionName = el.child(0).text();
            double x = Double.parseDouble(el.child(2).text());
            LocalDate exp = LocalDate.parse(el.child(3).text(), formatter);

            derivative2.setTicker(optionName);
            derivative2.setX(x);
            derivative2.setExpiry(exp);
            derivative2.setStock(stock);

            return Optional.of(derivative2);
        }
        catch (NumberFormatException ex) {
            return Optional.empty();
        }
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
        if ((closeEl == null)
            || openEl.isEmpty()
            || hiEl.isEmpty()
            || loEl.isEmpty()) {
            return Optional.empty();
        }
        try {
            double close = Double.parseDouble(closeEl.text());
            double open = Double.parseDouble(openEl.text());
            double hi = Double.parseDouble(hiEl.text());
            double lo = Double.parseDouble(loEl.text());

            StockPriceBean result = new StockPriceBean(LocalDate.now(), open, hi, lo, close, 0);
            result.setStock(stock);
            return Optional.of(result);
        }
        catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    Elements findRawOptions(Document doc, boolean isCalls) {
        String search = isCalls ? "American call" : "American put";
        Elements result = doc.getElementsContainingOwnText(search);
        return result;
    }

    private DerivativePrice createDerivativePrice(Element el, Derivative derivative) {
        int sz = el.childNodeSize();
        if (sz < 15) {
            return null;
        }
        try {
            Element buyEl = el.child(4);
            Element sellEl = el.child(5);
            double buy = Double.parseDouble(buyEl.text());
            double sell = Double.parseDouble(sellEl.text());
            DerivativePrice bean = new DerivativePriceBean(derivative, buy, sell, null);
            return bean;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    List<DerivativePrice> createDerivativePrices(Elements rawOptions) {
        List<DerivativePrice> result = new ArrayList<>();
        for (Element el : rawOptions) {
            //DerivativePrice price = processElement(el.parent(), Derivative.OptionType.CALL, stock);
            DerivativePrice price = createDerivativePrice(el.parent(), null);
            if (price != null) {
                result.add(price);
            }
        }
        return result;
    }
    //endregion
}
